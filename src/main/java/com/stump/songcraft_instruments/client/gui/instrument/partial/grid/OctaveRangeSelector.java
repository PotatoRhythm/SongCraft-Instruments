package com.stump.songcraft_instruments.client.gui.instrument.partial.grid;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.joml.Vector2ic;

/**
 * A row of octave cells shown above the control bar in Octave Swap mode.
 * The cells within the player's octave range are lit, with the current octave highlighted.
 * The edges of the lit block are handles, dragged to change the range.
 * <p>
 * This is intentionally not a widget, so that it never takes keyboard focus away from the notes,
 * and survives the screen's widgets being cleared when its grid is rebuilt.
 */
@OnlyIn(Dist.CLIENT)
public class OctaveRangeSelector {
    public static final int LOWEST_OCTAVE = -2, HIGHEST_OCTAVE = 2;
    private static final int CELL_COUNT = HIGHEST_OCTAVE - LOWEST_OCTAVE + 1;

    public static final int CELL_WIDTH = 20, HEIGHT = 12;
    public static final int WIDTH = CELL_WIDTH * CELL_COUNT;
    /**
     * How far (in pixels) from a handle a press still grabs it
     */
    private static final int HANDLE_REACH = 4;

    private static final int
        BORDER_COLOR = 0xFF000000,
        UNAVAILABLE_FILL = 0x90101010, UNAVAILABLE_TEXT = 0xFF3C3C3C,
        OUTSIDE_FILL = 0xB0000000, OUTSIDE_TEXT = 0xFF8A8A8A,
        INSIDE_FILL = 0xE0505050, INSIDE_TEXT = 0xFFFFFFFF,
        CURRENT_FILL = 0xFF7BBF7B, CURRENT_TEXT = 0xFF173A17,
        RANGE_OUTLINE = 0xFFD0D0D0,
        HANDLE_COLOR = 0xFFFFFFFF, HANDLE_HOVER_COLOR = 0xFFC1FFC8;

    private enum Handle { MIN, MAX }

    private final GridInstrumentScreen screen;
    private int x, y;
    private Handle dragging = null;

    public OctaveRangeSelector(final GridInstrumentScreen screen) {
        this.screen = screen;
    }

    public void setPosition(final int x, final int y) {
        this.x = x;
        this.y = y;
    }


    //#region Rendering

    public void render(final GuiGraphics gui, final Font font, final int mouseX, final int mouseY) {
        final int min = screen.getMinOctave(), max = screen.getMaxOctave(), current = screen.getCurrentOctave();

        for (int octave = LOWEST_OCTAVE; octave <= HIGHEST_OCTAVE; octave++) {
            final int cellX = cellX(octave);

            final int fill, textColor;
            if (!isAvailable(octave)) {
                fill = UNAVAILABLE_FILL;
                textColor = UNAVAILABLE_TEXT;
            } else if (octave == current) {
                fill = CURRENT_FILL;
                textColor = CURRENT_TEXT;
            } else if (octave >= min && octave <= max) {
                fill = INSIDE_FILL;
                textColor = INSIDE_TEXT;
            } else {
                fill = OUTSIDE_FILL;
                textColor = OUTSIDE_TEXT;
            }

            gui.fill(cellX, y, cellX + CELL_WIDTH, y + HEIGHT, fill);

            final String label = octaveLabel(octave);
            gui.drawString(font, label,
                cellX + (CELL_WIDTH - font.width(label)) / 2 + 1, y + (HEIGHT - font.lineHeight) / 2 + 1,
                textColor, false
            );
        }

        // Cell dividers and border
        for (int i = 1; i < CELL_COUNT; i++)
            gui.fill(x + i * CELL_WIDTH, y, x + i * CELL_WIDTH + 1, y + HEIGHT, BORDER_COLOR);
        gui.renderOutline(x - 1, y - 1, WIDTH + 2, HEIGHT + 2, BORDER_COLOR);

        // Outline the range, with its edges as handles
        final int rangeLeft = cellX(min), rangeRight = cellX(max) + CELL_WIDTH;
        gui.fill(rangeLeft, y - 1, rangeRight, y, RANGE_OUTLINE);
        gui.fill(rangeLeft, y + HEIGHT, rangeRight, y + HEIGHT + 1, RANGE_OUTLINE);

        final Handle hovered = (dragging != null) ? dragging : getHandleAt(mouseX, mouseY);
        renderHandle(gui, rangeLeft, hovered == Handle.MIN);
        renderHandle(gui, rangeRight, hovered == Handle.MAX);

        if (isMouseOver(mouseX, mouseY) || dragging != null)
            screen.setTooltipForNextRenderPass(font.split(tooltip(min, max), 200), this::positionTooltip, true);
    }

