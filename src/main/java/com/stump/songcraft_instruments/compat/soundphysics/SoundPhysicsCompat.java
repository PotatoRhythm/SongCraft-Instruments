package com.stump.songcraft_instruments.compat.soundphysics;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.logging.LogUtils;
import com.sonicether.soundphysics.SoundPhysics;
import com.sonicether.soundphysics.SoundPhysicsMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.WorldOriginSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Integration with Sound Physics Remastered, giving instrument notes reverb and occlusion.
 * <p>Stereo and local notes are played relative to the listener at (0, 0, 0), which Sound Physics
 * treats as "no position" and leaves dry. We track the real world position of every instrument
 * sound by its OpenAL source, and have Sound Physics evaluate that position instead.
 * The note's volume and distance falloff (and so its Stereo/Mono crossfade) are kept as Songcraft
 * defines them; Sound Physics only applies filters and reverb sends on top of it, and its muffling
 * is kept above a minimum so notes never vanish.</p>
 * <p>Changes in environment are crossfaded into through a twin source (see {@link SoundPair}),
 * as OpenAL changing a filter at once is heard as a click.</p>
 * <p>Only reached through the {@code mixins.songcraft_instruments.soundphysics.json} mixins,
 * which only apply when Sound Physics Remastered is installed.</p>
 */
@OnlyIn(Dist.CLIENT)
public class SoundPhysicsCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Sound Physics skips {@link SoundSource#RECORDS} sounds unless it is set to update moving sounds,
     * since it would not otherwise keep them up to date. We keep ours up to date ourselves,
     * so we process them under a different category. It is otherwise only used to detect block sounds.
     */
    private static final SoundSource PROCESSING_CATEGORY = SoundSource.PLAYERS;
    /**
     * How often gliding environments are applied to instrument sounds, in addition to every tick
     */
    private static final long GLIDE_INTERVAL_MS = 10;
    private static final AtomicBoolean GLIDE_QUEUED = new AtomicBoolean();
    private static volatile Executor soundExecutor;
    private static ScheduledExecutorService glideScheduler;

    /**
     * {@code AL_DISTANCE_MODEL} as a source property (AL_EXT_source_distance_model), as vanilla sets it
     */
    static final int AL_SOURCE_DISTANCE_MODEL = 0xD000;

    /**
     * The most high frequencies a filter lets through, just short of all of them (-0.009dB); see onSetEnvironment
     */
    private static final float MAX_CUTOFF = 0.999f;
    /**
     * How far above the top of a block sounds coming from inside of it are evaluated from
     */
    private static final double ABOVE_BLOCK_OFFSET = 0.1;
    /**
     * The distance within which a new emitter starts from the environment of an existing one
     */
    private static final double EMITTER_INHERIT_DISTANCE = 2;
    /**
     * How long an emitter's environment is kept after its last sound
     */
    private static final long EMITTER_LIFETIME_NANOS = 10_000_000_000L;

    /**
     * Instrument sounds by their OpenAL source. Accessed from the sound thread.
     */
    private static final Map<Integer, TrackedSound> SOURCES = new ConcurrentHashMap<>();
    /**
     * Instrument sounds by their instance. Accessed from the main thread.
     */
    private static final Map<SoundInstance, TrackedSound> INSTANCES = new WeakHashMap<>();
    /**
     * The environments of the places instrument sounds come from
     */
    private static final Map<BlockPos, EmitterEnvironment> EMITTERS = new ConcurrentHashMap<>();

    /**
     * The instrument sound we are currently processing on this thread, or null.
     * Used so we don't redirect our own calls, and to know which sound Sound Physics' environment is for.
     */
    private static final ThreadLocal<Processing> PROCESSING = new ThreadLocal<>();

    /**
     * @param auxOnly Whether only the reverb of the sound is processed; its direct sound is then intentionally silenced
     */
    private record Processing(TrackedSound tracked, int source, boolean auxOnly) {}

    private static final class TrackedSound {
        private final boolean relative;
        /**
         * The distance a linearly attenuated sound fully fades out at, or -1 if it is not linearly attenuated
         */
        private final float maxDistance;
        private final EmitterEnvironment environment;
        private final ResourceLocation soundId;
        /**
         * The environment last applied to this sound's source, or null if none was yet.
         * Only used when it has no {@link #pair}. Sound thread only.
         */
        private float[] appliedEnvironment;
        /**
         * The twin of this sound's source that its environment crossfades through,
         * or null if it has none (then its environment glides instead). Sound thread only.
         */
        private @Nullable SoundPair pair;
        private volatile Vec3 origin;
        /**
         * Where Sound Physics evaluates this sound from; see {@link #physicsOrigin}
         */
        private volatile Vec3 physicsOrigin;

