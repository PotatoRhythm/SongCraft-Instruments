package com.stump.songcraft_instruments.networking.packet;

import com.stump.songcraft_instruments.client.pedal.PedalMonitor;
import com.stump.songcraft_instruments.networking.IModPacket;
import com.stump.songcraft_instruments.pedal.PedalChain;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Tells a player the pedals their open instrument plays through, so they hear their own notes through them.
 * See {@code PedalMonitorSync}.
 */
public class S2CPedalMonitorPacket implements IModPacket {
    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_CLIENT;

    private final PedalChain chain;

    public S2CPedalMonitorPacket(final PedalChain chain) {
        this.chain = chain;
    }
    public S2CPedalMonitorPacket(final FriendlyByteBuf buf) {
        chain = PedalChain.read(buf);
    }

    @Override
    public void write(final FriendlyByteBuf buf) {
        chain.write(buf);
    }

    @Override
    public void handle(final Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PedalMonitor.setChain(chain));
    }
}
