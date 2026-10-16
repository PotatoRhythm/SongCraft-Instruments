package com.stump.songcraft_instruments.pedal;

/**
 * What a Vibrato pedal's knobs mean, and the pitch it gives a note at any moment.
 * @param hz How many times a second the pitch wobbles
 * @param semitones How far the pitch bends either way, from 0 to {@link #MAX_SEMITONES}
 * @param triangle How much of a triangle the wobble is: 0 for a smooth sine, 1 for a straight-lined triangle
 */
public record VibratoParams(double hz, double semitones, double triangle) {
    public static final double MIN_HZ = 2, MAX_HZ = 10;
    public static final double MAX_SEMITONES = 1;

    public static VibratoParams of(final PedalEffect effect) {
        return new VibratoParams(
            MIN_HZ * Math.pow(MAX_HZ / MIN_HZ, effect.knobFraction(0)),
            effect.knobFraction(1) * MAX_SEMITONES,
            effect.knobFraction(2)
        );
    }

    /**
     * @param seconds The time on a free-running clock, shared by every note, so chords wobble together
     * @return The pitch multiplier at that time
     */
    public double pitchAt(final double seconds) {
        final double phase = hz * seconds;
        final double sine = Math.sin(2 * Math.PI * phase);
        // A triangle in step with the sine: rising through 0 at the start of each cycle
        final double cycle = phase - Math.floor(phase + 0.25);
        final double triangleWave = 1 - 4 * Math.abs(cycle - 0.25);
        final double wave = sine + triangle * (triangleWave - sine);
        return Math.pow(2, semitones * wave / 12);
    }
}
