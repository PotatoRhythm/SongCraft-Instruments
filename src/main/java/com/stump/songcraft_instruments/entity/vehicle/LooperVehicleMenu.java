package com.stump.songcraft_instruments.entity.vehicle;

import com.stump.songcraft_instruments.menu.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The record slot of a looper carried by a boat or minecart, with buttons to play or pause it and toggle looping.
 */
public class LooperVehicleMenu extends AbstractContainerMenu {
    public static final int BUTTON_PLAY = 0, BUTTON_LOOPING = 1, BUTTON_VOLUME_DOWN = 2, BUTTON_VOLUME_UP = 3;

    public static final int RECORD_SLOT_X = 17, RECORD_SLOT_Y = 21;
    public static final int INVENTORY_Y = 70;

    private final Container container;
    private final ContainerData data;

    /**
     * Client side, filled by the server
     */
    public LooperVehicleMenu(final int containerId, final Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(VehicleLooper.DATA_COUNT));
    }
    public LooperVehicleMenu(final int containerId, final Inventory inventory, final Container container, final ContainerData data) {
        super(ModMenus.LOOPER_VEHICLE.get(), containerId);
        checkContainerSize(container, 1);
        checkContainerDataCount(data, VehicleLooper.DATA_COUNT);
        this.container = container;
        this.data = data;

        addSlot(new Slot(container, 0, RECORD_SLOT_X, RECORD_SLOT_Y) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return container.canPlaceItem(0, stack);
            }
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_Y + row * 18));
        }
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, 8 + column * 18, INVENTORY_Y + 58));

        addDataSlots(data);
    }

    public boolean isPlaying() {
        return data.get(VehicleLooper.DATA_PLAYING) != 0;
    }
    public boolean isLooping() {
        return data.get(VehicleLooper.DATA_LOOPING) != 0;
    }
    public int getStatus() {
        return data.get(VehicleLooper.DATA_STATUS);
    }
    public int getElapsedSeconds() {
        return data.get(VehicleLooper.DATA_ELAPSED);
    }
    public int getLengthSeconds() {
        return data.get(VehicleLooper.DATA_LENGTH);
    }
    public int getVolume() {
        return data.get(VehicleLooper.DATA_VOLUME);
    }

    @Override
    public boolean clickMenuButton(final Player player, final int id) {
        if (!(container instanceof VehicleLooper looper))
            return false;

        switch (id) {
            case BUTTON_PLAY -> looper.cyclePlaying();
            case BUTTON_LOOPING -> looper.cycleLooping();
            case BUTTON_VOLUME_DOWN -> looper.stepVolume(player, false);
            case BUTTON_VOLUME_UP -> looper.stepVolume(player, true);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = slots.get(index);
        if (!slot.hasItem())
            return ItemStack.EMPTY;

        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();

        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();

        return original;
    }

    @Override
    public boolean stillValid(final Player player) {
        return container.stillValid(player);
    }
}
