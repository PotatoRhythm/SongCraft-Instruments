package com.stump.songcraft_instruments.render;

import com.stump.songcraft_instruments.block.blockentity.KotoBlockEntity;
import com.stump.songcraft_instruments.model.KotoModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class KotoRenderer extends GeoBlockRenderer<KotoBlockEntity> {

    public KotoRenderer(BlockEntityRendererProvider.Context context) {
        super(new KotoModel());
    }
}
