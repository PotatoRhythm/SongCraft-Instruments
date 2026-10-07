package com.stump.songcraft_instruments.render;

import com.stump.songcraft_instruments.block.blockentity.DrumsetBlockEntity;
import com.stump.songcraft_instruments.model.DrumsetModel;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class DrumsetRenderer extends AbstractDyeableBlockRenderer<DrumsetBlockEntity> {

    public DrumsetRenderer(BlockEntityRendererProvider.Context context) {
        super(new DrumsetModel());
    }
}