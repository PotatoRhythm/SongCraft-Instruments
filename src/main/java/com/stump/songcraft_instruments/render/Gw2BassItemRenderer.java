package com.stump.songcraft_instruments.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.stump.songcraft_instruments.client.gw2bass.Gw2BassPlaying;
import com.stump.songcraft_instruments.item.Gw2BassItem;
import com.stump.songcraft_instruments.model.Gw2BassItemModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class Gw2BassItemRenderer extends GeoItemRenderer<Gw2BassItem> {
    /** The strings' bones are named this followed by their number, from 1 for the lowest */
    private static final String STRING_BONE_PREFIX = "String ";
    /** Which way the strings swing across the fretboard, as a bone position per pixel */
    private static final float ACROSS_X = -0.612f, ACROSS_Z = -0.791f;
    /** The centre of a texel in the middle of a string's texture, in the strings' usual silver */
    private static final float STRING_TEXEL_U = 211.5f / 256, STRING_TEXEL_V = 90.5f / 256;

    /** Whether the bone being rendered is a string */
    private boolean renderingString;

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

        renderingString = bone.getName().startsWith(STRING_BONE_PREFIX);
        try {
            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
        } finally {
            renderingString = false;
        }
    }

    @Override
    public void renderCube(PoseStack poseStack, GeoCube cube, VertexConsumer buffer, int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {
        if (!renderingString) {
            super.renderCube(poseStack, cube, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        // The strings are thinner than a pixel on screen, so which of their faces covers each pixel flickers along
        // their length. Every face is lit as the top is, so that the strings are evenly shaded rather than dashed.
        RenderUtils.translateToPivotPoint(poseStack, cube);
        RenderUtils.rotateMatrixAroundCube(poseStack, cube);
        RenderUtils.translateAwayFromPivotPoint(poseStack, cube);

        final Matrix3f normalisedPoseState = poseStack.last().normal();
        final Matrix4f poseState = poseStack.last().pose();
        final Vector3f normal = normalisedPoseState.transform(new Vector3f(Direction.UP.step()));

        // Every face is also coloured from the same texel, so that the strings are one flat colour rather than speckled
        for (GeoQuad quad : cube.quads()) {
            if (quad == null)
                continue;

            for (GeoVertex vertex : quad.vertices()) {
                final Vector3f position = vertex.position();
                final Vector4f transformed = poseState.transform(new Vector4f(position.x(), position.y(), position.z(), 1));

                buffer.vertex(transformed.x(), transformed.y(), transformed.z(), red, green, blue, alpha,
                    STRING_TEXEL_U, STRING_TEXEL_V, packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
            }
        }
    }
}