        private TrackedSound(boolean relative, float maxDistance, EmitterEnvironment environment,
                             ResourceLocation soundId, Vec3 origin) {
            this.relative = relative;
            this.maxDistance = maxDistance;
            this.environment = environment;
            this.soundId = soundId;
            setOrigin(origin);
        }

        /**
         * Main thread only, as it accesses the level
         */
        private void setOrigin(Vec3 origin) {
            this.origin = origin;
            this.physicsOrigin = SoundPhysicsCompat.physicsOrigin(origin);
        }
    }


    /**
     * Starts tracking an instrument sound. Called on the main thread when a sound is played,
     * before its channel starts playing.
     */
    public static void onSoundPlay(SoundInstance sound, ChannelAccess.ChannelHandle handle) {
        if (!(sound instanceof WorldOriginSoundInstance originSound))
            return;

        // Same as the attenuation SoundEngine#play gives the channel
        final float maxDistance = (sound.getAttenuation() == SoundInstance.Attenuation.LINEAR)
            ? Math.max(sound.getVolume(), 1) * sound.getSound().getAttenuationDistance()
            : -1;

        final Vec3 origin = originSound.getWorldOrigin();
        final TrackedSound tracked = new TrackedSound(
            sound.isRelative(), maxDistance, emitterAt(origin), sound.getLocation(), origin
        );
        INSTANCES.put(sound, tracked);

        // Queued before the channel's play(), which is where Sound Physics first processes it
        handle.execute((channel) -> {
            final int source = sourceOf(channel);
            // Before vanilla sets the channel up, so its twin mirrors all of that.
            // A disabled Sound Physics applies no environment to crossfade through.
            // (We still track the sound, as Sound Physics changes its distance falloff even when disabled.)
            tracked.pair = SoundPhysicsMod.CONFIG.enabled.get() ? SoundPair.create(source) : null;
            SOURCES.put(source, tracked);

            if (debugLogging())
                LOGGER.info("[start] src={} {} relative={} dist={} twin={}", source, tracked.soundId, tracked.relative,
                    fmt(origin.distanceTo(NoteSound.listenerPos())), (tracked.pair == null) ? "none" : tracked.pair.twin);
        });
    }

    /**
     * Called on the main thread every sound engine tick.
     * Keeps the positions of instrument sounds updated, and reprocesses them periodically.
     */
    public static void tick(Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel, Executor soundExecutor) {
        SoundPhysicsCompat.soundExecutor = soundExecutor;
        startGliding();

        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        // When enabled, Sound Physics reprocesses all sounds by itself
        final boolean reprocess = !SoundPhysicsMod.CONFIG.updateMovingSounds.get();
        final int interval = Math.max(1, SoundPhysicsMod.CONFIG.soundUpdateInterval.get());

        instanceToChannel.forEach((sound, handle) -> {
            final TrackedSound tracked = INSTANCES.get(sound);
            if (tracked == null)
                return;

            tracked.setOrigin(((WorldOriginSoundInstance) sound).getWorldOrigin());

            if (reprocess && Math.floorMod(level.getGameTime() + sound.hashCode(), interval) == 0) {
                final ResourceLocation soundId = sound.getLocation();
                handle.execute((channel) -> process(tracked, sourceOf(channel), soundId, false));
            }
        });

        final long now = System.nanoTime();
        EMITTERS.values().removeIf((emitter) -> now - emitter.lastUsedNanos > EMITTER_LIFETIME_NANOS);
    }

