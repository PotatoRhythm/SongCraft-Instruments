package com.stump.songcraft_instruments.block.blockentity.microphone;

/**
 * <p>How lively a microphone stand's mic is, as driven by the notes sung into it.</p>
 * The mic swells up while notes are held, gives a little pop with each new note,
 * then eases back down to rest once the last note is let go of.
 * <p>Purely client-side state; it is never saved or synced.</p>
 */
public class MicrophonePulse {
    // All timings are in seconds

    /** How long the mic takes to swell up when a note starts */
    private static final double SWELL_TIME = 0.08;
    /** How long the mic takes to ease back to rest after the last note ends */
    private static final double RELEASE_TIME = 0.3;
    /** How long the pop of a new note lasts */
    private static final double POP_TIME = 0.2;
    /** How long a one-shot note keeps the mic swelled up */
    private static final double TAP_HOLD = 0.15;

    private static final double NEVER = Double.POSITIVE_INFINITY;

    private double swellAt = Double.NaN;
    private double releaseAt = NEVER;
    /** The swell of the mic as the latest note started, for it to swell up from there */
    private double swellFrom;
    private double popAt = Double.NaN;
    /** How many held notes are keeping the mic swelled up */
    private int heldNotes;


    /**
     * Swells the mic up for a single note, letting it ease back shortly after
     */
    public void tap() {
        final double now = now();
        start(now);

        if (heldNotes == 0)
            releaseAt = now + TAP_HOLD;
    }

    /**
     * Swells the mic up until the note is {@link #release released}
     */
    public void hold() {
        start(now());
        heldNotes++;
        releaseAt = NEVER;
    }

    /**
     * Lets go of a note held with {@link #hold}, easing the mic back once no other held note is playing
     */
    public void release() {
        if (heldNotes == 0 || --heldNotes > 0)
            return;

        // Let the mic finish swelling up first
        releaseAt = Math.max(now(), swellAt + SWELL_TIME);
    }

    private void start(final double now) {
        swellFrom = swell(now);
        swellAt = now;
        popAt = now;
    }


    /**
     * @return How swelled up the mic currently is: 0 at rest and 1 while notes are playing
     */
    public double swell() {
        return swell(now());
    }

    private double swell(final double now) {
        if (Double.isNaN(swellAt))
            return 0;

        if (now < releaseAt)
            return swellUp(now - swellAt);

        final double sinceRelease = now - releaseAt;
        if (sinceRelease >= RELEASE_TIME) {
            // Back at rest
            swellAt = Double.NaN;
            return 0;
        }

        return swellUp(releaseAt - swellAt) * (1 - easeOutQuad(sinceRelease / RELEASE_TIME));
    }

    private double swellUp(final double time) {
        return lerp(swellFrom, 1, easeOutCubic(Math.min(time / SWELL_TIME, 1)));
    }

    /**
     * @return The pop of the latest note: a quick bump from 0 up to 1 and back, or 0 once it's over
     */
    public double pop() {
        if (Double.isNaN(popAt))
            return 0;

        final double progress = (now() - popAt) / POP_TIME;
        if (progress >= 1) {
            popAt = Double.NaN;
            return 0;
        }

        // Peaks a quarter of the way in, so the pop hits fast and fades out slower
        return Math.sin(Math.PI * Math.sqrt(progress));
    }


    public static double now() {
        return System.nanoTime() / 1e9;
    }

    private static double lerp(final double from, final double to, final double t) {
        return from + (to - from) * t;
    }
    private static double easeOutQuad(final double t) {
        return 1 - (1 - t) * (1 - t);
    }
    private static double easeOutCubic(final double t) {
        final double inv = 1 - t;
        return 1 - inv * inv * inv;
    }
}
