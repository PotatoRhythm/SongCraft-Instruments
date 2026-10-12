package com.stump.songcraft_instruments.model;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.MicrophoneStandBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class MicrophoneStandModel extends GeoModel<MicrophoneStandBlockEntity> {

    @Override
    public ResourceLocation getModelResource(MicrophoneStandBlockEntity animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "geo/microphone_stand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MicrophoneStandBlockEntity animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "textures/block/microphone_stand.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MicrophoneStandBlockEntity animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "animations/microphone_stand.animation.json");
    }

    @Override
    public void setCustomAnimations(MicrophoneStandBlockEntity animatable, long instanceId, AnimationState<MicrophoneStandBlockEntity> animationState) {
        MicrophonePulseAnimator.animateMic(this, animatable.getMicPulse());
    }
}