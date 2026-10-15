package com.stump.songcraft_instruments.model;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.item.Gw2BassItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Gw2BassItemModel extends GeoModel<Gw2BassItem> {
    @Override
    public ResourceLocation getModelResource(Gw2BassItem animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "geo/gw2_bass.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Gw2BassItem animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "textures/item/gw2_bass.png");
    }

    @Override
    public ResourceLocation getAnimationResource(Gw2BassItem animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "animations/gw2_bass.animation.json");
    }
}
