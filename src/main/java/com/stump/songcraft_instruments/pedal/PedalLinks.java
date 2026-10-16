package com.stump.songcraft_instruments.pedal;

import com.stump.songcraft_instruments.block.blockentity.PedalBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.SpeakerBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.speaker.Speaker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Connects pedals to each other and to speaker blocks, in signal order: instrument → pedal → pedal → speaker.
 * <p>Each pedal has one output (the next pedal or a speaker), and each pedal and speaker takes one pedal as input.
 * A link is stored on both ends and only counts while both agree, so a broken block, or a new one placed where an old
 * one was, never leaves a stale link behind. Chains are at most {@link PedalChain#MAX_LENGTH} pedals long.</p>
 * <p>Whenever a chain changes, the speaker at its end is told, and works out its new {@link PedalChain} itself.
 * Server only.</p>
 */
public final class PedalLinks {
    public enum ConnectResult { CONNECTED, ALREADY_CONNECTED, SAME_BLOCK, NOT_A_TARGET, LOOP, TOO_LONG }

    private PedalLinks() {}


    /**
     * @return The pedal at the given position, or null if there is none (or it isn't loaded)
     */
    @Nullable
    public static PedalBlockEntity pedalAt(final Level level, final BlockPos pos) {
        return (level.isLoaded(pos) && (level.getBlockEntity(pos) instanceof PedalBlockEntity pedal)) ? pedal : null;
    }
    /**
     * @return The speaker block's speaker at the given position, or null if there is none (or it isn't loaded)
     */
    @Nullable
    public static Speaker speakerAt(final Level level, final BlockPos pos) {
        return (level.isLoaded(pos) && (level.getBlockEntity(pos) instanceof SpeakerBlockEntity sbe)) ? sbe.speaker() : null;
    }
    /**
     * @return What a pedal can be plugged into at the given position (a pedal or a speaker block), or null
     */
    @Nullable
    private static PedalInput inputAt(final Level level, final BlockPos pos) {
        final PedalBlockEntity pedal = pedalAt(level, pos);
        return (pedal != null) ? pedal : speakerAt(level, pos);
    }

    /**
     * @return Whether the pedal at {@code pedalPos} is plugged into whatever is at {@code targetPos}, with both agreeing
     */
    private static boolean isLinked(final Level level, final BlockPos pedalPos, final BlockPos targetPos) {
        final PedalBlockEntity pedal = pedalAt(level, pedalPos);
        final PedalInput target = inputAt(level, targetPos);
        return (pedal != null) && (target != null)
            && targetPos.equals(pedal.getOutput())
            && pedalPos.equals(target.getPedalInput());
    }

    /**
     * @return The pedal validly plugged into the given input, or null
     */
    @Nullable
    private static PedalBlockEntity upstreamOf(final Level level, final BlockPos inputPos, final PedalInput input) {
        final BlockPos pedalPos = input.getPedalInput();
        return ((pedalPos != null) && isLinked(level, pedalPos, inputPos)) ? pedalAt(level, pedalPos) : null;
    }
    /**
     * @return The position the given pedal is validly plugged into, or null
     */
    @Nullable
    private static BlockPos downstreamOf(final Level level, final PedalBlockEntity pedal) {
        final BlockPos target = pedal.getOutput();
        return ((target != null) && isLinked(level, pedal.getBlockPos(), target)) ? target : null;
    }


    //#region Connecting

    /**
     * Plugs the pedal at {@code pedalPos} into the pedal or speaker block at {@code targetPos}.
     * Whatever either was plugged into before is unplugged.
     */
    public static ConnectResult connect(final Level level, final BlockPos pedalPos, final BlockPos targetPos) {
        if (pedalPos.equals(targetPos))
            return ConnectResult.SAME_BLOCK;

        final PedalBlockEntity pedal = pedalAt(level, pedalPos);
        final PedalInput target = inputAt(level, targetPos);
        if (pedal == null || target == null)
            return ConnectResult.NOT_A_TARGET;
        if (isLinked(level, pedalPos, targetPos))
            return ConnectResult.ALREADY_CONNECTED;

        // Pedals after the target, including itself: connecting into one of them would loop back to this pedal
        final List<BlockPos> downstream = pedalsFrom(level, targetPos);
        if (downstream.contains(pedalPos))
            return ConnectResult.LOOP;
        // This pedal and the ones before it, then the target's (the target's old input is unplugged)
        if (countUpstream(level, pedal) + downstream.size() > PedalChain.MAX_LENGTH)
            return ConnectResult.TOO_LONG;

        unplugOutput(level, pedal);
        final PedalBlockEntity oldUpstream = upstreamOf(level, targetPos, target);
        if (oldUpstream != null)
            oldUpstream.setOutput(null);

        pedal.setOutput(targetPos);
        target.setPedalInput(pedalPos);
        notifySpeaker(level, pedalPos);
        return ConnectResult.CONNECTED;
    }

    /**
     * Unplugs both of the pedal's cables: the one into it, and the one out of it
     */
    public static void disconnect(final Level level, final PedalBlockEntity pedal) {
        final PedalBlockEntity upstream = upstreamOf(level, pedal.getBlockPos(), pedal);
        if (upstream != null)
            upstream.setOutput(null);
        pedal.setPedalInput(null);

        unplugOutput(level, pedal);
    }

    /**
     * @return Whether the pedal has a cable into it or out of it
     */
    public static boolean isConnected(final Level level, final PedalBlockEntity pedal) {
        return (upstreamOf(level, pedal.getBlockPos(), pedal) != null) || (downstreamOf(level, pedal) != null);
    }

    private static void unplugOutput(final Level level, final PedalBlockEntity pedal) {
        final BlockPos target = downstreamOf(level, pedal);
        pedal.setOutput(null);
        if (target == null)
            return;

        final PedalInput input = inputAt(level, target);
        if (input != null)
            input.setPedalInput(null);
        // The speaker the pedal fed into has lost it (and everything before it)
        notifySpeaker(level, target);
    }

    /**
     * @return How many pedals are validly chained into (and including) the given one
     */
    private static int countUpstream(final Level level, final PedalBlockEntity pedal) {
        int count = 1;
        PedalBlockEntity current = pedal;
        // One past the limit is enough to know it's too long
        while (count <= PedalChain.MAX_LENGTH) {
            current = upstreamOf(level, current.getBlockPos(), current);
            if (current == null)
                break;
            count++;
        }
        return count;
    }
    /**
     * @return The positions of the pedals validly chained from (and including) the given position, if it is a pedal
     */
    private static List<BlockPos> pedalsFrom(final Level level, final BlockPos pos) {
        final List<BlockPos> pedals = new ArrayList<>();
        PedalBlockEntity current = pedalAt(level, pos);
        while (current != null && pedals.size() <= PedalChain.MAX_LENGTH) {
            pedals.add(current.getBlockPos());
            final BlockPos next = downstreamOf(level, current);
            current = (next == null) ? null : pedalAt(level, next);
        }
        return pedals;
    }

    //#endregion


    //#region Resolving

    /**
     * Tells the speaker at the end of the chain going through the given position (a pedal or a speaker block)
     * that its pedals changed.
     */
    public static void notifySpeaker(final Level level, final BlockPos pos) {
        final Speaker speaker = findSpeaker(level, pos);
        if (speaker != null)
            speaker.onPedalsChanged();
    }
    /**
     * @return The speaker at the end of the chain going through the given position (a pedal or a speaker block),
     * or null if the chain doesn't end at a (loaded) speaker
     */
    @Nullable
    public static Speaker findSpeaker(final Level level, final BlockPos pos) {
        BlockPos current = pos;
        for (int i = 0; i <= PedalChain.MAX_LENGTH; i++) {
            final Speaker speaker = speakerAt(level, current);
            if (speaker != null)
                return speaker;

            final PedalBlockEntity pedal = pedalAt(level, current);
            if (pedal == null)
                return null;
            current = downstreamOf(level, pedal);
            if (current == null)
                return null;
        }
        return null;
    }

    /**
     * Works out the chain of effects plugged into a speaker block, in signal order.
     * @return The chain, or null if part of it isn't loaded (and so can't be worked out right now)
     */
    @Nullable
    public static PedalChain resolveChain(final Level level, final BlockPos speakerPos, final PedalInput speaker) {
        final List<PedalEffect> effects = new ArrayList<>();

        BlockPos inputPos = speakerPos;
        PedalInput input = speaker;
        for (int i = 0; i < PedalChain.MAX_LENGTH; i++) {
            final BlockPos pedalPos = input.getPedalInput();
            if (pedalPos == null)
                break;
            if (!level.isLoaded(pedalPos))
                return null;
            final PedalBlockEntity pedal = pedalAt(level, pedalPos);
            if (pedal == null || !isLinked(level, pedalPos, inputPos))
                break;

            if (!pedal.isBypassed())
                effects.add(0, pedal.getEffect());

            inputPos = pedalPos;
            input = pedal;
        }

        return effects.isEmpty() ? PedalChain.NONE : new PedalChain(effects);
    }

    //#endregion
}
