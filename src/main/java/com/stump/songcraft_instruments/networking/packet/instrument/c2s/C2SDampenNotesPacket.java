package com.stump.songcraft_instruments.networking.packet.instrument.c2s;

import com.stump.songcraft_instruments.block.blockentity.looper.LooperConnections;
import com.stump.songcraft_instruments.block.util.LooperSessionState;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.IModPacket;
import com.stump.songcraft_instruments.networking.packet.instrument.s2c.S2CDampenNotesPacket;
import com.stump.songcraft_instruments.util.SpeakerUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

public class C2SDampenNotesPacket implements IModPacket {

    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_SERVER;

    private final int initiatorId;

    public C2SDampenNotesPacket(int initiatorId) {
        this.initiatorId = initiatorId;
    }

    public C2SDampenNotesPacket(FriendlyByteBuf buf) {
        this.initiatorId = buf.readInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(initiatorId);
    }

    @Override
    public void handle(Context context) {
        final ServerPlayer player = context.getSender();

        if (player == null)
            return;

        // Participants dampening any instrument
        LooperConnections.getGroupSessionLooper(player)
            .filter((looper) -> looper.session().getState() == LooperSessionState.RECORDING)
            .filter((looper) -> looper.session().isInRecordRange(player))
            .ifPresent((looper) -> looper.writer().writeDampen(looper.getTicks(), player.getUUID()));

        // Dampening doesn't send individual releases, so release the player's held notes on their speakers too
        SpeakerUtil.releaseHeldNotes(player);

        SCPacketHandler.sendToTrackingEntityAndSelf(
                new S2CDampenNotesPacket(player.getId()),
                player
        );
    }
}