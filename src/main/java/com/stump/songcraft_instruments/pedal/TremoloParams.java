package com.stump.songcraft_instruments.pedal;

/**
 * What a Tremolo pedal's knobs mean, and the volume it gives a note at any moment.
 * @param hz How many times a second the volume pulses
 * @param depth How far the volume dips, from 0 (not at all) to {@link #MAX_DEPTH}
 * @param sharpness How square the pulses are: 1 for a smooth sine, up to {@link #MAX_SHARPNESS} for nearly on/off
 */
public record TremoloParams(double hz, double depth, double sharpness) {
    public static final double MIN_HZ = 1, MAX_HZ = 12;
    /**
     * Just short of fully silent: a sound whose volume reaches 0 is stopped by vanilla when volume settings change
     */
    public static final double MAX_DEPTH = 0.98;
    public static final double MAX_SHARPNESS = 20;

    public static TremoloParams of(final PedalEffect effect) {
        return new TremoloParams(
            MIN_HZ * Math.pow(MAX_HZ / MIN_HZ, effect.knobFraction(0)),
            effect.knobFraction(1) * MAX_DEPTH,
            1 + effect.knobFraction(2) * (MAX_SHARPNESS - 1)
        );
    }

    /**
     * @param seconds The time on a free-running clock, shared by every note, so chords pulse together
     * @return The volume multiplier at that time, from {@code 1 - depth} to 1
     */
    public double gainAt(final double seconds) {
        final double sine = Math.sin(2 * Math.PI * hz * seconds);
        // Squaring a sine off: tanh of a larger multiple of it flattens its tops, normalized back to -1 to 1
        final double wave = Math.tanh(sharpness * sine) / Math.tanh(sharpness);
        return 1 - depth * (0.5 - 0.5 * wave);
    }
}
