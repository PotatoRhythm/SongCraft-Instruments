package com.stump.songcraft_instruments.pedal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * The effects a speaker's notes go through, in signal order: from the pedal the instrument plugs into,
 * to the pedal plugged into the speaker. Bypassed pedals are left out.
 * <p>Sent along with every note a speaker relays (a few bytes per pedal), and to players whose instrument
 * plays through the speaker, so they hear their own notes with it too.</p>
 */
public record PedalChain(List<PedalEffect> effects) {
    /**
     * The most pedals one chain can have
     */
    public static final int MAX_LENGTH = 4;
    public static final PedalChain NONE = new PedalChain(List.of());

    private static final String KEY_SEPARATOR = "_";
    private static final String TYPE_TAG = "type", KNOBS_TAG = "knobs";

    public PedalChain {
        effects = List.copyOf(effects);
    }

    public boolean isEmpty() {
        return effects.isEmpty();
    }

    /**
     * @return Whether any of the effects plays out over time, see {@link PedalEffectType#timeBased}
     */
    public boolean isTimeBased() {
        for (final PedalEffect effect : effects)
            if (effect.type().timeBased)
                return true;
        return false;
    }
    /**
     * @return This chain without its effects that play out over time, as held notes are played through
     * until their release
     */
    public PedalChain withoutTimeBased() {
        if (!isTimeBased())
            return this;
        return new PedalChain(effects.stream().filter((effect) -> !effect.type().timeBased).toList());
    }

    /**
     * @return How long the chain's effects ring on after a note ends (its echoes), in seconds
     */
    public double tailSeconds() {
        double tail = 0;
        for (final PedalEffect effect : effects) {
            if (effect.type() == PedalEffectType.DELAY)
                tail += DelayParams.of(effect).tailSeconds();
        }
        return Math.min(tail, DelayParams.MAX_TAIL_SECONDS);
    }

    /**
     * @return This chain as part of a rendered sound's path, e.g. {@code fuzz9a8_fuzz3f2}. Parsed back by {@link #fromKey}.
     */
    public String key() {
        final StringBuilder key = new StringBuilder();
        for (final PedalEffect effect : effects) {
            if (!key.isEmpty())
                key.append(KEY_SEPARATOR);
            key.append(effect.key());
        }
        return key.toString();
    }
    /**
     * @return The chain of the given {@link #key()}, or null if it isn't one
     */
    public static PedalChain fromKey(final String key) {
        final List<PedalEffect> effects = new ArrayList<>();
        for (final String effectKey : key.split(KEY_SEPARATOR)) {
            final PedalEffect effect = PedalEffect.fromKey(effectKey);
            if (effect == null || effects.size() >= MAX_LENGTH)
                return null;
            effects.add(effect);
        }
        return new PedalChain(effects);
    }


    public void write(final FriendlyByteBuf buf) {
        buf.writeByte(effects.size());
        for (final PedalEffect effect : effects) {
            buf.writeByte(effect.type().ordinal());
            buf.writeInt(effect.knobs());
        }
    }
    public static PedalChain read(final FriendlyByteBuf buf) {
        final int size = buf.readByte();
        if (size == 0)
            return NONE;

        final List<PedalEffect> effects = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            final PedalEffectType type = PedalEffectType.byOrdinal(buf.readByte());
            final int knobs = buf.readInt();
            // Never trust the size we're given beyond what a chain can be
            if (type != null && effects.size() < MAX_LENGTH)
                effects.add(new PedalEffect(type, knobs));
        }
        return new PedalChain(effects);
    }

    public ListTag save() {
        final ListTag list = new ListTag();
        for (final PedalEffect effect : effects) {
            final CompoundTag tag = new CompoundTag();
            tag.putString(TYPE_TAG, effect.type().id);
            tag.putInt(KNOBS_TAG, effect.knobs());
            list.add(tag);
        }
        return list;
    }
    public static PedalChain load(final ListTag list) {
        final List<PedalEffect> effects = new ArrayList<>();
        for (int i = 0; i < list.size() && effects.size() < MAX_LENGTH; i++) {
            final CompoundTag tag = list.getCompound(i);
            final PedalEffectType type = PedalEffectType.byId(tag.getString(TYPE_TAG));
            if (type != null)
                effects.add(new PedalEffect(type, tag.getInt(KNOBS_TAG)));
        }
        return effects.isEmpty() ? NONE : new PedalChain(effects);
    }
    public static PedalChain load(final CompoundTag parent, final String key) {
        return load(parent.getList(key, Tag.TAG_COMPOUND));
    }
}
