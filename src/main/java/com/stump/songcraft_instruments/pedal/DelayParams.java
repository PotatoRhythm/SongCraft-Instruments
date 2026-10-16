package com.stump.songcraft_instruments.pedal;

/**
 * What a Delay pedal's knobs mean, shared by its rendering and by how long its notes take to fade when dampened.
 * @param seconds The time between echoes
 * @param feedback How much of each echo comes back as the next one, from 0 to {@link #MAX_FEEDBACK}
 * @param mix How loud the first echo is, compared to the note
 */
public record DelayParams(double seconds, double feedback, double mix) {
    public static final double MIN_SECONDS = 0.06, MAX_SECONDS = 1;
    /**
     * Kept well below 1, so echoes always die away
     */
    public static final double MAX_FEEDBACK = 0.7;
    /**
     * The longest the echoes may ring on after a note, so a note's sound (and the memory it takes) stays bounded
     */
    public static final double MAX_TAIL_SECONDS = 4;
    /**
     * Echoes quieter than this (-60 dB) are inaudible under the note, so the tail ends there
     */
    private static final double SILENCE = 0.001;

    public static DelayParams of(final PedalEffect effect) {
        return new DelayParams(
            MIN_SECONDS * Math.pow(MAX_SECONDS / MIN_SECONDS, effect.knobFraction(0)),
            effect.knobFraction(1) * MAX_FEEDBACK,
            effect.knobFraction(2)
        );
    }

    /**
     * @return How long the echoes ring on after the note
     */
    public double tailSeconds() {
        if (mix <= 0)
            return 0;

        // Echo n is mix * feedback^(n - 1) as loud as the note
        final int echoes = (feedback <= 0) ? 1
            : 1 + (int) Math.ceil(Math.log(SILENCE / mix) / Math.log(feedback));
        return Math.min(MAX_TAIL_SECONDS, Math.max(1, echoes) * seconds);
    }
}
