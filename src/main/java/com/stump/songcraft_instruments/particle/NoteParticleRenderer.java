package com.stump.songcraft_instruments.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.stump.songcraft_instruments.SCInstrumentMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders note particles before water and other translucent blocks, rather than after them as particles are.
 * <p>Water writes depth, so particles drawn after it are hidden behind its surface: notes played underwater
 * could only be seen from underwater (except on Fabulous graphics). Drawn first, water is drawn over them
 * as over anything else underwater.</p>
 * <p>Note particles are ticked by the particle engine as usual, but skipped by its rendering
 * ({@link ParticleRenderType#NO_RENDER}).</p>
 */
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public final class NoteParticleRenderer {
    private static final List<CustomNoteParticle> PARTICLES = new ArrayList<>();

    private NoteParticleRenderer() {
    }

    static void add(CustomNoteParticle particle) {
        PARTICLES.add(particle);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)
            return;

        final Minecraft minecraft = Minecraft.getInstance();
        // Particles removed by the engine, or left behind when it was cleared (e.g. on changing dimension)
        PARTICLES.removeIf((particle) -> !particle.isAlive() || (particle.getLevel() != minecraft.level));
        if (PARTICLES.isEmpty())
            return;

        final Camera camera = event.getCamera();
        final float partialTick = event.getPartialTick();
        final LightTexture lightTexture = minecraft.gameRenderer.lightTexture();

        // As ParticleEngine#render does
        lightTexture.turnOnLightLayer();
        RenderSystem.enableDepthTest();
        final PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.mulPoseMatrix(event.getPoseStack().last().pose());
        RenderSystem.applyModelViewMatrix();

        RenderSystem.setShader(GameRenderer::getParticleShader);
        final Tesselator tesselator = Tesselator.getInstance();
        final BufferBuilder buffer = tesselator.getBuilder();
        ParticleRenderType.PARTICLE_SHEET_OPAQUE.begin(buffer, minecraft.getTextureManager());

        for (final CustomNoteParticle particle : PARTICLES) {
            if (particle.shouldCull() && !event.getFrustum().isVisible(particle.getBoundingBox()))
                continue;
            particle.render(buffer, camera, partialTick);
        }

        ParticleRenderType.PARTICLE_SHEET_OPAQUE.end(tesselator);

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        lightTexture.turnOffLightLayer();
    }
}
