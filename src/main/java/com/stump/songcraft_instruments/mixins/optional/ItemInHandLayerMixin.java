package com.stump.songcraft_instruments.mixins.optional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stump.songcraft_instruments.client.gw2bass.Gw2BassPlaying;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets held instruments stay put while the arm holding them plays them.
 * <p>Only adds onto the hand's position, so it stacks with other mods' injections here.
 * Should another mod replace the method or the hand's positioning altogether, this is skipped rather than
 * failing, and instruments just follow the arm holding them.</p>
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

    @Inject(
        method = "renderArmWithItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/ArmedModel;translateToHand(Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            shift = At.Shift.AFTER
        ),
        require = 0
    )
    private void anchorHeldInstrument(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext displayContext,
                                      HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                      CallbackInfo ci) {
        Gw2BassPlaying.anchorHeldBass(((RenderLayer<?, ?>) (Object) this).getParentModel(), livingEntity, arm, poseStack);
    }
}
