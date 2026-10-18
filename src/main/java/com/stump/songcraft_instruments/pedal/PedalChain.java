package com.stump.songcraft_instruments.pedal;

import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The effects an instrument's notes go through, in signal order: from the pedal the instrument plugs into,
 * through each pedal after it. Bypassed pedals are left out.
 * <p>Sent along with every note of the instrument, wherever it's heard (a few bytes per pedal), to its own player
 * so they hear their own notes with it too, and saved with each recorded note by its key.</p>
 */
public record PedalChain(List<PedalEffect> effects) {
    /**
     * The most pedals one chain can have
     */
    public static final int MAX_LENGTH = 4;
    public static final PedalChain NONE = new PedalChain(List.of());

    private static final String KEY_SEPARATOR = "_";

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
     * @return The part of this chain rendered into sounds: without its purely live effects
     * (see {@link PedalEffectType.Kind#LIVE}). Live effects apply on top of it, as if they came last in the chain.
     */
    public PedalChain rendered() {
        for (final PedalEffect effect : effects)
            if (!effect.type().rendered)
                return new PedalChain(effects.stream().filter((rendered) -> rendered.type().rendered).toList());
        return this;
    }
    /**
     * @return Whether any of the effects is applied to notes while they play, see {@link PedalEffectType.Kind#LIVE}
     */
    public boolean hasLive() {
        for (final PedalEffect effect : effects)
            if (effect.type().live)
                return true;
        return false;
    }

    /**
     * @param seconds The time on a smooth, free-running clock
     * @return The volume the chain's live effects give a note at that time, from 0 to 1
     */
    public double liveGainAt(final double seconds) {
        double gain = 1;
        for (final PedalEffect effect : effects) {
            if (effect.type() == PedalEffectType.TREMOLO)
                gain *= TremoloParams.of(effect).gainAt(seconds);
        }
        return gain;
    }

    /**
     * @param seconds The time on a smooth, free-running clock, for effects that only need to be steady
     * @param tapeSeconds The time on the world's game clock, the same for every player, for effects whose randomness
     *                    every player should hear alike (a tape's wobble); see {@link LofiParams}
     * @return The pitch multiplier the chain's live effects give a note at that time
     */
    public double livePitchAt(final double seconds, final double tapeSeconds) {
        double pitch = 1;
        for (final PedalEffect effect : effects) {
            if (effect.type() == PedalEffectType.VIBRATO)
                pitch *= VibratoParams.of(effect).pitchAt(seconds);
            else if (effect.type() == PedalEffectType.LOFI)
                pitch *= LofiParams.of(effect).pitchAt(tapeSeconds);
        }
        return pitch;
    }

    /**
     * @return The chain's Lo-fi effect (the first, if it has several), whose tape hiss rolls while its instrument
     * plays; or null if it has none
     */
    @Nullable
    public PedalEffect tapeEffect() {
        for (final PedalEffect effect : effects)
            if (effect.type() == PedalEffectType.LOFI)
                return effect;
        return null;
    }
    /**
     * @return Whether the chain has an effect of the given type
     */
    public boolean has(final PedalEffectType type) {
        for (final PedalEffect effect : effects)
            if (effect.type() == type)
                return true;
        return false;
    }

    /**
     * @return How long the chain's effects ring on after a note ends (echoes and reverb), in seconds
     */
    public double tailSeconds() {
        double tail = 0;
        for (final PedalEffect effect : effects) {
            if (effect.type() == PedalEffectType.DELAY)
                tail += DelayParams.of(effect).tailSeconds();
            else if (effect.type() == PedalEffectType.REVERB)
                tail += ReverbParams.of(effect).tailSeconds();
        }
        return Math.min(tail, DelayParams.MAX_TAIL_SECONDS);
    }

    /**
     * How the chain's effects that play out over time ring with a note's sound after it: echoes, reverb, and the
     * reverb of echoes (or echoes of reverb), as they add up in order.
     * <p>Each effect's own ringing goes by its knobs: a delay's echoes every echo time, each feedback times as loud
     * as the last, and a reverb's ringing dying away evenly over its decay, as loud in all as its mix. Counted in
     * energy, as what's rung with adds up at a random phase. A delay's echoes also darken as they repeat,
     * which isn't counted.</p>
     * @return The share of all the energy they ring with that comes the given number of ticks after the sound
     * that rings with it, by tick. Adds up to 1, or empty if nothing in the chain rings on.
     */
    public double[] ringingByTick() {
        // The sound itself, then each effect's ringing added to everything before it
        double[] ringing = {1};
        for (final PedalEffect effect : effects) {
            final double[] own;
            if (effect.type() == PedalEffectType.DELAY) {
                final DelayParams params = DelayParams.of(effect);
                own = new double[ticks(params.tailSeconds()) + 1];
                double echo = params.mix();
                for (int n = 1; n * params.seconds() <= params.tailSeconds() + 1e-9; n++) {
                    own[Math.min(own.length - 1, (int) Math.round(n * params.seconds() * 20))] += echo * echo;
                    echo *= params.feedback();
                }
            } else if (effect.type() == PedalEffectType.REVERB) {
                final ReverbParams params = ReverbParams.of(effect);
                own = new double[ticks(params.tailSeconds()) + 1];
                // Dying away by 60 dB over its decay, tick by tick, and as loud in all as its mix
                final double perTick = Math.pow(10, -6 / (params.decaySeconds() * 20));
                final double total = 1 - Math.pow(perTick, own.length);
                for (int n = 0; n < own.length && total > 0; n++)
                    own[n] = params.mix() * params.mix() * Math.pow(perTick, n) * (1 - perTick) / total;
            } else {
                continue;
            }
            own[0] += 1;

            final double[] combined = new double[ringing.length + own.length - 1];
            for (int i = 0; i < ringing.length; i++)
                for (int j = 0; j < own.length; j++)
                    combined[i + j] += ringing[i] * own[j];
            ringing = combined;
        }

        ringing[0] -= 1;
        double sum = 0;
        for (final double share : ringing)
            sum += share;
        if (sum <= 1e-12)
            return new double[0];
        for (int i = 0; i < ringing.length; i++)
            ringing[i] /= sum;
        return ringing;
    }
    private static int ticks(final double seconds) {
        return (int) Math.ceil(seconds * 20);
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
}
