package com.stump.songcraft_instruments.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stump.songcraft_instruments.item.KotoBlockItem;
import com.stump.songcraft_instruments.model.KotoItemModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class KotoItemRenderer extends GeoItemRenderer<KotoBlockItem> {

    public KotoItemRenderer() {
        super(new KotoItemModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // The item shares the block's model, which sits on the block's floor. GeckoLib lifts item models
        // half a block above that, so lower it back to where the koto's display transforms expect it.
        poseStack.pushPose();
        poseStack.translate(0, -0.5, 0);
        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
