package com.stump.songcraft_instruments.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.LooperBlock;
import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.looper.LooperVolumeControls;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

/**
 * Renders the looper's volume buttons and the indicator on its scale, which move apart from the rest of its model
 */
public class LooperRenderer implements BlockEntityRenderer<LooperBlockEntity> {
    public static final ResourceLocation
        PLUS_BUTTON_MODEL = new ResourceLocation(SCInstrumentMod.MODID, "block/looper/plus_button"),
        MINUS_BUTTON_MODEL = new ResourceLocation(SCInstrumentMod.MODID, "block/looper/minus_button"),
        INDICATOR_MODEL = new ResourceLocation(SCInstrumentMod.MODID, "block/looper/indicator");
    public static final List<ResourceLocation> MODELS = List.of(PLUS_BUTTON_MODEL, MINUS_BUTTON_MODEL, INDICATOR_MODEL);

    public LooperRenderer(final BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(final LooperBlockEntity looper, final float partialTick, final PoseStack poseStack,
                       final MultiBufferSource buffer, final int packedLight, final int packedOverlay) {
        final BlockState state = looper.getBlockState();
        if (!(state.getBlock() instanceof LooperBlock block))
            return;

        // The looper is a solid block, so it has no light of its own; take that of the side the controls are on
        final int light = (looper.getLevel() == null) ? packedLight
            : LevelRenderer.getLightColor(looper.getLevel(), looper.getBlockPos().relative(block.getControlsDir(state)));

        renderControls(state, looper.volumeControls(), poseStack, buffer, light, packedOverlay);
    }

    /**
     * Renders the volume buttons and indicator of a looper, as placed or carried by a vehicle
     */
    public static void renderControls(final BlockState state, final LooperVolumeControls controls,
                                      final PoseStack poseStack, final MultiBufferSource buffer,
                                      final int light, final int overlay) {
        final long now = Util.getMillis();
        renderControls(state, poseStack, buffer, light, overlay,
            controls.getPressDepth(true, now), controls.getPressDepth(false, now), controls.getIndicatorOffset(now)
        );
    }

    /**
     * @param plusDepth How far the plus button is pushed in, in pixels
     * @param minusDepth How far the minus button is pushed in, in pixels
     * @param indicatorOffset How far the indicator is moved along the scale from 0%, in pixels
     */
    private static void renderControls(final BlockState state, final PoseStack poseStack,
                                       final MultiBufferSource buffer, final int light, final int overlay,
                                       final float plusDepth, final float minusDepth, final float indicatorOffset) {
        final VertexConsumer consumer = buffer.getBuffer(Sheets.cutoutBlockSheet());

        poseStack.pushPose();

        // Rotate as the block state's model is: the model's front faces north
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-(state.getValue(LooperBlock.FACING).toYRot() + 180)));
        poseStack.translate(-0.5f, 0, -0.5f);

        // The buttons are pushed in towards the model's west, the indicator slides along its z axis
        renderPart(PLUS_BUTTON_MODEL, -plusDepth / 16, 0, state, poseStack, consumer, light, overlay);
        renderPart(MINUS_BUTTON_MODEL, -minusDepth / 16, 0, state, poseStack, consumer, light, overlay);
        renderPart(INDICATOR_MODEL, 0, indicatorOffset / 16, state, poseStack, consumer, light, overlay);

        poseStack.popPose();
    }

    private static void renderPart(final ResourceLocation modelLocation, final float x, final float z,
                                   final BlockState state, final PoseStack poseStack, final VertexConsumer consumer,
                                   final int light, final int overlay) {
        final BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLocation);

        poseStack.pushPose();
        poseStack.translate(x, 0, z);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
            poseStack.last(), consumer, state, model, 1, 1, 1, light, overlay, ModelData.EMPTY, null
        );
        poseStack.popPose();
    }
}
