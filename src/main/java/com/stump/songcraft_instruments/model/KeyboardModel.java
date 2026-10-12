package com.stump.songcraft_instruments.model;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.KeyboardBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class KeyboardModel extends GeoModel<KeyboardBlockEntity> {

    @Override
    public ResourceLocation getModelResource(KeyboardBlockEntity animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "geo/keyboard.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KeyboardBlockEntity animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "textures/block/keyboard.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KeyboardBlockEntity animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "animations/keyboard.animation.json");
    }

    @Override
    public void setCustomAnimations(KeyboardBlockEntity animatable, long instanceId, AnimationState<KeyboardBlockEntity> animationState) {
        KeyboardKeyAnimator.animateKeys(this, animatable.getKeyboardKeys());
    }
}
