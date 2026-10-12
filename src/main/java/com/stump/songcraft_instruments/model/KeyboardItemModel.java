package com.stump.songcraft_instruments.model;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.item.KeyboardBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class KeyboardItemModel extends GeoModel<KeyboardBlockItem> {

    @Override
    public ResourceLocation getModelResource(KeyboardBlockItem animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "geo/keyboard.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KeyboardBlockItem animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "textures/block/keyboard.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KeyboardBlockItem animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "animations/keyboard.animation.json");
    }

    @Override
    public void setCustomAnimations(KeyboardBlockItem animatable, long instanceId, AnimationState<KeyboardBlockItem> animationState) {
        // The item shares the keyboard's bones, so keep its keys from showing a keyboard's pressed keys
        KeyboardKeyAnimator.animateKeys(this, null);
    }
}
