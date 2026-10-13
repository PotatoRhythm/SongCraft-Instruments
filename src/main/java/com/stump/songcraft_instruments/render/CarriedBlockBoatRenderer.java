package com.stump.songcraft_instruments.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.stump.songcraft_instruments.entity.vehicle.CarriedBlockVehicle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import org.joml.Quaternionf;

/**
 * Renders a boat with the looper or speaker it carries, where a chest boat's chest would be
 */
public class CarriedBlockBoatRenderer extends BoatRenderer {
    private final BlockRenderDispatcher blockRenderer;

    public CarriedBlockBoatRenderer(final EntityRendererProvider.Context context) {
        super(context, false);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(final Boat boat, final float entityYaw, final float partialTicks, final PoseStack poseStack,
                       final MultiBufferSource buffer, final int packedLight) {
        super.render(boat, entityYaw, partialTicks, poseStack, buffer, packedLight);
        if (!(boat instanceof CarriedBlockVehicle vehicle))
            return;

        poseStack.pushPose();

        // Same as the boat's own model, so the block moves and wobbles along with it
        poseStack.translate(0.0F, CarriedBlockVehicle.BOAT_MODEL_Y, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

        final float hurtTime = boat.getHurtTime() - partialTicks;
        final float damage = Math.max(boat.getDamage() - partialTicks, 0.0F);
        if (hurtTime > 0.0F)
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurtTime) * hurtTime * damage / 10.0F * boat.getHurtDir()));

        final float bubbleAngle = boat.getBubbleAngle(partialTicks);
        if (!Mth.equal(bubbleAngle, 0.0F))
            poseStack.mulPose(new Quaternionf().setAngleAxis(bubbleAngle * Mth.DEG_TO_RAD, 1.0F, 0.0F, 1.0F));

        // Where a chest boat's chest is; note particles are placed to match, see CarriedBlockVehicle
        final float bottom = (boat.getVariant() == Boat.Type.BAMBOO)
            ? CarriedBlockVehicle.RAFT_BLOCK_BOTTOM : CarriedBlockVehicle.BOAT_BLOCK_BOTTOM;
        poseStack.translate(0.0F, bottom, CarriedBlockVehicle.BOAT_BLOCK_OFFSET);
        poseStack.scale(CarriedBlockVehicle.BLOCK_SCALE, CarriedBlockVehicle.BLOCK_SCALE, CarriedBlockVehicle.BLOCK_SCALE);
        // The block's front faces north, towards the passenger
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        blockRenderer.renderSingleBlock(vehicle.getCarriedBlockState(), poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }
}
