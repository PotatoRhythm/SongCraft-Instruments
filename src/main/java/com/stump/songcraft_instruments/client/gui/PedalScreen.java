package com.stump.songcraft_instruments.client.gui;

import com.stump.songcraft_instruments.menu.PedalMenu;
import com.stump.songcraft_instruments.pedal.PedalEffectType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * An effects pedal's knobs, each turned with its - and + buttons, and its footswitch
 */
@OnlyIn(Dist.CLIENT)
public class PedalScreen extends AbstractContainerScreen<PedalMenu> {
    // A plain vanilla panel, stretched to this screen's size, same as the looper vehicle's
    private static final ResourceLocation BACKGROUND = new ResourceLocation("textures/gui/demo_background.png");
    private static final int BACKGROUND_WIDTH = 248, BACKGROUND_HEIGHT = 166, BACKGROUND_BORDER = 4;

    private static final int TEXT_COLOR = 0x404040;
    private static final int ROW_Y = 22, ROW_HEIGHT = 18;
    private static final int LABEL_X = 10, BUTTON_SIZE = 14;
    private static final int DOWN_X = 80, VALUE_WIDTH = 24, UP_X = DOWN_X + BUTTON_SIZE + VALUE_WIDTH;
    private static final int TOGGLE_WIDTH = 100, TOGGLE_HEIGHT = 20;

    private Button toggleButton;

    public PedalScreen(final PedalMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = 140;
    }

    @Override
    protected void init() {
        final int knobs = menu.getEffectType().knobCount();
        imageHeight = ROW_Y + knobs * ROW_HEIGHT + TOGGLE_HEIGHT + 12;
        super.init();

        for (int i = 0; i < knobs; i++) {
            final int knob = i;
            final int y = topPos + ROW_Y + i * ROW_HEIGHT;
            addRenderableWidget(Button.builder(Component.literal("-"), (button) -> pressButton(PedalMenu.knobButton(knob, false)))
                .bounds(leftPos + DOWN_X, y, BUTTON_SIZE, BUTTON_SIZE)
                .build()
            );
            addRenderableWidget(Button.builder(Component.literal("+"), (button) -> pressButton(PedalMenu.knobButton(knob, true)))
                .bounds(leftPos + UP_X, y, BUTTON_SIZE, BUTTON_SIZE)
                .build()
            );
        }

        toggleButton = addRenderableWidget(Button.builder(Component.empty(), (button) -> pressButton(PedalMenu.BUTTON_TOGGLE))
            .bounds(leftPos + (imageWidth - TOGGLE_WIDTH) / 2, topPos + ROW_Y + knobs * ROW_HEIGHT + 4, TOGGLE_WIDTH, TOGGLE_HEIGHT)
            .build()
        );
        updateToggle();
    }

    private void pressButton(final int buttonId) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateToggle();
    }
    private void updateToggle() {
        toggleButton.setMessage(Component.translatable(menu.isBypassed()
            ? "gui.songcraft_instruments.pedal.bypassed"
            : "gui.songcraft_instruments.pedal.enabled"
        ));
    }

    @Override
    public void render(final GuiGraphics gui, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(final GuiGraphics gui, final float partialTick, final int mouseX, final int mouseY) {
        gui.blitNineSliced(BACKGROUND, leftPos, topPos, imageWidth, imageHeight,
            BACKGROUND_BORDER, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, 0, 0);
    }

    @Override
    protected void renderLabels(final GuiGraphics gui, final int mouseX, final int mouseY) {
        gui.drawString(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);

        final PedalEffectType type = menu.getEffectType();
        for (int i = 0; i < type.knobCount(); i++) {
            final int textY = ROW_Y + i * ROW_HEIGHT + (BUTTON_SIZE - font.lineHeight) / 2 + 1;
            gui.drawString(font, Component.translatable("gui.songcraft_instruments.pedal.knob." + type.knob(i).name()),
                LABEL_X, textY, TEXT_COLOR, false);

            // Shown from 0 to 10 like a real knob, though it turns in finer steps
            final String value = String.format("%.1f", menu.getKnob(i) * 10f / (PedalEffectType.KNOB_STEPS - 1));
            gui.drawString(font, value, DOWN_X + BUTTON_SIZE + (VALUE_WIDTH - font.width(value)) / 2, textY,
                TEXT_COLOR, false);
        }
    }
}
