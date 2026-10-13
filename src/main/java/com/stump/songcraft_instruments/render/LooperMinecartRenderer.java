package com.stump.songcraft_instruments.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stump.songcraft_instruments.block.LooperBlock;
import com.stump.songcraft_instruments.entity.vehicle.LooperMinecart;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders a minecart with the looper it carries, along with the looper's volume controls
 */
public class LooperMinecartRenderer extends MinecartRenderer<LooperMinecart> {
    public LooperMinecartRenderer(final EntityRendererProvider.Context context) {
        super(context, ModelLayers.MINECART);
    }

    @Override
    protected void renderMinecartContents(final LooperMinecart minecart, final float partialTicks, final BlockState state,
                                          final PoseStack poseStack, final MultiBufferSource buffer, final int packedLight) {
        super.renderMinecartContents(minecart, partialTicks, state, poseStack, buffer, packedLight);
        if (state.getBlock() instanceof LooperBlock)
            LooperRenderer.renderControls(state, minecart.looper().volumeControls(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
    }
}