    /**
     * Updates the environments of instrument sounds every {@link #GLIDE_INTERVAL_MS}, rather than only every tick:
     * so their crossfades (see {@link SoundPair}) are smooth and quick, and so the environments of sounds without
     * a pair glide in steps small enough not to be heard as clicks.
     */
    private static void startGliding() {
        if (glideScheduler != null)
            return;

        glideScheduler = Executors.newSingleThreadScheduledExecutor((runnable) -> {
            final Thread thread = new Thread(runnable, "Songcraft Instruments Sound Physics glide");
            thread.setDaemon(true);
            return thread;
        });
        glideScheduler.scheduleAtFixedRate(() -> {
            final Executor executor = soundExecutor;
            // Don't pile up glides if the sound thread is busy
            if ((executor != null) && !SOURCES.isEmpty() && GLIDE_QUEUED.compareAndSet(false, true))
                executor.execute(SoundPhysicsCompat::glideAll);
        }, GLIDE_INTERVAL_MS, GLIDE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Moves every instrument sound towards its environment. Sound thread only.
     */
    private static void glideAll() {
        GLIDE_QUEUED.set(false);

        try {
            SOURCES.forEach((source, tracked) -> updateEnvironment(tracked, source));
        } catch (RuntimeException e) {
            LOGGER.error("Failed to glide the Sound Physics environment of instrument sounds", e);
        }
    }

    /**
     * Called on the sound thread when a channel is destroyed, freeing its source.
     */
    public static void onChannelDestroyed(int source) {
        final TrackedSound tracked = SOURCES.remove(source);
        if (tracked == null)
            return;

        if (tracked.pair != null) {
            tracked.pair.delete();
            tracked.pair = null;
        }

        if (debugLogging())
            LOGGER.info("[end] src={} {}", source, tracked.soundId);
    }


    // Mirroring a channel onto its twin, called on the sound thread by ChannelMixin

    private static @Nullable SoundPair pairOf(int source) {
        final TrackedSound tracked = SOURCES.get(source);
        return (tracked == null) ? null : tracked.pair;
    }

    /**
     * @return The volume to give the channel's own source
     */
    public static float onSetVolume(int source, float volume) {
        final SoundPair pair = pairOf(source);
        return (pair == null) ? volume : pair.setVolume(volume);
    }
    public static void onSetPitch(int source, float pitch) {
        final SoundPair pair = pairOf(source);
        if (pair != null)
            pair.mirrorFloat(AL10.AL_PITCH, pitch);
    }
    public static void onSetLooping(int source, boolean looping) {
        final SoundPair pair = pairOf(source);
        if (pair != null)
            pair.mirrorInt(AL10.AL_LOOPING, looping ? AL10.AL_TRUE : AL10.AL_FALSE);
    }
    public static void onSetRelative(int source, boolean relative) {
        final SoundPair pair = pairOf(source);
        if (pair != null)
            pair.mirrorInt(AL10.AL_SOURCE_RELATIVE, relative ? AL10.AL_TRUE : AL10.AL_FALSE);
    }
    public static void onAttenuationSet(int source) {
        final SoundPair pair = pairOf(source);
        if (pair != null)
            pair.mirrorAttenuation();
    }
    public static void onAttachStaticBuffer(int source) {
        final SoundPair pair = pairOf(source);
        if (pair != null)
            pair.mirrorBuffer();
    }
    /**
     * A streamed sound's buffers can't be shared with a twin, so it glides its environment instead
     */
    public static void onAttachBufferStream(int source) {
        final TrackedSound tracked = SOURCES.get(source);
        if ((tracked != null) && (tracked.pair != null)) {
            tracked.pair.delete();
            tracked.pair = null;
        }
    }
    /**
     * Plays (or resumes) the channel's source, along with its twin
     */
    public static void play(int source) {
        final SoundPair pair = pairOf(source);
        if (pair != null)
            pair.play();
        else
            AL10.alSourcePlay(source);
    }
    public static void pause(int source) {
        final SoundPair pair = pairOf(source);
        if (pair != null)
            pair.pause();
        else
            AL10.alSourcePause(source);
    }
    public static void onStop(int source) {
        final SoundPair pair = pairOf(source);
        if (pair != null)
            pair.stopTwin();
    }

    /**
     * Called whenever Sound Physics is about to process a sound.
     * @return Whether the sound is an instrument sound we processed instead
     */
    public static boolean redirectProcessing(int source, ResourceLocation sound, boolean auxOnly) {
        if (PROCESSING.get() != null)
            return false;

        final TrackedSound tracked = SOURCES.get(source);
        if (tracked == null)
            return false;

        process(tracked, source, sound, auxOnly);
        return true;
    }

    /**
     * Called when Sound Physics applies the environment (muffling and reverb) it evaluated for a sound.
     * For instrument sounds, we keep the muffling above the configured minimum clarity so they never vanish,
     * and crossfade (or glide) into it rather than applying it at once.
     * @param values Sound Physics' {@code setEnvironment} values; see {@link EmitterEnvironment#VALUE_COUNT}
     * @return Whether we applied the environment instead
     */
    public static boolean onSetEnvironment(int source, float[] values) {
        final Processing processing = PROCESSING.get();
        if ((processing == null) || processing.auxOnly || (processing.source != source))
            return false;

        final float minClarity = ModClientConfigs.SOUND_PHYSICS_MIN_CLARITY.get().floatValue();
        values[EmitterEnvironment.DIRECT_CUTOFF] = Math.max(values[EmitterEnvironment.DIRECT_CUTOFF], minClarity);
        // Sound Physics derives the direct gain from the cutoff the same way
        values[EmitterEnvironment.DIRECT_GAIN] = Math.max(values[EmitterEnvironment.DIRECT_GAIN], (float) Math.pow(minClarity, 0.1));

        // OpenAL skips a low-pass filter that lets all the high frequencies through, and restarts it from scratch
        // once it doesn't. Moving in and out of that (e.g. peeking around a corner) is heard as a click,
        // so keep filters from ever fully opening.
        for (int i = EmitterEnvironment.FIRST_CUTOFF; i <= EmitterEnvironment.DIRECT_CUTOFF; i++)
            values[i] = Math.min(values[i], MAX_CUTOFF);

        if (debugLogging())
            LOGGER.info("[target] src={} {}", source, fmtEnvironment(values));

        processing.tracked.environment.setTarget(values);
        updateEnvironment(processing.tracked, source);
        return true;
    }

    /**
     * Called on the sound thread whenever a channel's position is set, which happens every tick for instrument sounds.
     * Glides the environment of instrument sounds, and keeps them heard from where Sound Physics evaluated them to be
     * (vanilla would otherwise move them back to their real position every tick, making them jump back and forth).
     * @param pos The real position of the sound
     * @return The position the sound should be heard from
     */
    public static Vec3 onPositionUpdate(int source, Vec3 pos) {
        final TrackedSound tracked = SOURCES.get(source);
        if (tracked == null)
            return pos;

        updateEnvironment(tracked, source);

        final Vec3 heardFrom = tracked.relative ? pos : heardFromPosition(tracked, pos);
        if (tracked.pair != null)
            tracked.pair.mirrorPosition((float) heardFrom.x, (float) heardFrom.y, (float) heardFrom.z);

        return heardFrom;
    }


    private static void process(TrackedSound tracked, int source, ResourceLocation sound, boolean auxOnly) {
        restoreAttenuation(tracked, source);

        final Vec3 origin = tracked.origin, physicsOrigin = tracked.physicsOrigin;
        final Vec3 newPos;

        PROCESSING.set(new Processing(tracked, source, auxOnly));
        try {
            newPos = SoundPhysics.processSound(
                source, physicsOrigin.x, physicsOrigin.y, physicsOrigin.z, PROCESSING_CATEGORY, sound, auxOnly
            );
        } finally {
            PROCESSING.remove();
        }

        // Sound Physics evaluates where a sound is heard from (e.g. a doorway), in world coordinates.
        // We stop it from moving instrument sounds there itself (see isProcessing), and glide them there instead.
        if (tracked.relative) {
            // A relative sound must stay on the listener. In case Sound Physics still moved it:
            if (newPos != null) {
                AL10.alSource3f(source, AL10.AL_POSITION, 0, 0, 0);
                if (tracked.pair != null)
                    tracked.pair.mirrorPosition(0, 0, 0);
            }
        } else {
            // Keep hearing it from there until the next evaluation, rather than only until the next tick
            tracked.environment.setTargetDirection(
                (newPos == null) ? null : newPos.subtract(NoteSound.listenerPos()).normalize()
            );

            if (debugLogging())
                LOGGER.info("[direction] src={} {} targetOffAngle={}", source, tracked.soundId,
                    (newPos == null) ? "none" : fmt(angleBetween(origin, newPos)));

            final Vec3 pos = heardFromPosition(tracked, origin);
            AL10.alSource3f(source, AL10.AL_POSITION, (float) pos.x, (float) pos.y, (float) pos.z);
            if (tracked.pair != null)
                tracked.pair.mirrorPosition((float) pos.x, (float) pos.y, (float) pos.z);
        }
    }

    /**
     * @return Whether we are currently processing the given instrument sound source.
     * Sound Physics shouldn't move the position of such a source itself.
     */
    public static boolean isProcessing(int source) {
        final Processing processing = PROCESSING.get();
        return (processing != null) && (processing.source == source);
    }

    /**
     * Moves a sound towards the environment of its emitter: by crossfading into it through its {@link SoundPair},
     * or (if it has none) by gliding to it.
     */
    private static void updateEnvironment(TrackedSound tracked, int source) {
        final boolean evaluated = tracked.environment.isEvaluated();

        if (tracked.pair != null) {
            tracked.pair.update(evaluated ? tracked.environment.target() : null);
            return;
        }

        if (!evaluated)
            return;

        final float[] v = tracked.environment.advance();

        // Re-applying it re-attaches the source's filters, which is needless (and may be heard)
        if (Arrays.equals(v, tracked.appliedEnvironment))
            return;
        tracked.appliedEnvironment = v.clone();

        setEnvironmentOf(source, v);
    }

    /**
     * Applies an environment to an OpenAL source through Sound Physics, without it being captured by onSetEnvironment
     */
    static void setEnvironmentOf(int alSource, float[] v) {
        final Processing processing = PROCESSING.get();
        PROCESSING.remove();
        try {
            SoundPhysics.setEnvironment(alSource, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9]);
        } finally {
            if (processing != null)
                PROCESSING.set(processing);
        }
    }


    // Debug logging, enabled along with Sound Physics' own debug logging

    private static boolean debugLogging() {
        return SoundPhysicsMod.CONFIG.debugLogging.get();
    }

    private static String fmt(double value) {
        return String.format("%.3f", value);
    }

    private static String fmtEnvironment(float[] v) {
        return String.format("direct=%.3f/%.3f sends=%.3f,%.3f,%.3f,%.3f sendCutoffs=%.3f,%.3f,%.3f,%.3f",
            v[EmitterEnvironment.DIRECT_CUTOFF], v[EmitterEnvironment.DIRECT_GAIN],
            v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7]);
    }

