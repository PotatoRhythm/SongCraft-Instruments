package com.stump.songcraft_instruments.client.trombone;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * <p>Where a player's trombone slide is, as driven by the notes they play.</p>
 * Each note moves the slide to the position a real tenor trombone plays it in. The slide stays there
 * between notes like a player's would, and drifts back in once the trombone has been quiet for a moment.
 * <p>Purely client-side state; it is never saved or synced.</p>
 */
@OnlyIn(Dist.CLIENT)
public class TromboneSlide {
    /** The open notes of a tenor trombone (its partials), in semitones above its lowest, Bb2 */
    private static final int[] PARTIALS = {0, 7, 12, 16, 19, 24, 26, 28, 31};
    /** The semitones the note grid's lowest C (C3) is above Bb2 */
    private static final int GRID_C_ABOVE_BB2 = 2;
    /** The furthest slide position, 7th, as semitones below the open note */
    private static final int LAST_POSITION = 6;

    /** The semitones of the 7 natural notes of an octave, as laid out on a note grid row */
    private static final int[] NATURAL_NOTE_PITCHES = {0, 2, 4, 5, 7, 9, 11};

    // All timings are in seconds

    /** How long the slide takes to move to a note's position */
    private static final double MOVE_TIME = 0.12;
    /** How long the trombone has to be quiet before the slide drifts back in */
    private static final double RETURN_DELAY = 1.2;
    /** How long the slide takes to drift back in */
    private static final double RETURN_TIME = 0.4;

    private double from, to;
    private double movedAt = Double.NEGATIVE_INFINITY, moveTime = MOVE_TIME;
    /** When the last note ended, or NaN while notes are playing */
    private double quietSince = Double.NaN;
    /** How many held notes are playing */
    private int heldNotes;


    /**
     * @param soundIndex The index of the played sound within its 7x3 note grid sound array
     * @param pitch The semitones the sound was pitched by
     * @return How far out the slide plays the given note: 0 in 1st position and 1 in 7th
     */
    public static double extensionOf(final int soundIndex, final int pitch) {
        final int row = soundIndex / NATURAL_NOTE_PITCHES.length;
        final int column = soundIndex % NATURAL_NOTE_PITCHES.length;
        final int aboveBb2 = GRID_C_ABOVE_BB2 + row * 12 + NATURAL_NOTE_PITCHES[column] + pitch;

        // Play each note on the nearest open note above it, pulling the slide out a semitone per position
        for (final int partial : PARTIALS) {
            if (partial >= aboveBb2)
                return Math.min(partial - aboveBb2, LAST_POSITION) / (double) LAST_POSITION;
        }
        // Notes above the highest partial are played as far in as they can be
        return 0;
    }


    /**
     * Moves the slide for a single note
     */
    public void tap(final double extension) {
        final double now = now();
        moveTo(extension, now, MOVE_TIME);

        if (heldNotes == 0)
            quietSince = now;
    }

    /**
     * Moves the slide for a note held until {@link #release released}
     */
    public void hold(final double extension) {
        moveTo(extension, now(), MOVE_TIME);
        heldNotes++;
        quietSince = Double.NaN;
    }

    /**
     * Lets go of a note held with {@link #hold}
     */
    public void release() {
        if (heldNotes == 0 || --heldNotes > 0)
            return;

        quietSince = now();
    }


    /**
     * @return How far out the slide currently is: 0 in 1st position and 1 in 7th
     */
    public double extension() {
        final double now = now();

        // Drift back in after a while of quiet
        if (!Double.isNaN(quietSince) && to != 0 && now >= quietSince + RETURN_DELAY)
            moveTo(0, quietSince + RETURN_DELAY, RETURN_TIME);

        return extension(now);
    }

    private double extension(final double now) {
        final double progress = Math.min((now - movedAt) / moveTime, 1);
        return from + (to - from) * easeInOutQuad(progress);
    }

    private void moveTo(final double extension, final double at, final double time) {
        from = extension(at);
        to = extension;
        movedAt = at;
        moveTime = time;
    }


    private static double now() {
        return System.nanoTime() / 1e9;
    }

    private static double easeInOutQuad(final double t) {
        return (t < .5) ? 2 * t * t : 1 - 2 * (1 - t) * (1 - t);
    }
}
