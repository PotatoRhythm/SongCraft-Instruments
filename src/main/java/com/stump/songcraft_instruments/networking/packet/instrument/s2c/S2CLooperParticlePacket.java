package com.stump.songcraft_instruments.networking.packet.instrument.s2c;

import com.stump.songcraft_instruments.networking.IModPacket;
import com.stump.songcraft_instruments.networking.packet.instrument.util.LooperParticlePacketUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

/**
 * Spawns a note particle above a looper or speaker: a block, or a boat or minecart carrying one.
 * Vehicles are sent by their entity ID, so the particle is placed where the client currently shows them.
 */
public class S2CLooperParticlePacket implements IModPacket {

    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_CLIENT;

    private static final int NO_ENTITY = -1;

    private final BlockPos pos;
    private final int entityId;
    private final int rgb;
    private final double size;

    public S2CLooperParticlePacket(BlockPos pos, int rgb, double size) {
        this(pos, NO_ENTITY, rgb, size);
    }
    /**
     * @param vehicle A boat or minecart carrying a looper or speaker
     */
    public S2CLooperParticlePacket(Entity vehicle, int rgb, double size) {
        this(vehicle.blockPosition(), vehicle.getId(), rgb, size);
    }
    private S2CLooperParticlePacket(BlockPos pos, int entityId, int rgb, double size) {
        this.pos = pos;
        this.entityId = entityId;
        this.rgb = rgb;
        this.size = size;
    }

    public S2CLooperParticlePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.entityId = buf.readVarInt();
        this.rgb = buf.readInt();
        this.size = buf.readDouble();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(entityId);
        buf.writeInt(rgb);
        buf.writeDouble(size);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        if (entityId == NO_ENTITY)
            LooperParticlePacketUtil.spawnLooperParticle(pos, rgb, size);
        else
            LooperParticlePacketUtil.spawnVehicleParticle(entityId, rgb, size);
        context.setPacketHandled(true);
    }
}
