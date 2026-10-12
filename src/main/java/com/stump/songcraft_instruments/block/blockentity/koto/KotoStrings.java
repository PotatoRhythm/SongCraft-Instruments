package com.stump.songcraft_instruments.block.blockentity.koto;

import java.util.Arrays;

/**
 * <p>The vibration of a koto's 5 strings, as plucked by the notes played on it.</p>
 * A plucked string quivers up and down, quickly at first and dying away like a real string's ring.
 * Higher strings quiver a little faster.
 * <p>Purely client-side state; it is never saved or synced.</p>
 */
public class KotoStrings {
    public static final int STRING_COUNT = 5;

    /** The semitones of the 7 natural notes of an octave, as laid out on a note grid row */
    private static final int[] NATURAL_NOTE_PITCHES = {0, 2, 4, 5, 7, 9, 11};
    /** The semitones the note grid spans */
    private static final int GRID_SEMITONES = 3 * 12;

    /** How far a freshly plucked string swings, in pixels */
    private static final double AMPLITUDE = 0.16;
    /** How many times a second the lowest string swings */
    private static final double LOWEST_FREQUENCY = 14;
    /** How many more times a second each higher string swings */
    private static final double FREQUENCY_STEP = 1.5;

    // All timings are in seconds

    /** How long a string's swing takes to die down to about a third */
    private static final double DECAY_TIME = 0.3;
    /** How long a string keeps vibrating at all */
    private static final double RING_TIME = 1.2;

    private final double[] pluckedAt = new double[STRING_COUNT];

    public KotoStrings() {
        Arrays.fill(pluckedAt, Double.NaN);
    }


    /**
     * @param soundIndex The index of the played sound within its 7x3 note grid sound array
     * @param pitch The semitones the sound was pitched by
     * @return The string that plays the given note, from 0 for the lowest to 4 for the highest.
     * The grid's range is split evenly between the strings.
     */
    public static int stringOf(final int soundIndex, final int pitch) {
        final int row = soundIndex / NATURAL_NOTE_PITCHES.length;
        final int column = soundIndex % NATURAL_NOTE_PITCHES.length;
        final int semitone = row * 12 + NATURAL_NOTE_PITCHES[column] + pitch;

        return Math.max(0, Math.min(STRING_COUNT - 1, Math.floorDiv(semitone * STRING_COUNT, GRID_SEMITONES)));
    }


    /**
     * Plucks the given string, restarting its vibration
     */
    public void pluck(final int string) {
        pluckedAt[string] = now();
    }

    /**
     * @return How far the given string is currently swung off its rest position, in pixels
     */
    public double displacement(final int string) {
        if (Double.isNaN(pluckedAt[string]))
            return 0;

        final double time = now() - pluckedAt[string];
        if (time >= RING_TIME) {
            // Back at rest
            pluckedAt[string] = Double.NaN;
            return 0;
        }

        final double frequency = LOWEST_FREQUENCY + string * FREQUENCY_STEP;
        // Fade the last stretch out completely so the string doesn't stop with a jump
        final double fadeOut = Math.min(1, (RING_TIME - time) / (RING_TIME * .25));

        return AMPLITUDE * Math.exp(-time / DECAY_TIME) * fadeOut * Math.sin(2 * Math.PI * frequency * time);
    }


    private static double now() {
        return System.nanoTime() / 1e9;
    }
}
