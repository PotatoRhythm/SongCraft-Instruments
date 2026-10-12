package com.stump.songcraft_instruments.block.blockentity.keyboard;

import java.util.Arrays;

/**
 * <p>The pressed state of a keyboard's 36 keys, as driven by the notes played on it.</p>
 * Each key is pressed by its note and springs back up afterward. Keys travel along eased curves
 * that mimic a real piano key: it gives way under the finger, bottoms out on the key bed's felt with a slight squish,
 * then rises back and settles past its rest position before coming to a stop.
 * <p>Purely client-side state; it is never saved or synced.</p>
 */
public class KeyboardKeys {
    public static final int OCTAVES = 3, KEYS_PER_OCTAVE = 12;
    public static final int KEY_COUNT = OCTAVES * KEYS_PER_OCTAVE;

    /** The semitones of the 7 natural notes of an octave, as laid out on a note grid row */
    private static final int[] NATURAL_NOTE_PITCHES = {0, 2, 4, 5, 7, 9, 11};

    // All timings are in seconds

    /** How long a key takes to travel down to the key bed */
    private static final double PRESS_TIME = 0.05;
    /** How far past full depth the key squishes into the felt as it bottoms out */
    private static final double BOTTOM_SQUISH = 0.07;
    /** How long the felt takes to push the key back to full depth after bottoming out */
    private static final double SETTLE_TIME = 0.07;

    /**
     * How long a note's key stays down. Notes are one-shot sounds, so other players can't know how long
     * a key was really held for; this resembles a regular, non-staccato key stroke.
     */
    private static final double TAP_HOLD = 0.17;

    /** How long a key takes to rise back to its rest position */
    private static final double RELEASE_TIME = 0.12;
    /** How far above its rest position a fully pressed key overshoots as it springs back up */
    private static final double RELEASE_BOUNCE = 0.08;
    /** How long a key takes to settle down from that overshoot */
    private static final double BOUNCE_SETTLE_TIME = 0.1;

    /**
     * A key restruck while still mostly down first rises this far (in depth) before going down again,
     * so that repeated notes read as separate strokes
     */
    private static final double RESTRIKE_LIFT_TO = 0.35;
    private static final double RESTRIKE_LIFT_TIME = 0.035;

    private static final double NEVER = Double.POSITIVE_INFINITY;

    private final double[] pressedAt = new double[KEY_COUNT];
    private final double[] releaseAt = new double[KEY_COUNT];
    /** The depth of a key as it was struck, for it to travel down from there */
    private final double[] strikeDepth = new double[KEY_COUNT];
    /** How many held notes are keeping a key down */
    private final int[] heldNotes = new int[KEY_COUNT];

    public KeyboardKeys() {
        Arrays.fill(pressedAt, Double.NaN);
    }


    /**
     * @param soundIndex The index of the played sound within its 7x3 note grid sound array
     * @param pitch The semitones the sound was pitched by
     * @return The key that plays the given note. Notes past the keyboard's 3 octaves
     * play the key of the same note in the nearest octave.
     */
    public static int keyOf(final int soundIndex, final int pitch) {
        final int row = soundIndex / NATURAL_NOTE_PITCHES.length;
        final int column = soundIndex % NATURAL_NOTE_PITCHES.length;
        final int semitone = row * KEYS_PER_OCTAVE + NATURAL_NOTE_PITCHES[column] + pitch;

        final int octave = Math.max(0, Math.min(OCTAVES - 1, Math.floorDiv(semitone, KEYS_PER_OCTAVE)));
        return octave * KEYS_PER_OCTAVE + Math.floorMod(semitone, KEYS_PER_OCTAVE);
    }

    /**
     * @return Whether the given key is a black key
     */
    public static boolean isBlackKey(final int key) {
        return Arrays.binarySearch(NATURAL_NOTE_PITCHES, key % KEYS_PER_OCTAVE) < 0;
    }


