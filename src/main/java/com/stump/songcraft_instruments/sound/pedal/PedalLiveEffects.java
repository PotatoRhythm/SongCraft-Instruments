package com.stump.songcraft_instruments.sound.pedal;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.logging.LogUtils;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.pedal.PedalEffectType;
import com.stump.songcraft_instruments.sound.ChannelSource;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Applies pedals' live effects (see {@link PedalEffectType.Kind#LIVE}) to notes while they play, so they also reach
 * sustained notes as they're held. Their tremolo is a volume multiplier on top of everything else about the note.
 * <p>Vanilla sets a playing sound's volume and pitch once a tick, which is far too coarse for a tremolo or vibrato,
 * so they're also set every {@link #UPDATE_INTERVAL_MS}. Both go through the sound's channel
 * ({@link Channel#setVolume}, {@link Channel#setPitch}), so whatever else follows them there
 * (such as Sound Physics' twin sources) applies to the result as usual.</p>
 * <p>The effects run off a free-running clock, shared by every note on this client, so chords pulse together.
 * Each client's clock is its own, so different players hear the pulses at slightly different times.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class PedalLiveEffects {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final long UPDATE_INTERVAL_MS = 10;
    private static final long EPOCH_NANOS = System.nanoTime();

    private static final class Live {
        final SoundInstance instance;
        final PedalChain chain;
        /**
         * Whether the chain changes the sound's volume (a tremolo), and its pitch (a vibrato)
         */
        final boolean changesVolume, changesPitch;
        /**
         * Set on the sound thread, once the sound's channel exists
         */
        volatile Channel channel;
        /**
         * The volume and pitch vanilla last gave the sound, before live effects, or -1 if it hasn't yet
         */
        volatile float baseVolume = -1, basePitch = -1;

        Live(final SoundInstance instance, final PedalChain chain) {
            this.instance = instance;
            this.chain = chain;
            this.changesVolume = chain.has(PedalEffectType.TREMOLO);
            this.changesPitch = chain.has(PedalEffectType.VIBRATO) || chain.has(PedalEffectType.LOFI);
        }
    }

    /**
     * By sound instance, for the main thread's volume updates
     */
    private static final Map<SoundInstance, Live> BY_INSTANCE = new ConcurrentHashMap<>();
    /**
     * By OpenAL source, for the sound thread's updates
     */
    private static final Map<Integer, Live> BY_SOURCE = new ConcurrentHashMap<>();

    private static final AtomicBoolean UPDATE_QUEUED = new AtomicBoolean();
    private static volatile Executor soundExecutor;
    private static ScheduledExecutorService scheduler;

    private PedalLiveEffects() {}

    private static double now() {
        return (System.nanoTime() - EPOCH_NANOS) / 1e9;
    }


    /**
     * Starts applying a sound's live effects, if it has any. Called on the main thread when a sound is played,
     * before its channel starts playing.
     */
    public static void onSoundPlay(final SoundInstance sound, final ChannelAccess.ChannelHandle handle) {
        if (!(sound instanceof PedalSoundInstance pedalSound) || !pedalSound.getPedalChain().hasLive() || (handle == null))
            return;

        final Live live = new Live(sound, pedalSound.getPedalChain());
        BY_INSTANCE.put(sound, live);
        handle.execute((channel) -> {
            live.channel = channel;
            BY_SOURCE.put(((ChannelSource) channel).songcraft_instruments$getSource(), live);
        });
    }

    /**
     * Called on the main thread when vanilla works out a playing sound's volume (every tick)
     * @return The volume to give the sound: the one given, through its live effects
     */
    public static float onVolume(final SoundInstance sound, final float volume) {
        final Live live = BY_INSTANCE.get(sound);
        if (live == null || !live.changesVolume)
            return volume;

        live.baseVolume = volume;
        return volume * (float) live.chain.liveGainAt(now());
    }
    /**
     * Called on the main thread when vanilla works out a playing sound's pitch (every tick)
     * @return The pitch to give the sound: the one given, through its live effects
     */
    public static float onPitch(final SoundInstance sound, final float pitch) {
        final Live live = BY_INSTANCE.get(sound);
        if (live == null || !live.changesPitch)
            return pitch;

        live.basePitch = pitch;
        return pitch * (float) live.chain.livePitchAt(now(), TapeClock.seconds());
    }

    /**
     * Called on the sound thread when a channel is freed
     */
    public static void onChannelDestroyed(final int source) {
        final Live live = BY_SOURCE.remove(source);
        if (live != null)
            BY_INSTANCE.remove(live.instance);
    }

    /**
     * Called on the main thread every sound engine tick
     */
    public static void tick(final Executor executor) {
        soundExecutor = executor;
        if (scheduler != null)
            return;

        scheduler = Executors.newSingleThreadScheduledExecutor((runnable) -> {
            final Thread thread = new Thread(runnable, "Songcraft Instruments pedal effects");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(() -> {
            final Executor sounds = soundExecutor;
            // Don't pile up updates if the sound thread is busy
            if ((sounds != null) && !BY_SOURCE.isEmpty() && UPDATE_QUEUED.compareAndSet(false, true))
                sounds.execute(PedalLiveEffects::updateAll);
        }, UPDATE_INTERVAL_MS, UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Sets the volume of every sound with live effects to where they are now. Sound thread only.
     */
    private static void updateAll() {
        UPDATE_QUEUED.set(false);

        try {
            final double now = now(), tapeNow = TapeClock.seconds();
            for (final Live live : BY_SOURCE.values()) {
                final Channel channel = live.channel;
                if (channel == null)
                    continue;

                // Until vanilla gives it a volume and pitch, it plays at the ones it started with
                final float volume = live.baseVolume, pitch = live.basePitch;
                if (live.changesVolume && volume >= 0)
                    channel.setVolume(volume * (float) live.chain.liveGainAt(now));
                if (live.changesPitch && pitch >= 0)
                    channel.setPitch(pitch * (float) live.chain.livePitchAt(now, tapeNow));
            }
        } catch (RuntimeException e) {
            LOGGER.error("Failed to update the live pedal effects of instrument sounds", e);
        }
    }

    /**
     * Forgets every sound. Called before the sound engine destroys its OpenAL context, and the sounds with it.
     */
    public static void clear() {
        BY_INSTANCE.clear();
        BY_SOURCE.clear();
    }
}