    /**
     * @return The angle in degrees between the directions from the listener to 2 positions
     */
    private static double angleBetween(Vec3 a, Vec3 b) {
        final Vec3 listener = NoteSound.listenerPos();
        final Vec3 toA = a.subtract(listener).normalize(), toB = b.subtract(listener).normalize();
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, toA.dot(toB)))));
    }

    /**
     * Sounds coming from inside a block (such as a Looper's) can't have a clear line of sight from where they are,
     * so Sound Physics would only ever hear them through reflections.
     * We have it evaluate them from just above the block instead. Main thread only.
     * @return The given position, moved just above the top of the block it is inside of, if any
     */
    private static Vec3 physicsOrigin(Vec3 origin) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return origin;

        final BlockPos pos = BlockPos.containing(origin);
        final VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (shape.isEmpty())
            return origin;

        final double top = pos.getY() + shape.max(Direction.Axis.Y);
        if (origin.y >= top)
            return origin;

        // Centered on the block, so all sounds from it (e.g. a Stereo note at the block's center
        // and its Mono crossfade at its corner) are evaluated alike
        return new Vec3(pos.getX() + 0.5, top + ABOVE_BLOCK_OFFSET, pos.getZ() + 0.5);
    }

    /**
     * @return The environment of the place a sound at the given position comes from
     */
    private static EmitterEnvironment emitterAt(Vec3 origin) {
        final BlockPos key = BlockPos.containing(origin);
        final EmitterEnvironment existing = EMITTERS.get(key);
        if (existing != null)
            return existing;

        // Start from a nearby environment, so an instrument moving between blocks doesn't jump
        EmitterEnvironment nearest = null;
        double nearestDistSqr = EMITTER_INHERIT_DISTANCE * EMITTER_INHERIT_DISTANCE;
        for (final Map.Entry<BlockPos, EmitterEnvironment> entry : EMITTERS.entrySet()) {
            final double distSqr = entry.getKey().distToCenterSqr(origin);
            if (distSqr <= nearestDistSqr) {
                nearest = entry.getValue();
                nearestDistSqr = distSqr;
            }
        }

        final EmitterEnvironment created = new EmitterEnvironment(nearest);
        final EmitterEnvironment raced = EMITTERS.putIfAbsent(key, created);
        return (raced != null) ? raced : created;
    }

    /**
     * @return The position a non-relative sound is heard from: at its real distance from the listener
     * (so its falloff is untouched), in the direction Sound Physics evaluated it to be heard from.
     * The direction is shared by all notes from the same place, and glides rather than jumping.
     */
    private static Vec3 heardFromPosition(TrackedSound tracked, Vec3 realPos) {
        final Vec3 listener = NoteSound.listenerPos();
        final Vec3 toSound = realPos.subtract(listener);
        final double distance = toSound.length();
        if (distance < 1e-4)
            return realPos;

        final Vec3 direction = tracked.environment.advanceDirection(toSound.scale(1 / distance));
        return listener.add(direction.scale(distance));
    }

    /**
     * Sound Physics keeps linearly attenuated sounds at full volume up to half their range.
     * Instruments use their own falloff (which their Stereo sounds mimic), so we restore vanilla's:
     * fading linearly from the source until {@link TrackedSound#maxDistance}.
     */
    private static void restoreAttenuation(TrackedSound tracked, int source) {
        if (tracked.maxDistance < 0)
            return;

        AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 0);
        AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, tracked.maxDistance);
        if (tracked.pair != null)
            tracked.pair.mirrorAttenuation();
    }

    private static int sourceOf(Channel channel) {
        return ((ChannelSource) channel).songcraft_instruments$getSource();
    }
}
