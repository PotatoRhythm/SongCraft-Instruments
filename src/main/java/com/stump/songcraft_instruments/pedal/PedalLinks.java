package com.stump.songcraft_instruments.pedal;

import com.stump.songcraft_instruments.block.blockentity.PedalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Connects pedals to each other, in signal order. Instruments plug into a pedal (see {@link InstrumentPedals}),
 * and play through it and every pedal after it.
 * <p>Each pedal has one output (the next pedal), but any number of pedals may plug into the same one,
 * so a band can share a pedal at the end of their chains. A link is stored on both ends and only counts while both
 * agree, so a broken pedal, or a new one placed where an old one was, never leaves a stale link behind.
 * No chain through the pedals may be longer than {@link PedalChain#MAX_LENGTH}.</p>
 * <p>Server only.</p>
 */
public final class PedalLinks {
    public enum ConnectResult { CONNECTED, ALREADY_CONNECTED, SAME_BLOCK, NOT_A_PEDAL, LOOP, TOO_LONG }

    private PedalLinks() {}


    /**
     * @return The pedal at the given position, or null if there is none (or it isn't loaded)
     */
    @Nullable
    public static PedalBlockEntity pedalAt(final Level level, final BlockPos pos) {
        return (level.isLoaded(pos) && (level.getBlockEntity(pos) instanceof PedalBlockEntity pedal)) ? pedal : null;
    }

    /**
     * @return Whether the pedal at {@code from} is plugged into the one at {@code to}, with both agreeing
     */
    private static boolean isLinked(final Level level, final BlockPos from, final BlockPos to) {
        final PedalBlockEntity source = pedalAt(level, from), target = pedalAt(level, to);
        return (source != null) && (target != null)
            && to.equals(source.getOutput())
            && target.getInputs().contains(from);
    }

    /**
     * @return The pedal the given one is validly plugged into, or null
     */
    @Nullable
    private static PedalBlockEntity downstreamOf(final Level level, final PedalBlockEntity pedal) {
        final BlockPos target = pedal.getOutput();
        return ((target != null) && isLinked(level, pedal.getBlockPos(), target)) ? pedalAt(level, target) : null;
    }


    //#region Connecting

    /**
     * Plugs the pedal at {@code pedalPos} into the pedal at {@code targetPos}.
     * Whatever the first was plugged into before is unplugged; the target keeps its other inputs.
     */
    public static ConnectResult connect(final Level level, final BlockPos pedalPos, final BlockPos targetPos) {
        if (pedalPos.equals(targetPos))
            return ConnectResult.SAME_BLOCK;

        final PedalBlockEntity pedal = pedalAt(level, pedalPos), target = pedalAt(level, targetPos);
        if (pedal == null || target == null)
            return ConnectResult.NOT_A_PEDAL;
        if (isLinked(level, pedalPos, targetPos))
            return ConnectResult.ALREADY_CONNECTED;

        // The target and the pedals after it: connecting into one of them would loop back to this pedal
        final List<BlockPos> downstream = pedalsFrom(level, target);
        if (downstream.contains(pedalPos))
            return ConnectResult.LOOP;
        // The longest chain of pedals into (and including) this one, then the target's
        if (longestUpstream(level, pedal, 0) + downstream.size() > PedalChain.MAX_LENGTH)
            return ConnectResult.TOO_LONG;

        unplugOutput(level, pedal);
        pedal.setOutput(targetPos);
        target.addInput(pedalPos);
        PedalMonitorSync.onPedalsChanged(level);
        return ConnectResult.CONNECTED;
    }

    /**
     * Unplugs every cable of the pedal: the ones into it, and the one out of it
     */
    public static void disconnect(final Level level, final PedalBlockEntity pedal) {
        for (final BlockPos inputPos : pedal.getInputs()) {
            final PedalBlockEntity input = pedalAt(level, inputPos);
            if ((input != null) && pedal.getBlockPos().equals(input.getOutput()))
                input.setOutput(null);
            pedal.removeInput(inputPos);
        }

        unplugOutput(level, pedal);
        PedalMonitorSync.onPedalsChanged(level);
    }

    /**
     * @return Whether any pedal is plugged into the given one, or it into another
     */
    public static boolean isConnected(final Level level, final PedalBlockEntity pedal) {
        if (downstreamOf(level, pedal) != null)
            return true;
        for (final BlockPos input : pedal.getInputs())
            if (isLinked(level, input, pedal.getBlockPos()))
                return true;
        return false;
    }

    private static void unplugOutput(final Level level, final PedalBlockEntity pedal) {
        final BlockPos targetPos = pedal.getOutput();
        pedal.setOutput(null);
        if (targetPos == null)
            return;

        final PedalBlockEntity target = pedalAt(level, targetPos);
        if (target != null)
            target.removeInput(pedal.getBlockPos());
    }

    /**
     * @param depth How many pedals were already counted after this one, so a loop can't recurse forever
     * @return How many pedals the longest chain into (and including) the given one has
     */
    private static int longestUpstream(final Level level, final PedalBlockEntity pedal, final int depth) {
        // One past the limit is enough to know it's too long
        if (depth > PedalChain.MAX_LENGTH)
            return 1;

        int longest = 0;
        for (final BlockPos inputPos : pedal.getInputs()) {
            if (!isLinked(level, inputPos, pedal.getBlockPos()))
                continue;
            longest = Math.max(longest, longestUpstream(level, pedalAt(level, inputPos), depth + 1));
        }
        return 1 + longest;
    }
    /**
     * @return The positions of the pedals validly chained from (and including) the given one
     */
    private static List<BlockPos> pedalsFrom(final Level level, final PedalBlockEntity first) {
        final List<BlockPos> pedals = new ArrayList<>();
        PedalBlockEntity current = first;
        while (current != null && pedals.size() <= PedalChain.MAX_LENGTH) {
            pedals.add(current.getBlockPos());
            current = downstreamOf(level, current);
        }
        return pedals;
    }

    //#endregion


    /**
     * Works out the effects an instrument plugged into the given pedal plays through, in signal order:
     * that pedal's, then each one after it. A chain stops at a pedal that isn't loaded, so pedals only apply
     * while they are.
     */
    public static PedalChain resolveChain(final Level level, final PedalBlockEntity first) {
        final List<PedalEffect> effects = new ArrayList<>();

        PedalBlockEntity pedal = first;
        for (int i = 0; i < PedalChain.MAX_LENGTH && pedal != null; i++) {
            if (!pedal.isBypassed())
                effects.add(pedal.getEffect());
            pedal = downstreamOf(level, pedal);
        }
        return effects.isEmpty() ? PedalChain.NONE : new PedalChain(effects);
    }

    /**
     * @return The ID of the Lo-fi pedal (the tape) an instrument plugged into the given pedal plays through:
     * the first one switched on in its chain, as in {@link PedalChain#tapeEffect}. Null if there's none.
     */
    @Nullable
    public static UUID findTape(final Level level, final PedalBlockEntity first) {
        PedalBlockEntity pedal = first;
        for (int i = 0; i < PedalChain.MAX_LENGTH && pedal != null; i++) {
            if (!pedal.isBypassed() && pedal.getEffect().type() == PedalEffectType.LOFI)
                return pedal.getPedalId();
            pedal = downstreamOf(level, pedal);
        }
        return null;
    }
}
