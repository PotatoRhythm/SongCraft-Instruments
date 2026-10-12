package com.stump.songcraft_instruments.render;

import com.stump.songcraft_instruments.item.TromboneItem;
import com.stump.songcraft_instruments.model.TromboneItemModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TromboneItemRenderer extends GeoItemRenderer<TromboneItem> {

    public TromboneItemRenderer() {
        super(new TromboneItemModel());
    }
}
