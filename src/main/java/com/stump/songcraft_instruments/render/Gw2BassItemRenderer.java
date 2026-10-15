package com.stump.songcraft_instruments.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.stump.songcraft_instruments.client.gw2bass.Gw2BassPlaying;
import com.stump.songcraft_instruments.item.Gw2BassItem;
import com.stump.songcraft_instruments.model.Gw2BassItemModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class Gw2BassItemRenderer extends GeoItemRenderer<Gw2BassItem> {
    /** The strings' bones are named this followed by their number, from 1 for the lowest */
    private static final String STRING_BONE_PREFIX = "String ";
    /** Which way the strings swing across the fretboard, as a bone position per pixel */
    private static final float ACROSS_X = -0.612f, ACROSS_Z = -0.791f;

    public Gw2BassItemRenderer() {
        super(new Gw2BassItemModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Gw2BassItem animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Set right as each string is rendered, as every bass shares the same bones
        if (bone.getName().startsWith(STRING_BONE_PREFIX)) {
            final int string = Integer.parseInt(bone.getName().substring(STRING_BONE_PREFIX.length())) - 1;
            final float swing = Gw2BassPlaying.stringVibration(currentItemStack, renderPerspective, string);

            bone.setPosX(ACROSS_X * swing);
            bone.setPosZ(ACROSS_Z * swing);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
            packedLight, packedOverlay, red, green, blue, alpha);
    }
}
