package com.stump.songcraft_instruments.render;

import com.stump.songcraft_instruments.item.Gw2BassItem;
import com.stump.songcraft_instruments.model.Gw2BassItemModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class Gw2BassItemRenderer extends GeoItemRenderer<Gw2BassItem> {

    public Gw2BassItemRenderer() {
        super(new Gw2BassItemModel());
    }
}