    /**
     * Places the tooltip below the selector, so it doesn't cover the cells being dragged
     */
    private Vector2ic positionTooltip(final int screenWidth, final int screenHeight,
                                      final int mouseX, final int mouseY,
                                      final int tooltipWidth, final int tooltipHeight) {
        // Leave room for the tooltip's own border
        final int tooltipX = Mth.clamp(mouseX + 12, 4, screenWidth - tooltipWidth - 4);
        final int tooltipY = Mth.clamp(y + HEIGHT + 2 + 6, 4, screenHeight - tooltipHeight - 4);
        return new Vector2i(tooltipX, tooltipY);
    }

    private void renderHandle(final GuiGraphics gui, final int edgeX, final boolean hovered) {
        gui.fill(edgeX - 1, y - 2, edgeX + 1, y + HEIGHT + 2, hovered ? HANDLE_HOVER_COLOR : HANDLE_COLOR);
    }

    private static Component tooltip(final int min, final int max) {
        return Component.translatable("button.songcraft_instruments.octave_range.tooltip",
            octaveLabel(min), octaveLabel(max)
        );
    }

    private static String octaveLabel(final int octave) {
        return (octave > 0) ? ("+" + octave) : String.valueOf(octave);
    }

    //#endregion


    //#region Input

    public boolean isMouseOver(final double mouseX, final double mouseY) {
        return mouseX >= x - HANDLE_REACH && mouseX < x + WIDTH + HANDLE_REACH
            && mouseY >= y - 2 && mouseY < y + HEIGHT + 2;
    }

    /**
     * Grabs the handle under the mouse. Pressing a cell outside the range stretches the nearest edge to it.
     * @return Whether the press was consumed
     */
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button != 0 || !isMouseOver(mouseX, mouseY))
            return false;

        dragging = getHandleAt(mouseX, mouseY);

        if (dragging == null) {
            final int octave = octaveAt(mouseX);
            if (octave < screen.getMinOctave())
                dragging = Handle.MIN;
            else if (octave > screen.getMaxOctave())
                dragging = Handle.MAX;
        }

        if (dragging != null) {
            playClickSound();
            dragTo(mouseX);
        }
        return true;
    }

    public boolean mouseDragged(final double mouseX) {
        if (dragging == null)
            return false;

        dragTo(mouseX);
        return true;
    }

    public boolean mouseReleased() {
        if (dragging == null)
            return false;

        dragging = null;
        ModClientConfigs.OCTAVE_SWAP_MIN.set(screen.getMinOctave());
        ModClientConfigs.OCTAVE_SWAP_MAX.set(screen.getMaxOctave());
        return true;
    }

    private void dragTo(final double mouseX) {
        int min = screen.getMinOctave(), max = screen.getMaxOctave();

        // Snap to the nearest boundary between cells
        final int boundary = Mth.clamp((int) Math.round((mouseX - x) / CELL_WIDTH), 0, CELL_COUNT);

        if (dragging == Handle.MIN)
            min = Mth.clamp(boundary + LOWEST_OCTAVE, screen.getLowestAvailableOctave(), max);
        else
            max = Mth.clamp(boundary + LOWEST_OCTAVE - 1, min, screen.getHighestAvailableOctave());

        screen.setOctaveRange(min, max);
    }

    private Handle getHandleAt(final double mouseX, final double mouseY) {
        if (!isMouseOver(mouseX, mouseY))
            return null;

        final int minDist = (int) Math.abs(mouseX - cellX(screen.getMinOctave())),
            maxDist = (int) Math.abs(mouseX - (cellX(screen.getMaxOctave()) + CELL_WIDTH));

        if (Math.min(minDist, maxDist) > HANDLE_REACH)
            return null;
        return (minDist <= maxDist) ? Handle.MIN : Handle.MAX;
    }

    private int octaveAt(final double mouseX) {
        return Mth.clamp((int) Math.floor((mouseX - x) / CELL_WIDTH), 0, CELL_COUNT - 1) + LOWEST_OCTAVE;
    }

    //#endregion


    private int cellX(final int octave) {
        return x + (octave - LOWEST_OCTAVE) * CELL_WIDTH;
    }

    private boolean isAvailable(final int octave) {
        return octave >= screen.getLowestAvailableOctave() && octave <= screen.getHighestAvailableOctave();
    }

    private static void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
    }
}
