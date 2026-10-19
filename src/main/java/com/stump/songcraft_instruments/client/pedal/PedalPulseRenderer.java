package com.stump.songcraft_instruments.client.pedal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.List;

/**
 * Shows a note going through a chain of pedals: a few specks of dust in the note's color rise from every pedal in
 * the chain at once. Sent by {@code PedalPulses} when a player plays.
 */
@OnlyIn(Dist.CLIENT)
public final class PedalPulseRenderer {
    private static final int SPECKS_PER_PEDAL = 3;
    private static final float SPECK_SIZE = 0.6f;
    /**
     * How far (in blocks) specks spread around a pedal's middle, sideways and up
     */
    private static final double SPREAD = 0.3, RISE = 0.15;

    private PedalPulseRenderer() {}

    public static void addPulse(final List<BlockPos> pedals, final int rgb) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        final DustParticleOptions dust = new DustParticleOptions(new Vector3f(
            ((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f
        ), SPECK_SIZE);
        final RandomSource random = level.random;

        for (final BlockPos pedal : pedals) {
            final Vec3 top = PedalCableOverlay.cablePoint(pedal);
            for (int i = 0; i < SPECKS_PER_PEDAL; i++)
                level.addParticle(dust,
                    top.x + (random.nextDouble() - 0.5) * SPREAD,
                    top.y + random.nextDouble() * RISE,
                    top.z + (random.nextDouble() - 0.5) * SPREAD,
                    0, 0, 0);
        }
    }
}
