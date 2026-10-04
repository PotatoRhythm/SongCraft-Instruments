package com.stump.songcraft_instruments.client.gui.instrument;

import java.awt.Color;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.WarningScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A disclaimer shown the first time any instrument is opened, until the player acknowledges it.
 * @implNote
 * This screen was heavily inspired by <a href=https://ko-fi.com/s/665c3cc518>Kistu-Lyre+</a>'s Disclaimer screen.
 * Please consider supporting them on Ko-fi!
 */
@OnlyIn(Dist.CLIENT)
public class DisclaimerScreen extends WarningScreen {

    private static final Component TITLE = Component.translatable(
        "songcraft_instruments.disclaimer.title"
    ).withStyle(ChatFormatting.BOLD);
    private static final Component CONTENT = Component.translatable("songcraft_instruments.disclaimer.content");

    /*
     * The title, text and button are laid out as one block, centered vertically (slightly above center).
     * The gaps between them grow with the available space, and shrink to their minimums on small screens.
     */
    private static final int
        MIN_MARGIN = 10,
        MIN_TITLE_GAP = 12, MAX_TITLE_GAP = 30,
        MIN_BUTTON_GAP = 8, MAX_BUTTON_GAP = 24,
        // Keeps lines at a comfortable reading length on wide screens
        MAX_TEXT_WIDTH = 360;
    /**
     * The fraction of the leftover space placed above the block; less than half places it slightly above center
     */
    private static final float TOP_SPACE_RATIO = .4f;

    private final Screen previousScreen;
    private MultiLineLabel contentLabel = MultiLineLabel.EMPTY;
    private int titleY, contentY;

    public DisclaimerScreen(final Screen previousScreen) {
        super(TITLE, CONTENT, null, TITLE.copy().append("\n").append(CONTENT));
        this.previousScreen = previousScreen;
    }


    @Override
    protected int getLineHeight() {
        return 10;
    }

    @Override
    protected void init() {
        contentLabel = MultiLineLabel.create(font, CONTENT, Math.min(width - 100, MAX_TEXT_WIDTH));

        final Button acknowledgeButton = Button.builder(CommonComponents.GUI_ACKNOWLEDGE, (button) -> {
            ModClientConfigs.ACCEPTED_DISCLAIMER.set(true);
            minecraft.setScreen(previousScreen);
        }).build();

        final int contentHeight = contentLabel.getLineCount() * getLineHeight();
        final int fixedHeight = font.lineHeight + contentHeight + acknowledgeButton.getHeight();

        // Grow the gaps from their minimums towards their maximums as space allows
        final int minGaps = MIN_TITLE_GAP + MIN_BUTTON_GAP, maxGaps = MAX_TITLE_GAP + MAX_BUTTON_GAP;
        final int spareSpace = height - 2 * MIN_MARGIN - fixedHeight - minGaps;
        final float gapProgress = Mth.clamp((float) spareSpace / (maxGaps - minGaps), 0, 1);

        final int titleGap = (int) Mth.lerp(gapProgress, MIN_TITLE_GAP, MAX_TITLE_GAP),
            buttonGap = (int) Mth.lerp(gapProgress, MIN_BUTTON_GAP, MAX_BUTTON_GAP);

        final int blockHeight = fixedHeight + titleGap + buttonGap;
        titleY = Math.max(MIN_MARGIN, (int) ((height - blockHeight) * TOP_SPACE_RATIO));
        contentY = titleY + font.lineHeight + titleGap;

        // Keep the button on-screen, even if it has to overlap the text
        final int buttonY = Math.min(
            contentY + contentHeight + buttonGap,
            height - acknowledgeButton.getHeight() - MIN_MARGIN
        );
        acknowledgeButton.setPosition((width - acknowledgeButton.getWidth()) / 2, buttonY);

        this.addRenderableWidget(acknowledgeButton);

        super.init();
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        // Replaces vanilla's rendering, which places the text at a fixed height
        renderBackground(gui);
        renderTitle(gui);
        contentLabel.renderLeftAligned(gui,
            (width - contentLabel.getWidth()) / 2, contentY, getLineHeight(), Color.WHITE.getRGB()
        );

        for (final Renderable renderable : renderables)
            renderable.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderTitle(GuiGraphics gui) {
        gui.drawCenteredString(font, title, width/2, titleY, Color.WHITE.getRGB());
    }


    @Override
    public void onClose() {
        super.onClose();
        previousScreen.onClose();
    }


    @Override
    protected void initButtons(int idc) {}
}
