package com.stump.songcraft_instruments.networking.packet;

import com.stump.songcraft_instruments.block.blockentity.looper.RecordingSession;
import com.stump.songcraft_instruments.block.blockentity.looper.LooperConnections;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.util.LooperUtil;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.networking.IModPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.Optional;
import java.util.UUID;

public class DoesLooperExistPacket implements IModPacket {
    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_SERVER;
    public static final int MAX_RECORD_DIST = RecordingSession.MAX_RECORD_DIST;

    final Optional<InteractionHand> hand;

    public DoesLooperExistPacket(final InteractionHand hand) {
        this.hand = Optional.of(hand);
    }
    /**
     * Counts this update request as a request for a block instrument
     */
    public DoesLooperExistPacket() {
        this.hand = Optional.empty();
    }
    public DoesLooperExistPacket(FriendlyByteBuf buf) {
        hand = buf.readOptional((fbb) -> fbb.readEnum(InteractionHand.class));
    }

    @Override
    public void write(final FriendlyByteBuf buf) {
        buf.writeOptional(hand, FriendlyByteBuf::writeEnum);
    }

    @Override
    public void handle(final Context context) {
        final ServerPlayer player = context.getSender();
        final Level level = player.level();

        // Group participants are part of the session regardless of the instrument they opened
        final Optional<LooperBlockEntity> groupLooper = LooperConnections.getGroupSessionLooper(player);
        if (groupLooper.isPresent()) {
            groupLooper.get().connections().syncTo(player);
            return;
        }

        LooperBlockEntity looperBE;

        if (hand.isPresent()) {
            final ItemStack instrumentItem = player.getItemInHand(hand.get());
            looperBE = LooperUtil.getFromItemInstrument(level, instrumentItem);

            if (looperBE != null)
                // For items, also check if we are too far away
                if (!looperBE.getBlockPos().closerToCenterThan(player.position(), MAX_RECORD_DIST)) {
                    // Disconnect on the looper's end too, so it doesn't keep counting them as a participant
                    for (final UUID connectionId : LooperUtil.getConnectionIds(LooperUtil.looperTag(instrumentItem)))
                        looperBE.connections().remove(player.getUUID(), connectionId);
                    looperBE = null;
                    LooperUtil.remLooperTag(instrumentItem);
                }

            if (looperBE != null && !LooperUtil.isConnectedBy(looperBE, LooperUtil.looperTag(instrumentItem), player))
                looperBE = null;
        } else {
            final BlockPos instrumentBlockPos = InstrumentOpenProvider.getBlockPos(player);
            final BlockEntity instrumentBlockEntity = level.getBlockEntity(instrumentBlockPos);

            looperBE = LooperUtil.getFromBlockInstrument(level, instrumentBlockEntity);

            // Manually update the tag removal for the client
            if (looperBE == null) {
                SCPacketHandler.sendToClient(
                    new SyncModTagPacket(SCInstrumentMod.modTag(instrumentBlockEntity), instrumentBlockPos), player
                );
            }
            // Block instruments are shared; only the player who connected it may record with it
            else if (!LooperUtil.isConnectedBy(looperBE, LooperUtil.looperTag(instrumentBlockEntity), player)) {
                looperBE = null;
            }
        }

        if (looperBE == null)
            SCPacketHandler.sendToClient(new LooperUnplayablePacket(), player);
        else
            looperBE.connections().sync();
    }
    
}
