package com.stump.songcraft_instruments.util;

import com.stump.songcraft_instruments.block.blockentity.looper.LooperConnections;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.LooperUnplayablePacket;
import com.stump.songcraft_instruments.networking.packet.SyncModTagPacket;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public class LooperRecordStateUtil {

    /**
     * Handles as item if {@code hand} is present,
     * as block otherwise
     * @param instrumentClosed Whether this state change was caused by the player closing their instrument,
     *                         rather than by pressing the record button
     */
    public static void handle(ServerPlayer player, Optional<InteractionHand> hand, boolean recording, boolean instrumentClosed) {
        // Group participants may control the session from any instrument
        final Optional<LooperBlockEntity> groupLooper = LooperConnections.getGroupSessionLooper(player);
        if (groupLooper.isPresent()) {
            // Only the record button stops a group session; closing the instrument does not
            if (!recording && !instrumentClosed)
                groupLooper.get().session().stopGroupSession();
            return;
        }

        if (hand.isPresent()) {
            LooperRecordStateUtil.handleItem(player, hand.get(), recording, instrumentClosed);
        } else {
            LooperRecordStateUtil.handleBlock(player, recording, instrumentClosed);
        }
    }

    public static void handleBlock(ServerPlayer player, boolean recording, boolean instrumentClosed) {
        final BlockPos instrumentBlockPos = InstrumentOpenProvider.getBlockPos(player);

        final BlockEntity instrumentBlock = player.level().getBlockEntity(instrumentBlockPos);
        final CompoundTag looperTag = LooperUtil.looperTag(instrumentBlock);

        if (looperTag.isEmpty())
            return;
        if (ServerUtil.isMaliciousPos(player, looperTag))
            return;

        final LooperBlockEntity lbe = LooperUtil.getFromBlockInstrument(player.level(), instrumentBlock);
        if (lbe == null || !LooperUtil.isConnectedBy(lbe, looperTag, player)) {
            notifyLooperUnplayable(player);
            return;
        }

        changeRecordingState(player, lbe, () -> LooperUtil.remLooperTag(instrumentBlock), recording, instrumentClosed);
        SCPacketHandler.sendToClient(new SyncModTagPacket(SCInstrumentMod.modTag(instrumentBlock), instrumentBlockPos), player);
    }

    public static void handleItem(ServerPlayer player, InteractionHand hand, boolean recording, boolean instrumentClosed) {
        final ItemStack instrumentItem = player.getItemInHand(hand);
        final CompoundTag looperTag = LooperUtil.looperTag(instrumentItem);

        if (looperTag.isEmpty())
            return;
        if (ServerUtil.isMaliciousPos(player, looperTag))
            return;


        final LooperBlockEntity lbe = LooperUtil.getFromItemInstrument(player.level(), instrumentItem);
        if (lbe == null || !LooperUtil.isConnectedBy(lbe, looperTag, player)) {
            notifyLooperUnplayable(player);
            return;
        }

        changeRecordingState(player, lbe, () -> LooperUtil.remLooperTag(instrumentItem), recording, instrumentClosed);
    }

    public static void changeRecordingState(ServerPlayer player, LooperBlockEntity lbe,
                                            Runnable looperTagRemover,
                                            boolean recording, boolean instrumentClosed) {

        // A group session the player is not participating in
        if (lbe.session().isGroupSession())
            return;

        // With multiple players connected, recording is done as a group
        // by those on their connected instrument's screen.
        // Recording over a synced looper's record is done as one too, to count in with it.
        final boolean overdub = lbe.getBackingLooper() != null;
        if (recording && (overdub || lbe.connections().getAll().size() > 1)) {
            lbe.connections().disconnectAbsentPlayers();

            if (overdub || lbe.connections().getAll().size() > 1) {
                if (!lbe.session().startGroupSession())
                    notifyLooperUnplayable(player);
                return;
            }
            // Nobody else is present; record solo
        }

        if (recording) {
            if (lbe.session().isLocked() && !lbe.session().isLockedBy(player)) {
                notifyLooperUnplayable(player);
                return;
            }

            LooperUtil.setRecording(player, lbe.getBlockPos());
        } else {
            if (!lbe.session().isLockedBy(player)) {
                // Never started recording; disarm so the looper accepts connections again
                LooperUtil.setNotRecording(player);
                return;
            }

            lbe.lock();

            player.level().setBlockAndUpdate(
                lbe.getBlockPos(),
                lbe.setPlaying(true, lbe.getBlockState())
            );

            looperTagRemover.run();

            LooperUtil.setNotRecording(player);
        }
    }

    /**
     * Restarts the recording the player is taking part in, be it a group or solo recording.
     */
    public static void handleRestart(final ServerPlayer player) {
        final Optional<LooperBlockEntity> groupLooper = LooperConnections.getGroupSessionLooper(player);
        if (groupLooper.isPresent()) {
            groupLooper.get().session().restartGroupSession();
            return;
        }

        if (!LooperUtil.isRecording(player))
            return;

        final BlockPos looperPos = LooperUtil.getRecordingLooperPos(player);
        if (looperPos == null || !player.level().isLoaded(looperPos))
            return;

        final LooperBlockEntity lbe = LooperUtil.getFromPos(player.level(), looperPos);
        if (lbe != null)
            lbe.session().restartSoloRecording(player);
    }

    private static void notifyLooperUnplayable(final ServerPlayer player) {
        SCPacketHandler.sendToClient(new LooperUnplayablePacket(), player);
    }

}
