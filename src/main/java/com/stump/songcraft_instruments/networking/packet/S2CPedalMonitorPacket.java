package com.stump.songcraft_instruments.networking.packet;

import com.stump.songcraft_instruments.client.pedal.PedalMonitor;
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
 * Tells a player the pedal chains of the speakers their open instrument plays through,
 * so they hear their own notes through them. See {@code PedalMonitorSync}.
 */
public class S2CPedalMonitorPacket implements IModPacket {
    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_CLIENT;

    /**
     * The most entries read, as many as an instrument can have speakers
     */
    private static final int MAX_ENTRIES = 8;

    /**
     * @param speakerPos Where the speaker is
     * @param chain The speaker's pedals; never empty
     */
    public record Entry(BlockPos speakerPos, PedalChain chain) {}

    private final List<Entry> entries;

    public S2CPedalMonitorPacket(final List<Entry> entries) {
        this.entries = entries;
    }
    public S2CPedalMonitorPacket(final FriendlyByteBuf buf) {
        final int size = buf.readVarInt();
        entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            final Entry entry = new Entry(buf.readBlockPos(), PedalChain.read(buf));
            if (entries.size() < MAX_ENTRIES && !entry.chain().isEmpty())
                entries.add(entry);
        }
    }

    @Override
    public void write(final FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (final Entry entry : entries) {
            buf.writeBlockPos(entry.speakerPos());
            entry.chain().write(buf);
        }
    }

    @Override
    public void handle(final Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PedalMonitor.setSpeakers(entries));
    }
}
