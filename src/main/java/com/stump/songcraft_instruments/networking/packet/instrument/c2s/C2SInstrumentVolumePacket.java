package com.stump.songcraft_instruments.networking.packet.instrument.c2s;

import com.stump.songcraft_instruments.block.blockentity.looper.LooperConnections;
import com.stump.songcraft_instruments.capability.recording.RecordingCapabilityProvider;
import com.stump.songcraft_instruments.networking.IModPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Tells the server the player's instrument volume,
 * so the other players connected to their looper can see it.
 */
public class C2SInstrumentVolumePacket implements IModPacket {
    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_SERVER;

    // In percent
    private final int volume;

    public C2SInstrumentVolumePacket(final int volume) {
        this.volume = volume;
    }
    public C2SInstrumentVolumePacket(final FriendlyByteBuf buf) {
        volume = buf.readVarInt();
    }

    @Override
    public void write(final FriendlyByteBuf buf) {
        buf.writeVarInt(volume);
    }

    @Override
    public void handle(final Context context) {
        final ServerPlayer player = context.getSender();
        if (player == null)
            return;

        final int clamped = Math.max(0, Math.min(100, volume));
        if (RecordingCapabilityProvider.getVolume(player) == clamped)
            return;

        RecordingCapabilityProvider.setVolume(player, clamped);
        LooperConnections.getConnectedLooper(player).ifPresent((lbe) -> lbe.connections().sync());
    }
}
