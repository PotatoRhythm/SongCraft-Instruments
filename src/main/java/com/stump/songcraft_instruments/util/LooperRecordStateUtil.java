package com.stump.songcraft_instruments.util;

import com.stump.songcraft_instruments.block.blockentity.looper.LooperConnections;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.LooperUnplayablePacket;
import com.stump.songcraft_instruments.networking.packet.SyncModTagPacket;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.recording.Recording;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
        // Participants may control the session from any instrument
        final Optional<LooperBlockEntity> groupLooper = LooperConnections.getGroupSessionLooper(player);
        if (groupLooper.isPresent()) {
            // Only the record button stops a session; closing the instrument does not
            if (!recording && !instrumentClosed)
                groupLooper.get().session().stopGroupSession();
            return;
        }

        // Without a session, there is nothing to stop
        if (!recording)
            return;

        if (hand.isPresent()) {
            LooperRecordStateUtil.handleItem(player, hand.get());
        } else {
            LooperRecordStateUtil.handleBlock(player);
        }
    }

    public static void handleBlock(ServerPlayer player) {
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

        startRecording(player, lbe);
        SCPacketHandler.sendToClient(new SyncModTagPacket(SCInstrumentMod.modTag(instrumentBlock), instrumentBlockPos), player);
    }

    public static void handleItem(ServerPlayer player, InteractionHand hand) {
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

        startRecording(player, lbe);
    }

    /**
     * Starts a recording session's countdown, be it with one player or many.
     * Its participants are those on their connected instrument's screen.
     */
    public static void startRecording(ServerPlayer player, LooperBlockEntity lbe) {
        // A session the player is not participating in
        if (lbe.session().isGroupSession())
            return;

        lbe.connections().disconnectAbsentPlayers();

        if (lbe.session().wouldExceedMaxParts()) {
            player.displayClientMessage(
                Component.translatable("songcraft_instruments.record.too_many_parts", Recording.MAX_PARTS)
                    .withStyle(ChatFormatting.RED),
                true
            );
            return;
        }

        if (!lbe.session().startGroupSession())
            notifyLooperUnplayable(player);
    }

    /**
     * Restarts the recording session the player is taking part in.
     */
    public static void handleRestart(final ServerPlayer player) {
        LooperConnections.getGroupSessionLooper(player)
            .ifPresent((lbe) -> lbe.session().restartGroupSession());
    }

    private static void notifyLooperUnplayable(final ServerPlayer player) {
        SCPacketHandler.sendToClient(new LooperUnplayablePacket(), player);
    }

}
