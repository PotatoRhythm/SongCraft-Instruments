package com.stump.songcraft_instruments.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Shows the one sound of an instrument that has no others to switch to, where the sound type button would be.
 * It is drawn as a flat inset panel rather than a button so that it reads as a label, and can't be clicked or focused.
 */
public class SoundLabel extends AbstractWidget {
    private static final int BACKGROUND = 0x90000000, BORDER = 0xFF3C3C3C, TEXT = 0xA0A0A0;

    public SoundLabel(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        active = false;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.fill(getX(), getY(), getX() + width, getY() + height, BACKGROUND);
        pGuiGraphics.renderOutline(getX(), getY(), width, height, BORDER);
        renderScrollingString(pGuiGraphics, Minecraft.getInstance().font, 2, TEXT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        pNarrationElementOutput.add(NarratedElementType.TITLE, getMessage());
    }
}
