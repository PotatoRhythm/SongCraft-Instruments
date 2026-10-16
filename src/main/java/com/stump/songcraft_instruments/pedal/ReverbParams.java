package com.stump.songcraft_instruments.pedal;

/**
 * What a Reverb pedal's knobs mean, shared by its rendering and by how long its notes take to fade when dampened.
 * @param decaySeconds How long the reverb takes to die away by 60 dB, as heard
 * @param toneHz How bright the reverb is: each pass through the room is darkened above this
 * @param mix How loud the reverb is: at 1, it carries as much energy as the note itself
 */
public record ReverbParams(double decaySeconds, double toneHz, double mix) {
    public static final double MIN_DECAY_SECONDS = 0.3, MAX_DECAY_SECONDS = 4;
    public static final double MIN_TONE_HZ = 1500, MAX_TONE_HZ = 12000;
    /**
     * How much longer than its decay the reverb rings on: from -60 dB down to the -80 dB rendered tails end at
     */
    private static final double TAIL_PER_DECAY = 4 / 3.0;

    public static ReverbParams of(final PedalEffect effect) {
        return new ReverbParams(
            MIN_DECAY_SECONDS * Math.pow(MAX_DECAY_SECONDS / MIN_DECAY_SECONDS, effect.knobFraction(0)),
            MIN_TONE_HZ * Math.pow(MAX_TONE_HZ / MIN_TONE_HZ, effect.knobFraction(1)),
            effect.knobFraction(2)
        );
    }

    /**
     * @return How long the reverb rings on after the note
     */
    public double tailSeconds() {
        return (mix <= 0) ? 0 : Math.min(DelayParams.MAX_TAIL_SECONDS, decaySeconds * TAIL_PER_DECAY);
    }
}
