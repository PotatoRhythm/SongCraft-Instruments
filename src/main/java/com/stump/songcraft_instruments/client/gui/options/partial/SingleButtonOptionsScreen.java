package com.stump.songcraft_instruments.client.gui.options.partial;

import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.options.GridInstrumentOptionsScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.*;

/**
 * A subclass of {@link GridInstrumentOptionsScreen} that implements a simple, singular button.
 */
@OnlyIn(Dist.CLIENT)
public abstract class SingleButtonOptionsScreen extends GridInstrumentOptionsScreen {
    private final static int SPACER_HEIGHT = 5;

    public SingleButtonOptionsScreen(final GridInstrumentScreen screen) {
        super(screen);
    }
    public SingleButtonOptionsScreen(final Screen lastScreen) {
        super(lastScreen);
    }

    protected abstract String optionsLabelKey();
    protected abstract AbstractButton constructButton();


    @Override
    protected void initControlSection(GridLayout grid, GridLayout.RowHelper rowHelper) {
        super.initControlSection(grid, rowHelper);
        rowHelper.addChild(SpacerElement.height(SPACER_HEIGHT), 2);
        rowHelper.addChild(constructButton(), 2);
    }

    @Override
    public void render(GuiGraphics gui, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(gui, pMouseX, pMouseY, pPartialTick);
    }

}