package com.stump.songcraft_instruments.menu;

import com.stump.songcraft_instruments.block.PedalBlock;
import com.stump.songcraft_instruments.block.blockentity.PedalBlockEntity;
import com.stump.songcraft_instruments.pedal.PedalEffectType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * An effects pedal's knobs and footswitch. Has no slots: the knobs are synced through its data,
 * and turned through its buttons, so the server checks every change.
 */
public class PedalMenu extends AbstractContainerMenu {
    /**
     * Turning knob {@code i} down is button {@code 2i}, up is {@code 2i + 1}
     */
    public static final int BUTTON_TOGGLE = PedalEffectType.MAX_KNOBS * 2;
    private static final double MAX_USE_DISTANCE_SQR = 8 * 8;

    /**
     * Server side only
     */
    @Nullable
    private final PedalBlockEntity pedal;
    /**
     * Known as soon as the menu opens, unlike the synced data, so the screen can lay out its knobs
     */
    private final PedalEffectType effectType;
    private final ContainerData data;

    /**
     * Client side, filled by the server. The pedal's position is sent along when the menu opens.
     */
    public PedalMenu(final int containerId, final Inventory inventory, final FriendlyByteBuf extraData) {
        this(containerId, null, typeAt(inventory.player.level(), extraData.readBlockPos()),
            new SimpleContainerData(PedalBlockEntity.DATA_COUNT));
    }
    public PedalMenu(final int containerId, final PedalBlockEntity pedal, final ContainerData data) {
        this(containerId, pedal, pedal.getEffect().type(), data);
    }
    private PedalMenu(final int containerId, @Nullable final PedalBlockEntity pedal, final PedalEffectType effectType,
                      final ContainerData data) {
        super(ModMenus.PEDAL.get(), containerId);
        checkContainerDataCount(data, PedalBlockEntity.DATA_COUNT);
        this.pedal = pedal;
        this.effectType = effectType;
        this.data = data;
        addDataSlots(data);
    }

    private static PedalEffectType typeAt(final Level level, final BlockPos pos) {
        return (level.getBlockState(pos).getBlock() instanceof PedalBlock block) ? block.effectType : PedalEffectType.FUZZ;
    }

    public PedalEffectType getEffectType() {
        return effectType;
    }
    public int getKnob(final int index) {
        return data.get(index);
    }
    public boolean isBypassed() {
        return data.get(PedalBlockEntity.DATA_BYPASSED) != 0;
    }

    public static int knobButton(final int knob, final boolean up) {
        return knob * 2 + (up ? 1 : 0);
    }

    @Override
    public boolean clickMenuButton(final Player player, final int id) {
        if (pedal == null)
            return false;

        if (id == BUTTON_TOGGLE) {
            pedal.toggleBypassed();
            return true;
        }

        final int knob = id / 2;
        if (id < 0 || knob >= pedal.getEffect().type().knobCount())
            return false;

        pedal.stepKnob(knob, (id % 2 == 1) ? 1 : -1);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        return (pedal != null) && !pedal.isRemoved()
            && player.distanceToSqr(pedal.getBlockPos().getCenter()) <= MAX_USE_DISTANCE_SQR;
    }
}
