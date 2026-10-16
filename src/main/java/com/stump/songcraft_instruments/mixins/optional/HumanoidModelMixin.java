package com.stump.songcraft_instruments.mixins.optional;

import com.stump.songcraft_instruments.client.GenericInstrumentSway;
import com.stump.songcraft_instruments.client.MicrophoneSway;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets players' heads move along with the instruments they're playing, once vanilla has posed them,
 * and lifts the shoulders of players raising their arms while singing.
 * <p>Only adds onto the pose, so it stacks with other mods' injections here.
 * Should another mod replace the method altogether, this is skipped rather than failing, and heads and shoulders just stay put.</p>
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

    @Inject(
        method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
        at = @At("TAIL"),
        require = 0
    )
    private void moveHeadWithInstrument(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                        float netHeadYaw, float headPitch, CallbackInfo ci) {
        GenericInstrumentSway.moveHead((HumanoidModel<?>) (Object) this, entity);
        MicrophoneSway.liftShoulders((HumanoidModel<?>) (Object) this, entity);
    }
}
