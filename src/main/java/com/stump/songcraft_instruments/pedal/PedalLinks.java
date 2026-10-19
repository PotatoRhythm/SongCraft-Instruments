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
 * <p>Each pedal has at most one input (the pedal before it) and one output (the pedal after it), so pedals form
 * a single line. A link is stored on both ends and only counts while both agree, so a broken pedal, or a new one
 * placed where an old one was, never leaves a stale link behind.
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
            && from.equals(target.getInput());
    }

    /**
     * @return The pedal the given one is validly plugged into, or null
     */
    @Nullable
    private static PedalBlockEntity downstreamOf(final Level level, final PedalBlockEntity pedal) {
        final BlockPos target = pedal.getOutput();
        return ((target != null) && isLinked(level, pedal.getBlockPos(), target)) ? pedalAt(level, target) : null;
    }


    /**
     * @return The pedal validly plugged into the given one, or null
     */
    @Nullable
    private static PedalBlockEntity upstreamOf(final Level level, final PedalBlockEntity pedal) {
        final BlockPos source = pedal.getInput();
        return ((source != null) && isLinked(level, source, pedal.getBlockPos())) ? pedalAt(level, source) : null;
    }


    //#region Connecting

    /**
     * Plugs the pedal at {@code pedalPos} into the pedal at {@code targetPos}.
     * Whatever the first was plugged into before, and whatever was plugged into the target before, is unplugged.
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
        // The pedals into (and including) this one, then the target and the pedals after it.
        // Whatever plugged into the target is unplugged, so its pedals don't count
        if (upstreamCount(level, pedal) + downstream.size() > PedalChain.MAX_LENGTH)
            return ConnectResult.TOO_LONG;

        unplugOutput(level, pedal);
        unplugInput(level, target);
        pedal.setOutput(targetPos);
        target.setInput(pedalPos);
        PedalMonitorSync.onPedalsChanged(level);
        return ConnectResult.CONNECTED;
    }

    /**
     * Unplugs every cable of the pedal: the ones into it, and the one out of it
     */
    public static void disconnect(final Level level, final PedalBlockEntity pedal) {
        unplugInput(level, pedal);
        unplugOutput(level, pedal);
        PedalMonitorSync.onPedalsChanged(level);
    }

    /**
     * @return Whether any pedal is plugged into the given one, or it into another
     */
    public static boolean isConnected(final Level level, final PedalBlockEntity pedal) {
        return (downstreamOf(level, pedal) != null) || (upstreamOf(level, pedal) != null);
    }

    private static void unplugOutput(final Level level, final PedalBlockEntity pedal) {
        final BlockPos targetPos = pedal.getOutput();
        pedal.setOutput(null);
        if (targetPos == null)
            return;

        final PedalBlockEntity target = pedalAt(level, targetPos);
        if ((target != null) && pedal.getBlockPos().equals(target.getInput()))
            target.setInput(null);
    }
    private static void unplugInput(final Level level, final PedalBlockEntity pedal) {
        final BlockPos sourcePos = pedal.getInput();
        pedal.setInput(null);
        if (sourcePos == null)
            return;

        final PedalBlockEntity source = pedalAt(level, sourcePos);
        if ((source != null) && pedal.getBlockPos().equals(source.getOutput()))
            source.setOutput(null);
    }

    /**
     * @return How many pedals are chained into (and including) the given one
     */
    private static int upstreamCount(final Level level, final PedalBlockEntity pedal) {
        int count = 0;
        // One past the limit is enough to know it's too long, and stops a loop
        for (PedalBlockEntity current = pedal; (current != null) && (count <= PedalChain.MAX_LENGTH); count++)
            current = upstreamOf(level, current);
        return count;
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
     * @return The positions of the pedals an instrument plugged into the given pedal plays through, in signal order,
     * bypassed ones included: that pedal, then each one after it
     */
    public static List<BlockPos> resolvePath(final Level level, final PedalBlockEntity first) {
        final List<BlockPos> pedals = pedalsFrom(level, first);
        return (pedals.size() > PedalChain.MAX_LENGTH) ? pedals.subList(0, PedalChain.MAX_LENGTH) : pedals;
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
