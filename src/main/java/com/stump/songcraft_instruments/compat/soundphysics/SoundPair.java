package com.stump.songcraft_instruments.compat.soundphysics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;

import javax.annotation.Nullable;

/**
 * An instrument sound's OpenAL source, paired with a twin source playing the same sound in sync.
 * <p>OpenAL applies a new filter setting at once, which is heard as a click. So to change a sound's environment
 * (muffling and reverb), we apply it to whichever of the pair is silent, and crossfade their volumes into it,
 * which OpenAL does smoothly.</p>
 * <p>The twin mirrors everything vanilla does to the primary source (see {@link SoundPhysicsCompat}).
 * Sound thread only.</p>
 */
@OnlyIn(Dist.CLIENT)
final class SoundPair {
    /**
     * How long crossfading into a new environment takes
     */
    private static final long FADE_NANOS = 60_000_000L;
    /**
     * How long to wait after a crossfade before starting another, for the faded-out source to fully fall silent
     */
    private static final long SETTLE_NANOS = 40_000_000L;
    /**
     * Changes in environment smaller than this (in any of its values) don't warrant a crossfade
     */
    private static final double MIN_CHANGE_DB = 0.5;
    /**
     * The most pairs at a time, so instruments never take the sources vanilla sounds need
     */
    private static final int MAX_PAIRS = 32;
    private static int pairCount;

    final int primary, twin;

    private float baseVolume = 1;
    /**
     * Which of the pair is currently heard: 0 for the primary, 1 for the twin.
     * While fading, the one being faded out of.
     */
    private int active;
    private boolean fading;
    private long fadeStartNanos, fadeEndNanos;

    private final float[][] appliedEnvironment = new float[2][];
    private final float[] appliedGain = {-1, -1};

    private SoundPair(int primary, int twin) {
        this.primary = primary;
        this.twin = twin;
    }

    /**
     * @return A new pair for the given source, or null if no twin source could be made for it
     */
    static @Nullable SoundPair create(int primary) {
        if (pairCount >= MAX_PAIRS)
            return null;

        final int twin = AL10.alGenSources();
        if (AL10.alGetError() != AL10.AL_NO_ERROR)
            return null;

        AL10.alSourcef(twin, AL10.AL_GAIN, 0);
        pairCount++;
        return new SoundPair(primary, twin);
    }

    void delete() {
        AL10.alSourceStop(twin);
        AL10.alSourcei(twin, AL10.AL_BUFFER, 0);
        AL10.alDeleteSources(twin);
        clearErrors();
        pairCount--;
    }

    private int source(int index) {
        return (index == 0) ? primary : twin;
    }


    // Mirroring the primary source

    /**
     * @param volume The volume vanilla sets for the sound
     * @return The volume of the primary source
     */
    float setVolume(float volume) {
        baseVolume = volume;
        applyGains(System.nanoTime());
        return appliedGain[0];
    }

    void mirrorFloat(int param, float value) {
        AL10.alSourcef(twin, param, value);
        clearErrors();
    }
    void mirrorInt(int param, int value) {
        AL10.alSourcei(twin, param, value);
        clearErrors();
    }

    void mirrorPosition(float x, float y, float z) {
        AL10.alSource3f(twin, AL10.AL_POSITION, x, y, z);
        clearErrors();
    }

    /**
     * Copies the primary's distance attenuation onto the twin
     */
    void mirrorAttenuation() {
        mirrorInt(SoundPhysicsCompat.AL_SOURCE_DISTANCE_MODEL, AL10.alGetSourcei(primary, SoundPhysicsCompat.AL_SOURCE_DISTANCE_MODEL));
        mirrorFloat(AL10.AL_MAX_DISTANCE, AL10.alGetSourcef(primary, AL10.AL_MAX_DISTANCE));
        mirrorFloat(AL10.AL_ROLLOFF_FACTOR, AL10.alGetSourcef(primary, AL10.AL_ROLLOFF_FACTOR));
        mirrorFloat(AL10.AL_REFERENCE_DISTANCE, AL10.alGetSourcef(primary, AL10.AL_REFERENCE_DISTANCE));
    }

    void mirrorBuffer() {
        mirrorInt(AL10.AL_BUFFER, AL10.alGetSourcei(primary, AL10.AL_BUFFER));
    }

    /**
     * Starts both sources at once, so they play in sync
     */
    void play() {
        AL10.alSourcePlayv(new int[] {primary, twin});
    }
    void pause() {
        AL10.alSourcePausev(new int[] {primary, twin});
    }
    void stopTwin() {
        AL10.alSourceStop(twin);
        clearErrors();
    }


    // Environment

    /**
     * Moves the pair towards the given environment: applies it to the silent source and crossfades into it
     * @param desired The environment to have, or null if there is none yet
     */
    void update(@Nullable float[] desired) {
        final long now = System.nanoTime();

        if (fading && (now - fadeStartNanos >= FADE_NANOS)) {
            fading = false;
            active = 1 - active;
            fadeEndNanos = now;
        }

        if (desired != null) {
            if (appliedEnvironment[active] == null) {
                // The first environment; the sound isn't heard with any other yet
                applyEnvironment(active, desired);
            } else if (!fading && (now - fadeEndNanos >= SETTLE_NANOS) && differs(appliedEnvironment[active], desired)) {
                applyEnvironment(1 - active, desired);
                fading = true;
                fadeStartNanos = now;
            }
        }

        applyGains(now);
    }

    private void applyEnvironment(int index, float[] values) {
        appliedEnvironment[index] = values.clone();
        SoundPhysicsCompat.setEnvironmentOf(source(index), values);
    }

    private void applyGains(long now) {
        for (int i = 0; i < 2; i++) {
            final float gain = baseVolume * weight(i, now);
            if (gain != appliedGain[i]) {
                appliedGain[i] = gain;
                // The primary's volume is otherwise set by vanilla, which we modify to be this
                AL10.alSourcef(source(i), AL10.AL_GAIN, gain);
            }
        }
        clearErrors();
    }

    /**
     * @return How much of the given source is heard, from 0 to 1. Crossfades linearly,
     * as both play the same sound in sync.
     */
    private float weight(int index, long now) {
        if (!fading)
            return (index == active) ? 1 : 0;

        final float progress = fadeProgress(now);
        return (index == active) ? (1 - progress) : progress;
    }

    private float fadeProgress(long now) {
        return Math.min(1, (now - fadeStartNanos) / (float) FADE_NANOS);
    }

    private static boolean differs(float[] a, float[] b) {
        for (int i = 0; i < a.length; i++)
            if (Math.abs(EmitterEnvironment.toDb(a[i]) - EmitterEnvironment.toDb(b[i])) > MIN_CHANGE_DB)
                return true;
        return false;
    }

    /**
     * Our calls must not leave errors for vanilla's own error checks to pick up
     */
    private static void clearErrors() {
        AL10.alGetError();
    }
}
