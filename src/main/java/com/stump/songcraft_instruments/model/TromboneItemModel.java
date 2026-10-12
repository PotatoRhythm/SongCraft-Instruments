package com.stump.songcraft_instruments.model;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.trombone.TromboneSlides;
import com.stump.songcraft_instruments.item.TromboneItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class TromboneItemModel extends GeoModel<TromboneItem> {
    private static final String SLIDE_BONE = "Slide";
    /** How far out the slide goes in 7th position, in pixels */
    private static final float MAX_SLIDE_TRAVEL = 7;

    @Override
    public ResourceLocation getModelResource(TromboneItem animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "geo/trombone.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TromboneItem animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "textures/item/trombone.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TromboneItem animatable) {
        return new ResourceLocation(SCInstrumentMod.MODID, "animations/trombone.animation.json");
    }

    @Override
    public void setCustomAnimations(TromboneItem animatable, long instanceId, AnimationState<TromboneItem> animationState) {
        final CoreGeoBone slide = getAnimationProcessor().getBone(SLIDE_BONE);
        if (slide == null)
            return;

        final ItemDisplayContext context = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        final double extension = (context == null) ? 0 : TromboneSlides.extension(context);

        // The slide extends forward, away from the mouthpiece, which is towards negative Z
        slide.setPosZ(slide.getInitialSnapshot().getOffsetZ() - (float) extension * MAX_SLIDE_TRAVEL);
        slide.markPositionAsChanged();
    }
}
