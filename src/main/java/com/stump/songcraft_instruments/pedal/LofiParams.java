package com.stump.songcraft_instruments.pedal;

/**
 * What a Lo-fi pedal's knobs mean: a worn cassette tape.
 * <p>Its tape tone and saturation are rendered into each sound; its wow and flutter (pitch) and its dropouts (volume)
 * are applied live (see {@link PedalEffectType.Kind#RENDERED_AND_LIVE}), and its hiss rolls on its own while the
 * instrument plays (see {@code TapeDecks}).</p>
 * <p>The wobble and dropouts are driven by the world's game time ({@code TapeClock}) through a repeatable random
 * function, rather than by each player's own clock, so every player hears the same drift and the same dropouts
 * at the same moment, like one tape.</p>
 * @param age How worn the tape is, from 0 to 1: tone loss, saturation and hiss
 * @param wobble How much wow and flutter, from 0 to 1
 * @param dropouts How often (and how deep) the tape drops out, from 0 (never) to 1
 */
public record LofiParams(double age, double wobble, double dropouts) {
    /**
     * The tape's bandwidth: from a fresh tape's to a worn one's, as heard
     */
    public static final double FRESH_LOWPASS_HZ = 16000, WORN_LOWPASS_HZ = 3500,
        FRESH_HIGHPASS_HZ = 30, WORN_HIGHPASS_HZ = 150;
    /**
     * How hard the worn tape saturates: its peaks are rounded off by tanh, with this much gain into it
     */
    public static final double MAX_SATURATION_DRIVE = 4;
    /**
     * The hiss's loudness, from a fresh tape's to a worn one's, relative to full scale
     */
    public static final double FRESH_HISS_DB = -54, WORN_HISS_DB = -34;
    /**
     * At full wobble: the slow drift's depth, and the fast shimmer's, in cents either way
     */
    public static final double MAX_WOW_CENTS = 30, MAX_FLUTTER_CENTS = 8;
    /**
     * Dropouts are decided per slot of this length; at full, a dropout starts in this share of them
     */
    private static final double DROPOUT_SLOT_SECONDS = 0.5, MAX_DROPOUT_CHANCE = 0.6;
    private static final double MIN_DROPOUT_SECONDS = 0.04, MAX_DROPOUT_SECONDS = 0.25;
    private static final double DROPOUT_ATTACK_SECONDS = 0.015, DROPOUT_RELEASE_SECONDS = 0.04;
    private static final double MIN_DROPOUT_DB = 6, MAX_DROPOUT_DB = 30;

    public static LofiParams of(final PedalEffect effect) {
        return new LofiParams(effect.knobFraction(0), effect.knobFraction(1), effect.knobFraction(2));
    }

    public double lowpassHz() {
        return FRESH_LOWPASS_HZ * Math.pow(WORN_LOWPASS_HZ / FRESH_LOWPASS_HZ, age);
    }
    public double highpassHz() {
        return FRESH_HIGHPASS_HZ * Math.pow(WORN_HIGHPASS_HZ / FRESH_HIGHPASS_HZ, age);
    }
    public double saturationDrive() {
        return 1 + (MAX_SATURATION_DRIVE - 1) * age;
    }
    /**
     * @return How loud the hiss is, as a gain relative to full scale
     */
    public double hissGain() {
        return Math.pow(10, (FRESH_HISS_DB + (WORN_HISS_DB - FRESH_HISS_DB) * age) / 20);
    }


    /**
     * @param tapeSeconds The time on the tape clock, shared by every player
     * @return The pitch multiplier the wow and flutter give a note at that time
     */
    public double pitchAt(final double tapeSeconds) {
        if (wobble <= 0)
            return 1;

        // Wow: a slow, uneven drift. Two slow sines that never line up, and a smooth random wander
        final double wow = 0.35 * Math.sin(2 * Math.PI * 0.53 * tapeSeconds)
            + 0.25 * Math.sin(2 * Math.PI * 0.31 * tapeSeconds + 1.7)
            + 0.4 * smoothNoise(tapeSeconds * 0.4, 0x5eed);
        // Flutter: a faster, finer shimmer
        final double flutter = 0.6 * Math.sin(2 * Math.PI * 6.3 * tapeSeconds)
            + 0.4 * Math.sin(2 * Math.PI * 9.7 * tapeSeconds + 0.6);

        final double cents = wobble * (MAX_WOW_CENTS * wow + MAX_FLUTTER_CENTS * flutter);
        return Math.pow(2, cents / 1200);
    }

    /**
     * @param tapeSeconds The time on the tape clock, shared by every player
     * @return The volume multiplier the dropouts give a note at that time, from about -30 dB to 1
     */
    public double gainAt(final double tapeSeconds) {
        if (dropouts <= 0)
            return 1;

        final long slot = (long) Math.floor(tapeSeconds / DROPOUT_SLOT_SECONDS);
        // A dropout may run on into the next slot, so the previous one is checked too
        return Math.min(dropoutGain(slot - 1, tapeSeconds), dropoutGain(slot, tapeSeconds));
    }
    private double dropoutGain(final long slot, final double tapeSeconds) {
        if (random(slot, 0) >= dropouts * MAX_DROPOUT_CHANCE)
            return 1;

        final double start = (slot + random(slot, 1)) * DROPOUT_SLOT_SECONDS;
        final double length = MIN_DROPOUT_SECONDS + (MAX_DROPOUT_SECONDS - MIN_DROPOUT_SECONDS) * random(slot, 2);
        final double depthDb = MIN_DROPOUT_DB + (MAX_DROPOUT_DB - MIN_DROPOUT_DB) * dropouts * random(slot, 3);

        final double t = tapeSeconds - start;
        if (t <= 0 || t >= length + DROPOUT_RELEASE_SECONDS)
            return 1;

        // Dips quickly, holds, then comes back a little slower, smoothly so it never clicks
        final double amount = (t < DROPOUT_ATTACK_SECONDS) ? smoothstep(t / DROPOUT_ATTACK_SECONDS)
            : (t < length) ? 1
            : 1 - smoothstep((t - length) / DROPOUT_RELEASE_SECONDS);
        final double floor = Math.pow(10, -depthDb / 20);
        return 1 - (1 - floor) * amount;
    }


    /**
     * @return Smooth random values from -1 to 1, changing about once per unit of {@code x}
     */
    private static double smoothNoise(final double x, final long seed) {
        final long i = (long) Math.floor(x);
        final double a = random(i, seed) * 2 - 1, b = random(i + 1, seed) * 2 - 1;
        return a + (b - a) * smoothstep(x - i);
    }
    private static double smoothstep(final double t) {
        final double clamped = Math.max(0, Math.min(1, t));
        return clamped * clamped * (3 - 2 * clamped);
    }
    /**
     * @return A random number from 0 to 1 that is always the same for the same inputs, on every player's game
     */
    private static double random(final long index, final long salt) {
        // SplitMix64
        long z = index * 0x9E3779B97F4A7C15L + salt * 0xBF58476D1CE4E5B9L + 0x632BE59BD9B4E019L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return (z >>> 11) * 0x1.0p-53;
    }
}
