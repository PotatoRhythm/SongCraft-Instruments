package com.stump.songcraft_instruments.model;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.KotoBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class KotoModel extends GeoModel<KotoBlockEntity> {

    @Override
    public ResourceLocation getModelResource(KotoBlockEntity animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "geo/koto.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KotoBlockEntity animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "textures/block/koto.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KotoBlockEntity animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "animations/koto.animation.json");
    }

    @Override
    public void setCustomAnimations(KotoBlockEntity animatable, long instanceId, AnimationState<KotoBlockEntity> animationState) {
        KotoStringAnimator.animateStrings(this, animatable.getKotoStrings());
    }
}
