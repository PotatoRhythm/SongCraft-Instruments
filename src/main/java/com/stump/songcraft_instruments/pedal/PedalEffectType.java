package com.stump.songcraft_instruments.pedal;

import javax.annotation.Nullable;

/**
 * The kinds of effects pedals apply, and their knobs.
 * <p>The ordinal and id are sent over the network and saved, so only ever add new types at the end.</p>
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
    OCTAVE("octave", Kind.RENDERED, new Knob("sub", 10), new Knob("up", 0), new Knob("dry", 15));

    /**
     * How an effect is applied to the notes played through it
     */
    public enum Kind {
        /**
         * Rendered into the sound ahead of time, once per sound file and setting
         */
        RENDERED,
        /**
         * Rendered into the sound ahead of time, for the exact pitch a note plays at, as it plays out over time
         * (like echoes): notes are pitched by resampling, which would otherwise stretch or squash its timing.
         * Held notes only get these on their release.
         */
        TIME_BASED,
        /**
         * Applied to notes while they play (like a tremolo's volume), so it also reaches sustained notes
         * as they're held. Not rendered, so turning its knobs never renders anything.
         */
        LIVE
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
     */
    public record Knob(String name, int defaultValue) {}

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
     * See {@link Kind#LIVE}
     */
    public final boolean live;
    private final Knob[] knobs;

    PedalEffectType(final String id, final Kind kind, final Knob... knobs) {
        if (knobs.length > MAX_KNOBS)
            throw new IllegalArgumentException("A pedal can have at most " + MAX_KNOBS + " knobs");

        this.id = id;
        this.kind = kind;
        this.timeBased = (kind == Kind.TIME_BASED);
        this.live = (kind == Kind.LIVE);
        this.knobs = knobs;
    }

    public int knobCount() {
        return knobs.length;
    }
    public Knob knob(final int index) {
        return knobs[index];
    }

    public PedalEffect defaultEffect() {
        PedalEffect effect = new PedalEffect(this, 0);
        for (int i = 0; i < knobs.length; i++)
            effect = effect.withKnob(i, knobs[i].defaultValue());
        return effect;
    }

    @Nullable
    public static PedalEffectType byId(final String id) {
        for (final PedalEffectType type : values())
            if (type.id.equals(id))
                return type;
        return null;
    }
    @Nullable
    public static PedalEffectType byOrdinal(final int ordinal) {
        final PedalEffectType[] types = values();
        return (ordinal >= 0 && ordinal < types.length) ? types[ordinal] : null;
    }
}
