package com.stump.songcraft_instruments.client.gui.options.partial;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.util.ClientUtil;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.awt.*;
import java.util.HashMap;
import java.util.Optional;

/**
 * The base class for all instrument options screens.
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractInstrumentOptionsScreen extends Screen {

    public final Optional<InstrumentScreen> instrumentScreen;
    public final Screen lastScreen;

    /**
     * True if this menu is an overlay of an {@link InstrumentScreen};
     * and one does exist
     */
    public final boolean isOverlay;
    
    
    public AbstractInstrumentOptionsScreen(Component pTitle, InstrumentScreen instrumentScreen, Screen lastScreen) {
        super(pTitle);
        this.instrumentScreen = Optional.ofNullable(instrumentScreen);
        this.lastScreen = lastScreen;

        this.isOverlay = instrumentScreen != null;
    }
    public AbstractInstrumentOptionsScreen(Component pTitle, Optional<InstrumentScreen> instrumentScreen, Screen lastScreen) {
        this(pTitle, instrumentScreen.orElse(null), lastScreen);
    }
    public AbstractInstrumentOptionsScreen(Component pTitle, InstrumentScreen instrumentScreen) {
        this(pTitle, instrumentScreen, null);
    }
    public AbstractInstrumentOptionsScreen(Component pTitle, Optional<InstrumentScreen> instrumentScreen) {
        this(pTitle, instrumentScreen, null);
    }
    public AbstractInstrumentOptionsScreen(Component pTitle, Screen prevScreen) {
        this(pTitle, Optional.empty(), prevScreen);
    }


    public int getSmallButtonWidth() {
        return 190;
    }
    public int getBigButtonWidth() {
        return (getSmallButtonWidth() + ClientUtil.GRID_HORZ_PADDING) * 2;
    }
    public int getButtonHeight() {
        return 20;
    }


    /*
     * The title, options grid and lower buttons are laid out as one block, centered vertically (slightly above center).
     * The gaps between them grow with the available space, and shrink to their minimums on small screens.
     */
    private static final int
        MIN_MARGIN = 10,
        MIN_TITLE_GAP = 8, MAX_TITLE_GAP = 20,
        MIN_BUTTONS_GAP = 12, MAX_BUTTONS_GAP = 40;
    /**
     * The fraction of the leftover space placed above the block; less than half places it slightly above center
     */
    private static final float TOP_SPACE_RATIO = .4f;

    /**
     * Fixed, unless the screen is laid out by {@link #arrangeVertically}
     */
    private int titleY = 15;

    /**
     * Centers the options grid horizontally, and positions it, the title and the lower buttons vertically
     * @param grid The options grid, already filled
     * @return The Y position of the lower buttons
     */
    protected int arrangeVertically(final Layout grid) {
        grid.arrangeElements();

        final int titleHeight = font.lineHeight;
        final int fixedHeight = titleHeight + grid.getHeight() + getButtonHeight();

        // Grow the gaps from their minimums towards their maximums as space allows
        final int minGaps = MIN_TITLE_GAP + MIN_BUTTONS_GAP, maxGaps = MAX_TITLE_GAP + MAX_BUTTONS_GAP;
        final int spareSpace = height - 2 * MIN_MARGIN - fixedHeight - minGaps;
        final float gapProgress = Mth.clamp((float) spareSpace / (maxGaps - minGaps), 0, 1);

        final int titleGap = (int) Mth.lerp(gapProgress, MIN_TITLE_GAP, MAX_TITLE_GAP),
            buttonsGap = (int) Mth.lerp(gapProgress, MIN_BUTTONS_GAP, MAX_BUTTONS_GAP);

        final int blockHeight = fixedHeight + titleGap + buttonsGap;
        titleY = Math.max(MIN_MARGIN, (int) ((height - blockHeight) * TOP_SPACE_RATIO));

        grid.setPosition((width - grid.getWidth()) / 2, titleY + titleHeight + titleGap);
        grid.arrangeElements();

        // Keep the buttons on-screen, even if they have to overlap the options
        return Math.min(
            grid.getY() + grid.getHeight() + buttonsGap,
            height - getButtonHeight() - MIN_MARGIN
        );
    }


    @Override
    public void render(GuiGraphics gui, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(gui);
        super.render(gui, pMouseX, pMouseY, pPartialTick);
        gui.drawCenteredString(font, title, width/2, titleY, Color.WHITE.getRGB());
    }


    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        // Pass keys to the instrument screen if they are consumed
        instrumentScreen.ifPresent((screen) -> {
           if (screen.isKeyConsumed(pKeyCode, pScanCode))
               screen.keyPressed(pKeyCode, pScanCode, pModifiers);
        });

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }
    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        instrumentScreen.ifPresent((screen) -> {
           if (screen.isKeyConsumed(pKeyCode, pScanCode))
               screen.keyReleased(pKeyCode, pScanCode, pModifiers);
        });

        return super.keyReleased(pKeyCode, pScanCode, pModifiers);
    }


    @Override
    public boolean isPauseScreen() {
        return !isOverlay;
    }

    @Override
    public void onClose() {
        saveOptions();

        if (isOverlay) {
            super.onClose();
            if (lastScreen != null)
                minecraft.pushGuiLayer(lastScreen);
        }
        else if (lastScreen != null)
            minecraft.setScreen(lastScreen);
        else
            super.onClose();
    }


    /* ---------------- Save System --------------- */

    protected final HashMap<String, Runnable> appliedOptions = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Queues the given option to later be saved.
     * Most notably, a save occurs when the client closes this screen.
     * @param optionKey A unique identifier of this option. If a duplicate entry
     * exists, it will be overwritten.
     * @param saveRunnable The runnable for saving the option
     */
    public void queueToSave(String optionKey, final Runnable saveRunnable) {
        final String modId = modId();
        if (modId != null)
            optionKey = modId + ":" + optionKey;

        if (appliedOptions.containsKey(optionKey))
            appliedOptions.replace(optionKey, saveRunnable);
        else
            appliedOptions.put(optionKey, saveRunnable);
    }

    public void saveOptions() {
        if (appliedOptions.isEmpty())
            return;

        appliedOptions.values().forEach(Runnable::run);
        ModClientConfigs.CONFIGS.save();
        
        LOGGER.info("Successfully saved "+appliedOptions.size()+" option(s) for "+title.getString());
    }


    /**
     * Fetches the Mod ID of the instrument being used
     * @apiNote Should be overwritten in the case of not being used by an instrument
     */
    public String modId() {
        return instrumentScreen.map(InstrumentScreen::getModId).orElse(null);
    }
    
}
