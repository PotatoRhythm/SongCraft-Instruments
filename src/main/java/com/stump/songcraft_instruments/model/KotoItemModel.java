package com.stump.songcraft_instruments.model;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.item.KotoBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class KotoItemModel extends GeoModel<KotoBlockItem> {

    @Override
    public ResourceLocation getModelResource(KotoBlockItem animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "geo/koto.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KotoBlockItem animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "textures/block/koto.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KotoBlockItem animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "animations/koto.animation.json");
    }
}
