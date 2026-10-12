package com.stump.songcraft_instruments.model;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.KeyboardStandBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class KeyboardStandModel extends GeoModel<KeyboardStandBlockEntity> {

    @Override
    public ResourceLocation getModelResource(KeyboardStandBlockEntity animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "geo/keyboard.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KeyboardStandBlockEntity animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "textures/block/keyboard.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KeyboardStandBlockEntity animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "animations/keyboard.animation.json");
    }

    @Override
    public void setCustomAnimations(KeyboardStandBlockEntity animatable, long instanceId, AnimationState<KeyboardStandBlockEntity> animationState) {
        KeyboardKeyAnimator.animateKeys(this, animatable.getKeyboardKeys());
    }
}
