package com.stump.songcraft_instruments.client.gui;

import com.stump.songcraft_instruments.entity.vehicle.LooperVehicleMenu;
import com.stump.songcraft_instruments.entity.vehicle.VehicleLooper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The record slot of a looper carried by a boat or minecart, with buttons to play or pause it and toggle looping
 */
@OnlyIn(Dist.CLIENT)
public class LooperVehicleScreen extends AbstractContainerScreen<LooperVehicleMenu> {
    // A plain vanilla panel, stretched to this screen's size
    private static final ResourceLocation BACKGROUND = new ResourceLocation("textures/gui/demo_background.png");
    private static final int BACKGROUND_WIDTH = 248, BACKGROUND_HEIGHT = 166, BACKGROUND_BORDER = 4;

    private static final int SLOT_COLOR = 0xFF8B8B8B, SLOT_SHADOW = 0xFF373737, SLOT_HIGHLIGHT = 0xFFFFFFFF;
    private static final int STATUS_COLOR = 0x404040;

    private static final int BUTTON_Y = 19, BUTTON_WIDTH = 62, BUTTON_HEIGHT = 20;
    private static final int PLAY_BUTTON_X = 40, LOOPING_BUTTON_X = 106;
    private static final int STATUS_Y = 45;

    private Button playButton, loopingButton;

    public LooperVehicleScreen(final LooperVehicleMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 152;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        playButton = addRenderableWidget(Button.builder(Component.empty(), (button) -> pressButton(LooperVehicleMenu.BUTTON_PLAY))
            .bounds(leftPos + PLAY_BUTTON_X, topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build()
        );
        loopingButton = addRenderableWidget(Button.builder(Component.empty(), (button) -> pressButton(LooperVehicleMenu.BUTTON_LOOPING))
            .bounds(leftPos + LOOPING_BUTTON_X, topPos + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build()
        );
        updateButtons();
    }

    private void pressButton(final int buttonId) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }
    private void updateButtons() {
        playButton.setMessage(Component.translatable(menu.isPlaying()
            ? "gui.songcraft_instruments.looper_vehicle.pause"
            : "gui.songcraft_instruments.looper_vehicle.play"
        ));
        playButton.active = menu.getStatus() == VehicleLooper.STATUS_READY;

        loopingButton.setMessage(Component.translatable(menu.isLooping()
            ? "gui.songcraft_instruments.looper_vehicle.looping_on"
            : "gui.songcraft_instruments.looper_vehicle.looping_off"
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

        // Slot outlines sit 1 pixel around the slot's item
        menu.slots.forEach((slot) -> renderSlotBackground(gui, leftPos + slot.x - 1, topPos + slot.y - 1));
    }
    private static void renderSlotBackground(final GuiGraphics gui, final int x, final int y) {
        gui.fill(x, y, x + 18, y + 18, SLOT_COLOR);
        gui.fill(x, y, x + 17, y + 1, SLOT_SHADOW);
        gui.fill(x, y, x + 1, y + 17, SLOT_SHADOW);
        gui.fill(x + 1, y + 17, x + 18, y + 18, SLOT_HIGHLIGHT);
        gui.fill(x + 17, y + 1, x + 18, y + 18, SLOT_HIGHLIGHT);
    }

    @Override
    protected void renderLabels(final GuiGraphics gui, final int mouseX, final int mouseY) {
        super.renderLabels(gui, mouseX, mouseY);
        gui.drawString(font, getStatusText(), titleLabelX, STATUS_Y, STATUS_COLOR, false);
    }

    private Component getStatusText() {
        return switch (menu.getStatus()) {
            case VehicleLooper.STATUS_NO_RECORD -> Component.translatable("gui.songcraft_instruments.looper_vehicle.no_record");
            case VehicleLooper.STATUS_EMPTY -> Component.translatable("gui.songcraft_instruments.looper_vehicle.empty");
            case VehicleLooper.STATUS_MISSING -> Component.translatable("gui.songcraft_instruments.looper_vehicle.missing");
            default -> Component.translatable("gui.songcraft_instruments.looper_vehicle.time",
                formatTime(menu.getElapsedSeconds()), formatTime(menu.getLengthSeconds())
            );
        };
    }
    private static String formatTime(final int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
