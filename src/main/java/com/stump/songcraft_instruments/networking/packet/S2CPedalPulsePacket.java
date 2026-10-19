package com.stump.songcraft_instruments.networking.packet;

import com.stump.songcraft_instruments.client.pedal.PedalPulseRenderer;
import com.stump.songcraft_instruments.networking.IModPacket;
import com.stump.songcraft_instruments.pedal.PedalChain;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a note going through a chain of pedals. See {@code PedalPulses}.
 */
public class S2CPedalPulsePacket implements IModPacket {
    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_CLIENT;

    private final List<BlockPos> pedals;
    private final int rgb;

    public S2CPedalPulsePacket(final List<BlockPos> pedals, final int rgb) {
        this.pedals = List.copyOf(pedals);
        this.rgb = rgb;
    }
    public S2CPedalPulsePacket(final FriendlyByteBuf buf) {
        final int count = Math.min(buf.readVarInt(), PedalChain.MAX_LENGTH);
        final List<BlockPos> read = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            read.add(buf.readBlockPos());
        pedals = read;
        rgb = buf.readInt();
    }

    @Override
    public void write(final FriendlyByteBuf buf) {
        buf.writeVarInt(pedals.size());
        for (final BlockPos pedal : pedals)
            buf.writeBlockPos(pedal);
        buf.writeInt(rgb);
    }

    @Override
    public void handle(final Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PedalPulseRenderer.addPulse(pedals, rgb));
    }
}
