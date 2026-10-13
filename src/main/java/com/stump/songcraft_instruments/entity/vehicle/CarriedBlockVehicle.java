package com.stump.songcraft_instruments.entity.vehicle;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * a boat or minecart carrying a looper or speaker. boats render it where a chest boat's chest would be,
 * minecarts as their display block.
 */
public interface CarriedBlockVehicle {
    // How boats place the carried block, relative to the boat's model (see CarriedBlockBoatRenderer)
    float BLOCK_SCALE = 0.75F;
    float BOAT_MODEL_Y = 0.375F;
    // Behind the passenger, from the boat's center
    float BOAT_BLOCK_OFFSET = 0.5F;
    // The block's bottom, same as the chest's
    float BOAT_BLOCK_BOTTOM = -0.1875F, RAFT_BLOCK_BOTTOM = 0.125F;
    // Minecarts render their display block on top of the cart's model, centered
    float MINECART_MODEL_Y = 0.375F;

    /**
     * How far above the carried block's top note particles spawn; the same as above a placed looper or speaker
     */
    double PARTICLE_HEIGHT = 0.3;

    BlockState getCarriedBlockState();

    /**
     * @return Where note particles spawn: just above the center of the carried block's top
     */
    Vec3 getNoteParticlePos();


    static Vec3 boatParticlePos(final Boat boat) {
        final float bottom = (boat.getVariant() == Boat.Type.BAMBOO) ? RAFT_BLOCK_BOTTOM : BOAT_BLOCK_BOTTOM;
        // The renderer rotates by 180 - yaw, then moves the block towards +Z
        final float yaw = boat.getYRot() * Mth.DEG_TO_RAD;
        return boat.position().add(
            Mth.sin(yaw) * BOAT_BLOCK_OFFSET,
            BOAT_MODEL_Y + bottom + BLOCK_SCALE + PARTICLE_HEIGHT,
            -Mth.cos(yaw) * BOAT_BLOCK_OFFSET
        );
    }

    static Vec3 minecartParticlePos(final AbstractMinecart minecart) {
        final double bottom = MINECART_MODEL_Y + (minecart.getDisplayOffset() - 8) / 16.0 * BLOCK_SCALE;
        return minecart.position().add(0, bottom + BLOCK_SCALE + PARTICLE_HEIGHT, 0);
    }
}