    /**
     * Strikes the given key for a single note, letting go of it shortly after
     */
    public void tap(final int key) {
        final double now = now();
        strike(key, now);

        if (heldNotes[key] == 0)
            releaseAt[key] = now + TAP_HOLD;
    }

    /**
     * Strikes the given key and keeps it down until {@link #release released}
     */
    public void hold(final int key) {
        strike(key, now());
        heldNotes[key]++;
        releaseAt[key] = NEVER;
    }

    /**
     * Lets go of a key held with {@link #hold}, once no other held note is keeping it down
     */
    public void release(final int key) {
        if (heldNotes[key] == 0 || --heldNotes[key] > 0)
            return;

        // Let the key reach the bottom first
        releaseAt[key] = Math.max(now(), pressedAt[key] + PRESS_TIME);
    }

    private void strike(final int key, final double now) {
        strikeDepth[key] = depth(key, now);
        pressedAt[key] = now;
    }


    /**
     * @return How far down the given key currently is: 0 at rest and 1 when fully pressed.
     * Briefly goes past 1 as the key bottoms out, and below 0 as it springs back up past its rest position.
     */
    public double depth(final int key) {
        return depth(key, now());
    }

    private double depth(final int key, final double now) {
        if (Double.isNaN(pressedAt[key]))
            return 0;

        if (now < releaseAt[key])
            return pressDepth(now - pressedAt[key], strikeDepth[key]);

        final double sinceRelease = now - releaseAt[key];
        if (sinceRelease >= RELEASE_TIME + BOUNCE_SETTLE_TIME) {
            // Back at rest
            pressedAt[key] = Double.NaN;
            return 0;
        }

        final double releasedFrom = pressDepth(releaseAt[key] - pressedAt[key], strikeDepth[key]);
        return releaseDepth(sinceRelease, releasedFrom);
    }

    /**
     * @param time The time since the key was struck
     * @param from The depth the key was struck at
     */
    private static double pressDepth(double time, double from) {
        // Lift a restruck key a little before taking it down again
        if (from > RESTRIKE_LIFT_TO) {
            if (time < RESTRIKE_LIFT_TIME)
                return lerp(from, RESTRIKE_LIFT_TO, easeOutQuad(time / RESTRIKE_LIFT_TIME));

            time -= RESTRIKE_LIFT_TIME;
            from = RESTRIKE_LIFT_TO;
        }

        // The key speeds up under the finger, then stops hard against the felt
        if (time < PRESS_TIME)
            return lerp(from, 1 + BOTTOM_SQUISH, easeInQuad(time / PRESS_TIME));

        time -= PRESS_TIME;
        if (time < SETTLE_TIME)
            return lerp(1 + BOTTOM_SQUISH, 1, easeOutCubic(time / SETTLE_TIME));

        return 1;
    }

    /**
     * @param time The time since the key was let go of
     * @param from The depth the key was let go of at
     */
    private static double releaseDepth(double time, final double from) {
        // A key let go of halfway down springs back up with less momentum
        final double bounce = -RELEASE_BOUNCE * Math.min(from, 1);

        if (time < RELEASE_TIME)
            return lerp(from, bounce, easeOutCubic(time / RELEASE_TIME));

        time -= RELEASE_TIME;
        return lerp(bounce, 0, easeInOutSine(Math.min(time / BOUNCE_SETTLE_TIME, 1)));
    }


    private static double now() {
        return System.nanoTime() / 1e9;
    }

    private static double lerp(final double from, final double to, final double t) {
        return from + (to - from) * t;
    }
    private static double easeInQuad(final double t) {
        return t * t;
    }
    private static double easeOutQuad(final double t) {
        return 1 - (1 - t) * (1 - t);
    }
    private static double easeOutCubic(final double t) {
        final double inv = 1 - t;
        return 1 - inv * inv * inv;
    }
    private static double easeInOutSine(final double t) {
        return -(Math.cos(Math.PI * t) - 1) / 2;
    }
}
