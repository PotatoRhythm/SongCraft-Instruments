package com.stump.songcraft_instruments.pedal;

import javax.annotation.Nullable;

/**
 * The kinds of effects pedals apply, and their knobs.
 * <p>The ordinal and id are sent over the network and saved, so only ever add new types at the end.</p>
 */
public enum PedalEffectType {
    // Level starts fully up: as loud as the instrument without the pedal, which pedals never go past
    FUZZ("fuzz", false, new Knob("drive", 9), new Knob("tone", 8), new Knob("level", 15)),
    // See DelayParams for what the knobs map to
    DELAY("delay", true, new Knob("time", 7), new Knob("feedback", 6), new Knob("mix", 8)),
    // Both crush knobs turn up towards more crushing (fewer bits, a lower sample rate)
    BITCRUSH("bitcrush", false, new Knob("crush", 8), new Knob("downsample", 6), new Knob("mix", 15));

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
    /**
     * Whether the effect plays out over time (like echoes), so has to be rendered for the exact pitch a note
     * plays at: notes are pitched by resampling, which would otherwise stretch or squash its timing.
     * Held notes only get such effects on their release.
     */
    public final boolean timeBased;
    private final Knob[] knobs;

    PedalEffectType(final String id, final boolean timeBased, final Knob... knobs) {
        if (knobs.length > MAX_KNOBS)
            throw new IllegalArgumentException("A pedal can have at most " + MAX_KNOBS + " knobs");

        this.id = id;
        this.timeBased = timeBased;
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
