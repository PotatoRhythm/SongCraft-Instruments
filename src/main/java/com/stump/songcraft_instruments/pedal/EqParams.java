package com.stump.songcraft_instruments.pedal;

/**
 * What an EQ pedal's knobs mean: how much each of its three bands is boosted or cut, in dB.
 * <p>Each knob is flat at {@link #FLAT}, and moves {@link #STEP_DB} per step either way: from -10.5 to +12 dB.</p>
 * @param bassDb The low shelf's gain, below {@link #BASS_HZ}
 * @param midDb The mid band's gain, around {@link #MID_HZ}
 * @param trebleDb The high shelf's gain, above {@link #TREBLE_HZ}
 */
public record EqParams(double bassDb, double midDb, double trebleDb) {
    /**
     * The knob value at which a band is left as it is
     */
    public static final int FLAT = 7;
    public static final double STEP_DB = 1.5;

    public static final double BASS_HZ = 100, MID_HZ = 800, TREBLE_HZ = 3200;
    /**
     * How wide the mid band is: low enough to shape the whole midrange, like an amp's mid knob
     */
    public static final double MID_Q = 0.8;

    public static EqParams of(final PedalEffect effect) {
        return new EqParams(gainDb(effect.knob(0)), gainDb(effect.knob(1)), gainDb(effect.knob(2)));
    }

    public static double gainDb(final int knobValue) {
        return (knobValue - FLAT) * STEP_DB;
    }

    /**
     * @return The knob's value as shown to players, e.g. "+4.5 dB"
     */
    public static String formatGain(final int knobValue) {
        final double db = gainDb(knobValue);
        return (db == 0) ? "0.0 dB" : String.format("%+.1f dB", db);
    }
}
