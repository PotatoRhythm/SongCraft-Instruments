package com.stump.songcraft_instruments.pedal;

/**
 * What a Lo-fi pedal's knobs mean: a worn cassette tape.
 * <p>Its tape tone (bandwidth, saturation and warmth) is rendered into each sound; its wow and flutter are applied to
 * the pitch live (see {@link PedalEffectType.Kind#RENDERED_AND_LIVE}), and its hiss rolls on its own while the
 * instrument plays (see {@code TapeDecks}).</p>
 * <p>The wobble is driven by the world's game time ({@code TapeClock}) through a repeatable random function, rather
 * than by each player's own clock, so every player hears the same drift at the same moment, like one tape.</p>
 * @param age How worn the tape is, from 0 to 1: how narrow its bandwidth is, and how loud its hiss
 * @param wobble How much wow and flutter, from 0 to 1
 * @param warmth How warm it is, from 0 (clean) to 1: saturation, more body in the low mids and softer highs
 */
public record LofiParams(double age, double wobble, double warmth) {
    /**
     * The tape's bandwidth: from a fresh tape's to a worn one's, as heard
     */
    public static final double FRESH_LOWPASS_HZ = 16000, WORN_LOWPASS_HZ = 3500,
        FRESH_HIGHPASS_HZ = 30, WORN_HIGHPASS_HZ = 150;
    /**
     * The hiss's loudness, from a fresh tape's to a worn one's, relative to full scale
     */
    public static final double FRESH_HISS_DB = -54, WORN_HISS_DB = -34;
    /**
     * At full wobble: the slow drift's depth, and the fast shimmer's, in cents either way
     */
    public static final double MAX_WOW_CENTS = 30, MAX_FLUTTER_CENTS = 8;
    /**
     * At full warmth: how hard the tape saturates (the gain into its soft clipper), and how lopsided the clipping is,
     * which adds the even harmonics that make tape sound warm rather than fuzzy
     */
    public static final double MAX_SATURATION_DRIVE = 6, MAX_SATURATION_BIAS = 0.15;
    /**
     * At full warmth: the low mids' boost, and the highs' cut, in dB
     */
    public static final double WARMTH_BODY_HZ = 200, MAX_WARMTH_BODY_DB = 4.5, WARMTH_BODY_Q = 0.7,
        WARMTH_SOFTEN_HZ = 5000, MAX_WARMTH_SOFTEN_DB = 3;

    public static LofiParams of(final PedalEffect effect) {
        return new LofiParams(effect.knobFraction(0), effect.knobFraction(1), effect.knobFraction(2));
    }

    public double lowpassHz() {
        return FRESH_LOWPASS_HZ * Math.pow(WORN_LOWPASS_HZ / FRESH_LOWPASS_HZ, age);
    }
    public double highpassHz() {
        return FRESH_HIGHPASS_HZ * Math.pow(WORN_HIGHPASS_HZ / FRESH_HIGHPASS_HZ, age);
    }
    /**
     * @return How loud the hiss is, as a gain relative to full scale
     */
    public double hissGain() {
        return Math.pow(10, (FRESH_HISS_DB + (WORN_HISS_DB - FRESH_HISS_DB) * age) / 20);
    }

    public double saturationDrive() {
        return 1 + (MAX_SATURATION_DRIVE - 1) * warmth;
    }
    public double saturationBias() {
        return MAX_SATURATION_BIAS * warmth;
    }
    public double bodyDb() {
        return MAX_WARMTH_BODY_DB * warmth;
    }
    public double softenDb() {
        return -MAX_WARMTH_SOFTEN_DB * warmth;
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
     * @return Smooth random values from -1 to 1, changing about once per unit of {@code x}
     */
    private static double smoothNoise(final double x, final long seed) {
        final long i = (long) Math.floor(x);
        final double a = random(i, seed) * 2 - 1, b = random(i + 1, seed) * 2 - 1;
        final double t = x - i;
        return a + (b - a) * t * t * (3 - 2 * t);
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
