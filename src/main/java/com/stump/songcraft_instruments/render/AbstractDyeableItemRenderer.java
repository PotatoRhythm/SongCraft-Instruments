package com.stump.songcraft_instruments.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public abstract class AbstractDyeableItemRenderer<T extends BlockItem & GeoItem>
        extends GeoItemRenderer<T> {

    public AbstractDyeableItemRenderer(GeoModel<T> model) {
        super(model);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone,
            RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
            float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        // Also tints Dyeable_ bones, for dyeable parts that animate separately
        if (bone.getName().startsWith("Dyeable")) {

            int color = 0xFFFFFF;

            if (getCurrentItemStack() != null) {
                CompoundTag tag = getCurrentItemStack().getTag();

                if (tag != null && tag.contains("DyeColor")) {
                    color = tag.getInt("DyeColor");
                }
            }

            red = ((color >> 16) & 255) / 255f;
            green = ((color >> 8) & 255) / 255f;
            blue = (color & 255) / 255f;
        }

        super.renderRecursively(poseStack, animatable, bone,
                renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}