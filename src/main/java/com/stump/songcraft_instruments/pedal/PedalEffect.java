package com.stump.songcraft_instruments.pedal;

import net.minecraft.util.Mth;

/**
 * One pedal's effect: its type, and the value of each of its knobs.
 * @param knobs Each knob's value, 4 bits each from the lowest bits up (see {@link PedalEffectType#KNOB_STEPS}).
 *              Packed into an int so effects compare and hash by value, and stay small over the network.
 */
public record PedalEffect(PedalEffectType type, int knobs) {
    private static final int KNOB_BITS = 4, KNOB_MASK = (1 << KNOB_BITS) - 1;

    /**
     * @return The knob's value, from 0 to {@link PedalEffectType#KNOB_STEPS} - 1
     */
    public int knob(final int index) {
        return (knobs >>> (index * KNOB_BITS)) & KNOB_MASK;
    }
    /**
     * @return The knob's value, from 0 to 1
     */
    public float knobFraction(final int index) {
        return knob(index) / (float) (PedalEffectType.KNOB_STEPS - 1);
    }

    public PedalEffect withKnob(final int index, final int value) {
        final int clamped = Mth.clamp(value, 0, PedalEffectType.KNOB_STEPS - 1);
        final int shift = index * KNOB_BITS;
        return new PedalEffect(type, (knobs & ~(KNOB_MASK << shift)) | (clamped << shift));
    }

    /**
     * @return This effect as part of a rendered sound's path, e.g. {@code fuzz9a8}: the type's id followed by
     * one hex digit per knob. Parsed back by {@link #fromKey}.
     */
    public String key() {
        final StringBuilder key = new StringBuilder(type.id);
        for (int i = 0; i < type.knobCount(); i++)
            key.append(Character.forDigit(knob(i), PedalEffectType.KNOB_STEPS));
        return key.toString();
    }
    /**
     * @return The effect of the given {@link #key()}, or null if it isn't one
     */
    public static PedalEffect fromKey(final String key) {
        for (final PedalEffectType type : PedalEffectType.values()) {
            if (!key.startsWith(type.id) || key.length() != type.id.length() + type.knobCount())
                continue;

            PedalEffect effect = new PedalEffect(type, 0);
            for (int i = 0; i < type.knobCount(); i++) {
                final int value = Character.digit(key.charAt(type.id.length() + i), PedalEffectType.KNOB_STEPS);
                if (value < 0)
                    return null;
                effect = effect.withKnob(i, value);
            }
            return effect;
        }
        return null;
    }
}
