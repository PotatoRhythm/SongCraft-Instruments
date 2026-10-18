package com.stump.songcraft_instruments.pedal;

import javax.annotation.Nullable;
import java.util.function.IntFunction;

/**
 * The kinds of effects pedals apply, and their knobs.
 * <p>The ordinal is sent over the network, and the id saved in recordings, so only ever add new types at the end,
 * and never change an id.</p>
 */
public enum PedalEffectType {
    // Level starts fully up: as loud as the instrument without the pedal, which pedals never go past
    FUZZ("fuzz", Kind.RENDERED, new Knob("drive", 9), new Knob("tone", 8), new Knob("level", 15)),
    // See DelayParams for what the knobs map to
    DELAY("delay", Kind.TIME_BASED, new Knob("time", 7), new Knob("feedback", 6), new Knob("mix", 8)),
    // Both crush knobs turn up towards more crushing (fewer bits, a lower sample rate)
    BITCRUSH("bitcrush", Kind.RENDERED, new Knob("crush", 8), new Knob("downsample", 6), new Knob("mix", 15)),
    // See TremoloParams for what the knobs map to
    TREMOLO("tremolo", Kind.LIVE, new Knob("speed", 7), new Knob("depth", 9), new Knob("shape", 3)),
    // See VibratoParams for what the knobs map to
    VIBRATO("vibrato", Kind.LIVE, new Knob("speed", 7), new Knob("depth", 6), new Knob("shape", 0)),
    // The levels of the octave below, the octave above, and the original note
    OCTAVE("octave", Kind.RENDERED, new Knob("sub", 10), new Knob("up", 0), new Knob("dry", 15)),
    // Each band's boost or cut, flat in the middle; see EqParams
    EQ("eq", Kind.RENDERED,
        new Knob("bass", EqParams.FLAT, EqParams::formatGain),
        new Knob("mid", EqParams.FLAT, EqParams::formatGain),
        new Knob("treble", EqParams.FLAT, EqParams::formatGain)),
    // See ReverbParams for what the knobs map to
    REVERB("reverb", Kind.TIME_BASED, new Knob("decay", 7), new Knob("tone", 8), new Knob("mix", 5)),
    // A worn cassette tape; see LofiParams
    LOFI("lofi", Kind.RENDERED_AND_LIVE, new Knob("age", 8), new Knob("wobble", 6), new Knob("warmth", 7));

    /**
     * How an effect is applied to the notes played through it
     */
    public enum Kind {
        /**
         * Rendered into the sound ahead of time, once per sound file, setting and pitch it's played at
         */
        RENDERED,
        /**
         * Rendered into the sound ahead of time like {@link #RENDERED}, but plays out over time (like echoes),
         * past the end of the note. Held notes only get these on their release.
         */
        TIME_BASED,
        /**
         * Applied to notes while they play (like a tremolo's volume), so it also reaches sustained notes
         * as they're held. Not rendered, so turning its knobs never renders anything.
         */
        LIVE,
        /**
         * Partly rendered (like {@link #RENDERED}) and partly applied live (like {@link #LIVE}),
         * such as a tape's tone, and its wobble
         */
        RENDERED_AND_LIVE
    }

    /**
     * Knob values are quantized to this many steps (0 to {@code KNOB_STEPS - 1}),
     * so turning a knob only ever produces a handful of rendered variants of a sound
     */
    public static final int KNOB_STEPS = 16;
    /**
     * Knobs are packed 4 bits each into an int, see {@link PedalEffect}
     */
    public static final int MAX_KNOBS = 8;

    /**
     * @param name Used in translation keys: {@code gui.songcraft_instruments.pedal.knob.<name>}
     * @param defaultValue From 0 to {@link #KNOB_STEPS} - 1
     * @param formatter Turns the knob's value into what players see, see {@link #format}
     */
    public record Knob(String name, int defaultValue, IntFunction<String> formatter) {
        /**
         * A knob shown from 0 to 10, like most real pedals'
         */
        public Knob(final String name, final int defaultValue) {
            this(name, defaultValue, PedalEffectType::dialValue);
        }

        /**
         * @param value The knob's value, from 0 to {@link #KNOB_STEPS} - 1
         * @return The value as shown to players
         */
        public String format(final int value) {
            return formatter.apply(value);
        }
    }

    /**
     * Used in rendered sound paths and translation keys, so must stay lowercase [a-z]
     */
    public final String id;
    public final Kind kind;
    /**
     * See {@link Kind#TIME_BASED}
     */
    public final boolean timeBased;
    /**
     * Whether any of it is rendered into sounds, see {@link Kind#RENDERED}
     */
    public final boolean rendered;
    /**
     * Whether any of it is applied while notes play, see {@link Kind#LIVE}
     */
    public final boolean live;
    private final Knob[] knobs;

    PedalEffectType(final String id, final Kind kind, final Knob... knobs) {
        if (knobs.length > MAX_KNOBS)
            throw new IllegalArgumentException("A pedal can have at most " + MAX_KNOBS + " knobs");

        this.id = id;
        this.kind = kind;
        this.timeBased = (kind == Kind.TIME_BASED);
        this.rendered = (kind != Kind.LIVE);
        this.live = (kind == Kind.LIVE || kind == Kind.RENDERED_AND_LIVE);
        this.knobs = knobs;
    }

    public int knobCount() {
        return knobs.length;
    }
    public Knob knob(final int index) {
        return knobs[index];
    }

    /**
     * @param value A knob's value, from 0 to {@link #KNOB_STEPS} - 1
     * @return The value from 0 to 10 like a real knob, though it turns in finer steps
     */
    private static String dialValue(final int value) {
        return String.format("%.1f", value * 10f / (KNOB_STEPS - 1));
    }

    public PedalEffect defaultEffect() {
        PedalEffect effect = new PedalEffect(this, 0);
        for (int i = 0; i < knobs.length; i++)
            effect = effect.withKnob(i, knobs[i].defaultValue());
        return effect;
    }

    @Nullable
    public static PedalEffectType byOrdinal(final int ordinal) {
        final PedalEffectType[] types = values();
        return (ordinal >= 0 && ordinal < types.length) ? types[ordinal] : null;
    }
}
