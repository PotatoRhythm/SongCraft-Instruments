package com.stump.songcraft_instruments.sound;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;
import org.lwjgl.openal.SOFTHRTF;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Makes instrument sounds sound underwater (or in lava) while the listener's head is in it, or while the instrument,
 * Looper or Speaker they come from is fully submerged in it.
 * <p>A submerged sound is heard through 3 paths:</p>
 * <ul>
 *     <li>Its direct sound, quieter and with its high frequencies cut (a low-pass filter)</li>
 *     <li>A very short, dark reverb that cuts everything above ~1kHz: a dull, smeared copy of the sound,
 *     the bulk of what is heard</li>
 *     <li>A modulated reverb (OpenAL's "underwater" preset), for the watery wobble and wash</li>
 * </ul>
 * <p>The latter two are auxiliary sends we reserve on top of the ones the device already has
 * (e.g. Sound Physics Remastered's 4). Without them, only the direct sound is muffled.</p>
 * <p>It all glides into place rather than changing at once, as OpenAL applying a new filter setting at once
 * is heard as a click. A sound that starts while already submerged starts submerged.</p>
 * <p>When Sound Physics applies a sound's direct filter, it muffles the environment it applies by the same amounts
 * instead (see {@code SoundPhysicsCompat}), as a source only has one direct filter.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class UnderwaterMuffling {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * How a sound is heard
     * @param directCutoff How much of the direct sound's high frequencies (above ~5kHz) are let through
     * @param directGain How much of the direct sound's volume is let through
     * @param muffledGain How much of the sound is sent through the muffling reverb
     * @param reverbGain How much of the sound is sent through the underwater reverb
     */
    public record Muffle(float directCutoff, float directGain, float muffledGain, float reverbGain) {}

    public static final Muffle NONE = new Muffle(1, 1, 0, 0);

    private static final Muffle WATER = new Muffle(0.15f, 0.6f, 0.6f, 0.33f);
    private static final Muffle LAVA = new Muffle(0.025f, 0.25f, 0.6f, 0.08f);
    /**
     * Without our auxiliary sends, only the direct sound can be muffled
     */
    private static final Muffle WATER_DIRECT_ONLY = new Muffle(0.12f, 0.8f, 0, 0);
    private static final Muffle LAVA_DIRECT_ONLY = new Muffle(0.02f, 0.45f, 0, 0);

    private enum Medium { AIR, WATER, LAVA }

    /**
     * The most high frequencies a filter lets through, just short of all of them (-0.009dB).
     * OpenAL skips a low-pass filter that lets all of them through, and restarts it from scratch once it doesn't,
     * which is heard as a click.
     */
    public static final float MAX_CUTOFF = 0.999f;

    /**
     * How fast the muffling may change, in decibels per second.
     * At 100, going under water takes ~0.3 seconds, in steps of ~1dB (when applied every 10ms).
     */
    private static final double MAX_DB_PER_SECOND = 100;
    /**
     * The lowest value considered when changing by decibels; anything below is treated as silent (-40dB)
     */
    private static final double DB_FLOOR = 0.01;
    private static final long GLIDE_INTERVAL_MS = 10;

    /**
     * How much of the high frequencies (above ~5kHz) are let into the effects, on top of what they cut
     */
    private static final float SEND_CUTOFF = 0.1f;

    /**
     * How many auxiliary sends we reserve: the muffling reverb and the underwater reverb
     */
    private static final int RESERVED_SENDS = 2;

    /**
     * Our OpenAL objects, made along with each OpenAL context
     * @param reverbSlot 0 when only the muffling reverb has a send
     */
    private record Efx(int directFilter, int sendFilter, int muffledSlot, int muffledSend, int reverbSlot, int reverbSend) {
        boolean hasSends() {
            return muffledSlot != 0;
        }
    }

    /**
     * Null when OpenAL has no EFX support, or while the sound engine is not loaded
     */
    private static volatile @Nullable Efx efx;

    /**
     * Another mod applying the filters of instrument sound sources, called on the sound thread
     */
    public interface ExternalFilters {
        /**
         * @return Whether the mod applies the direct filter of the given source, and so its muffling
         */
        boolean ownsDirectFilter(int source);
        /**
         * @return Another source playing the same sound in sync with the given one, or 0 if there is none.
         * It is muffled along with it.
         */
        int twinOf(int source);
    }

    private static volatile @Nullable ExternalFilters externalFilters;

    private static final class MuffledSound {
        /**
         * What the sound is submerged in. Set on the main thread.
         */
        private volatile Medium target;
        // Sound thread only
        private Muffle current;
        private @Nullable Muffle applied;
        private long lastUpdateNanos;

        private MuffledSound(Medium target) {
            this.target = target;
        }

        private Muffle advance(boolean hasSends) {
            final long now = System.nanoTime();
            final double maxStepDb = MAX_DB_PER_SECOND * (now - lastUpdateNanos) / 1e9;
            lastUpdateNanos = now;

            final Muffle target = muffleIn(this.target, hasSends);
            if (current == null) {
                current = target;
            } else if (!current.equals(target)) {
                current = new Muffle(
                    approach(current.directCutoff, target.directCutoff, maxStepDb),
                    approach(current.directGain, target.directGain, maxStepDb),
                    approach(current.muffledGain, target.muffledGain, maxStepDb),
                    approach(current.reverbGain, target.reverbGain, maxStepDb)
                );
            }
            return current;
        }
    }

    /**
     * Instrument sounds by their OpenAL source. Accessed from the sound thread.
     */
    private static final Map<Integer, MuffledSound> SOURCES = new ConcurrentHashMap<>();
    /**
     * Instrument sounds by their instance. Accessed from the main thread.
     */
    private static final Map<SoundInstance, MuffledSound> INSTANCES = new WeakHashMap<>();

    private static final AtomicBoolean GLIDE_QUEUED = new AtomicBoolean();
    private static volatile Executor soundExecutor;
    private static ScheduledExecutorService glideScheduler;

    private UnderwaterMuffling() {
    }

    public static void setExternalFilters(ExternalFilters externalFilters) {
        UnderwaterMuffling.externalFilters = externalFilters;
    }

    /**
     * @return The current muffling of an instrument sound's source. Sound thread only.
     */
    public static Muffle muffleOf(int source) {
        final MuffledSound muffled = SOURCES.get(source);
        return ((muffled == null) || (muffled.current == null)) ? NONE : muffled.current;
    }


    // Tracking instrument sounds

    /**
     * Starts muffling an instrument sound. Called on the main thread when a sound is played,
     * before its channel starts playing.
     */
    public static void onSoundPlay(SoundInstance sound, ChannelAccess.ChannelHandle handle) {
        if (!(sound instanceof WorldOriginSoundInstance originSound) || (handle == null))
            return;

        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        final MuffledSound muffled = new MuffledSound(mediumOf(level, originSound, listenerMedium()));
        INSTANCES.put(sound, muffled);

        // Queued before the channel is set up and played, so a submerged sound is never heard otherwise
        handle.execute((channel) -> {
            final int source = ((ChannelSource) channel).songcraft_instruments$getSource();
            SOURCES.put(source, muffled);
            muffled.lastUpdateNanos = System.nanoTime();
            update(source, muffled);
        });
    }

    /**
     * Called on the main thread every sound engine tick. Updates what each instrument sound is submerged in.
     */
    public static void tick(Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel, Executor soundExecutor) {
        UnderwaterMuffling.soundExecutor = soundExecutor;
        startGliding();

        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        final Medium listener = listenerMedium();
        instanceToChannel.keySet().forEach((sound) -> {
            final MuffledSound muffled = INSTANCES.get(sound);
            if (muffled != null)
                muffled.target = mediumOf(level, (WorldOriginSoundInstance) sound, listener);
        });
    }

    /**
     * Called on the sound thread when a channel is destroyed, freeing its source
     */
    public static void onChannelDestroyed(int source) {
        SOURCES.remove(source);
    }

    /**
     * Called on the sound thread when another mod starts playing a sound in sync on a twin source,
     * so it is muffled the same (see {@link ExternalFilters#twinOf})
     */
    public static void onTwinCreated(int source, int twin) {
        final Efx efx = UnderwaterMuffling.efx;
        final MuffledSound muffled = SOURCES.get(source);
        if ((efx == null) || (muffled == null) || (muffled.applied == null))
            return;

        applySends(efx, twin, muffled.applied);
        AL10.alGetError();
    }


    // What sounds are submerged in, on the main thread

    private static Medium listenerMedium() {
        return switch (Minecraft.getInstance().gameRenderer.getMainCamera().getFluidInCamera()) {
            case WATER -> Medium.WATER;
            case LAVA -> Medium.LAVA;
            default -> Medium.AIR;
        };
    }

    /**
     * @return What a sound is heard through:
     * the denser of what the listener's head is in and what the sound's origin is fully submerged in
     */
    private static Medium mediumOf(ClientLevel level, WorldOriginSoundInstance sound, Medium listener) {
        final Medium emitter = emitterMedium(level, sound);
        return (emitter.ordinal() > listener.ordinal()) ? emitter : listener;
    }

    /**
     * @return What the origin of a sound is fully submerged in
     */
    private static Medium emitterMedium(ClientLevel level, WorldOriginSoundInstance sound) {
        // A player playing a held instrument: once their head is under, as for the listener
        final Entity entity = sound.getOriginEntity();
        if (entity != null) {
            if (entity.isEyeInFluid(FluidTags.LAVA))
                return Medium.LAVA;
            if (entity.isEyeInFluid(FluidTags.WATER))
                return Medium.WATER;
            return Medium.AIR;
        }

        // A block: once the fluid also fills the block above it
        return mediumOf(level.getFluidState(BlockPos.containing(sound.getWorldOrigin()).above()));
    }

    private static Medium mediumOf(FluidState fluid) {
        if (fluid.is(FluidTags.LAVA))
            return Medium.LAVA;
        if (fluid.is(FluidTags.WATER))
            return Medium.WATER;
        return Medium.AIR;
    }

    private static Muffle muffleIn(Medium medium, boolean hasSends) {
        return switch (medium) {
            case AIR -> NONE;
            case WATER -> hasSends ? WATER : WATER_DIRECT_ONLY;
            case LAVA -> hasSends ? LAVA : LAVA_DIRECT_ONLY;
        };
    }


    // Applying the muffling, on the sound thread

    private static void startGliding() {
        if (glideScheduler != null)
            return;

        glideScheduler = Executors.newSingleThreadScheduledExecutor((runnable) -> {
            final Thread thread = new Thread(runnable, "Songcraft Instruments muffling glide");
            thread.setDaemon(true);
            return thread;
        });
        glideScheduler.scheduleAtFixedRate(() -> {
            final Executor executor = soundExecutor;
            // Don't pile up glides if the sound thread is busy
            if ((executor != null) && !SOURCES.isEmpty() && GLIDE_QUEUED.compareAndSet(false, true))
                executor.execute(UnderwaterMuffling::glideAll);
        }, GLIDE_INTERVAL_MS, GLIDE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private static void glideAll() {
        GLIDE_QUEUED.set(false);

        try {
            SOURCES.forEach(UnderwaterMuffling::update);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to glide the muffling of instrument sounds", e);
        }
    }

    /**
     * Moves a sound's muffling towards what it is submerged in, and applies it to its source
     */
    private static void update(int source, MuffledSound muffled) {
        final Efx efx = UnderwaterMuffling.efx;
        final Muffle muffle = muffled.advance((efx != null) && efx.hasSends());

        // Re-applying it re-attaches the source's filters, which is needless
        if ((efx == null) || muffle.equals(muffled.applied))
            return;
        muffled.applied = muffle;

        final ExternalFilters external = externalFilters;
        if ((external == null) || !external.ownsDirectFilter(source))
            applyDirect(efx, source, muffle);

        applySends(efx, source, muffle);
        final int twin = (external == null) ? 0 : external.twinOf(source);
        if (twin != 0)
            applySends(efx, twin, muffle);

        // Our calls must not leave errors for vanilla's own error checks to pick up
        AL10.alGetError();
    }

    private static void applyDirect(Efx efx, int source, Muffle muffle) {
        EXTEfx.alFilterf(efx.directFilter, EXTEfx.AL_LOWPASS_GAIN, muffle.directGain);
        EXTEfx.alFilterf(efx.directFilter, EXTEfx.AL_LOWPASS_GAINHF, Math.min(muffle.directCutoff, MAX_CUTOFF));
        // The source keeps a copy of the filter's settings, so one filter serves all sources
        AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, efx.directFilter);
    }

    private static void applySends(Efx efx, int source, Muffle muffle) {
        if (!efx.hasSends())
            return;

        applySend(efx, source, efx.muffledSlot, efx.muffledSend, muffle.muffledGain);
        if (efx.reverbSlot != 0)
            applySend(efx, source, efx.reverbSlot, efx.reverbSend, muffle.reverbGain);
    }

    private static void applySend(Efx efx, int source, int slot, int send, float gain) {
        if (gain <= 0) {
            // Out of the water, the effect is left alone
            AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, EXTEfx.AL_EFFECTSLOT_NULL, send, EXTEfx.AL_FILTER_NULL);
            return;
        }

        EXTEfx.alFilterf(efx.sendFilter, EXTEfx.AL_LOWPASS_GAIN, gain);
        EXTEfx.alFilterf(efx.sendFilter, EXTEfx.AL_LOWPASS_GAINHF, SEND_CUTOFF);
        AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, slot, send, efx.sendFilter);
    }


    // Setting up OpenAL

    /**
     * Called when the sound engine has created its OpenAL context, before any sound is played.
     * Reserves our auxiliary sends, and makes our filters and effects.
     */
    public static void onLibraryInit(long device) {
        efx = null;
        SOURCES.clear();

        if (!ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX")) {
            LOGGER.warn("OpenAL has no EFX support; instrument sounds won't sound underwater");
            return;
        }

        AL10.alGetError();
        final int directFilter = makeLowPass(), sendFilter = makeLowPass();
        if (AL10.alGetError() != AL10.AL_NO_ERROR) {
            LOGGER.warn("Failed to make the OpenAL filters for instrument sounds underwater");
            return;
        }

        final int firstSend = ALC10.alcGetInteger(device, EXTEfx.ALC_MAX_AUXILIARY_SENDS);
        final int sends = reserveSends(device, firstSend);

        int muffledSlot = 0, reverbSlot = 0;
        if (sends >= 1) {
            muffledSlot = makeSlot(makeMuffledEffect());
            if (sends >= 2)
                reverbSlot = makeSlot(makeReverbEffect());

            if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                LOGGER.warn("Failed to make the OpenAL effects for instrument sounds underwater; only muffling them");
                muffledSlot = reverbSlot = 0;
            }
        }

        LOGGER.info("Instrument sounds underwater: {} auxiliary sends reserved from send {}", sends, firstSend);
        efx = new Efx(directFilter, sendFilter, muffledSlot, firstSend, reverbSlot, firstSend + 1);
    }

    /**
     * Called before the sound engine destroys its OpenAL context, and our objects with it
     */
    public static void onLibraryCleanup() {
        efx = null;
    }

    /**
     * Raises the device's auxiliary sends by ours, so we never share a send with another mod
     * @param firstSend How many sends the device has
     * @return How many sends from {@code firstSend} on are ours
     */
    private static int reserveSends(long device, int firstSend) {
        if (!ALC10.alcIsExtensionPresent(device, "ALC_SOFT_HRTF")) {
            LOGGER.warn("OpenAL can't reset its device; instrument sounds underwater will only be muffled");
            return 0;
        }

        // Resetting the device resets every attribute not given, so keep HRTF as Minecraft set it
        final boolean hrtf = ALC10.alcGetInteger(device, SOFTHRTF.ALC_HRTF_SOFT) == ALC10.ALC_TRUE;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer attributes = stack.mallocInt(5)
                .put(SOFTHRTF.ALC_HRTF_SOFT).put(hrtf ? ALC10.ALC_TRUE : ALC10.ALC_FALSE)
                .put(EXTEfx.ALC_MAX_AUXILIARY_SENDS).put(firstSend + RESERVED_SENDS)
                .put(0)
                .flip();

            if (!SOFTHRTF.alcResetDeviceSOFT(device, attributes)) {
                LOGGER.warn("Failed to reserve OpenAL auxiliary sends: {}", ALC10.alcGetString(device, ALC10.alcGetError(device)));
                return 0;
            }
        }

        return Math.max(0, Math.min(RESERVED_SENDS, ALC10.alcGetInteger(device, EXTEfx.ALC_MAX_AUXILIARY_SENDS) - firstSend));
    }

    private static int makeLowPass() {
        final int filter = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(filter, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
        return filter;
    }

    private static int makeSlot(int effect) {
        final int slot = EXTEfx.alGenAuxiliaryEffectSlots();
        EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, effect);
        // Our send gains are what they are, regardless of the sound's distance
        EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_FALSE);
        return slot;
    }

    /**
     * A reverb so short it is barely heard as one, cutting everything above ~1kHz (the lowest it can):
     * a dull, slightly smeared copy of the sound, gently wobbling.
     * <p>Not an equalizer effect, as the OpenAL Soft Minecraft bundles (1.21.1) crashes with one.</p>
     */
    private static int makeMuffledEffect() {
        final int effect = EXTEfx.alGenEffects();
        EXTEfx.alEffecti(effect, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DENSITY, 1.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DIFFUSION, 1.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_GAIN, 1.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_GAINHF, 0.02f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_GAINLF, 1.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DECAY_TIME, 0.1f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DECAY_HFRATIO, 0.1f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DECAY_LFRATIO, 1.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_REFLECTIONS_GAIN, 1.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_REFLECTIONS_DELAY, 0.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_LATE_REVERB_GAIN, 0.5f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_LATE_REVERB_DELAY, 0.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_ECHO_TIME, 0.25f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_ECHO_DEPTH, 0.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_MODULATION_TIME, 0.6f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_MODULATION_DEPTH, 0.1f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_AIR_ABSORPTION_GAINHF, 1.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_HFREFERENCE, 1000.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_LFREFERENCE, 250.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_ROOM_ROLLOFF_FACTOR, 0.0f);
        EXTEfx.alEffecti(effect, EXTEfx.AL_EAXREVERB_DECAY_HFLIMIT, AL10.AL_TRUE);
        return effect;
    }

    /**
     * OpenAL's "underwater" reverb preset (EFX_REVERB_PRESET_UNDERWATER), with its slow, deep modulation
     */
    private static int makeReverbEffect() {
        final int effect = EXTEfx.alGenEffects();
        EXTEfx.alEffecti(effect, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DENSITY, 0.3645f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DIFFUSION, 1.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_GAIN, 0.3162f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_GAINHF, 0.01f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_GAINLF, 1.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DECAY_TIME, 1.49f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DECAY_HFRATIO, 0.1f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DECAY_LFRATIO, 1.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_REFLECTIONS_GAIN, 0.5963f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_REFLECTIONS_DELAY, 0.007f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_LATE_REVERB_GAIN, 7.0795f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_LATE_REVERB_DELAY, 0.011f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_ECHO_TIME, 0.25f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_ECHO_DEPTH, 0.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_MODULATION_TIME, 1.18f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_MODULATION_DEPTH, 0.348f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_AIR_ABSORPTION_GAINHF, 0.9943f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_HFREFERENCE, 5000.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_LFREFERENCE, 250.0f);
        EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_ROOM_ROLLOFF_FACTOR, 0.0f);
        EXTEfx.alEffecti(effect, EXTEfx.AL_EAXREVERB_DECAY_HFLIMIT, AL10.AL_TRUE);
        return effect;
    }

    /**
     * @return The given value moved towards the target by at most the given amount of decibels
     */
    private static float approach(float value, float target, double maxStepDb) {
        if (value == target)
            return value;

        final double valueDb = toDb(value), diffDb = toDb(target) - valueDb;
        // Also snaps from the floor to silence
        if (Math.abs(diffDb) <= maxStepDb)
            return target;

        return (float) Math.pow(10, (valueDb + Math.signum(diffDb) * maxStepDb) / 20);
    }

    private static double toDb(float value) {
        return 20 * Math.log10(Math.max(value, DB_FLOOR));
    }
}
