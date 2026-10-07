package com.stump.songcraft_instruments.render;

import com.stump.songcraft_instruments.item.DrumsetBlockItem;
import com.stump.songcraft_instruments.model.DrumsetItemModel;

public class DrumsetItemRenderer
        extends AbstractDyeableItemRenderer<DrumsetBlockItem> {

    public DrumsetItemRenderer() {
        super(new DrumsetItemModel());
    }
}