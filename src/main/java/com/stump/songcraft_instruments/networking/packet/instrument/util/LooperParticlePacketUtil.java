package com.stump.songcraft_instruments.networking.packet.instrument.util;

import com.stump.songcraft_instruments.entity.vehicle.CarriedBlockVehicle;
import com.stump.songcraft_instruments.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class LooperParticlePacketUtil {

    public static void spawnLooperParticle(BlockPos pos, int rgb, double size) {
        spawnParticle(new Vec3(pos.getX() + 0.5, pos.getY() + 1.25, pos.getZ() + 0.5), rgb, size);
    }

    /**
     * Spawns the particle on top of the looper or speaker carried by a boat or minecart,
     * where this client currently shows it
     */
    public static void spawnVehicleParticle(int entityId, int rgb, double size) {
        Level level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(entityId) instanceof CarriedBlockVehicle vehicle)
            spawnParticle(vehicle.getNoteParticlePos(), rgb, size);
    }

    private static void spawnParticle(Vec3 pos, int rgb, double size) {
        Level level = Minecraft.getInstance().level;
        if (level == null)
            return;

        double xOffset = (level.random.nextDouble() - 0.5) * 0.30;
        double yOffset = (level.random.nextDouble() - 0.5) * 0.20;
        double zOffset = (level.random.nextDouble() - 0.5) * 0.30;

        level.addParticle(
                ModParticles.CUSTOM_NOTE.get(),
                pos.x() + xOffset,
                pos.y() + yOffset,
                pos.z() + zOffset,
                rgb,
                size,
                0
        );
    }
}
