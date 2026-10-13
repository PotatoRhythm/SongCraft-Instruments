package com.stump.songcraft_instruments.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Places one of this mod's minecarts on a rail, the same way a vanilla minecart item does
 */
public class ModMinecartItem extends Item {
    @FunctionalInterface
    public interface MinecartFactory {
        AbstractMinecart create(Level level, double x, double y, double z);
    }

    private final MinecartFactory factory;

    public ModMinecartItem(final MinecartFactory factory, final Properties properties) {
        super(properties);
        this.factory = factory;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.RAILS))
            return InteractionResult.FAIL;

        final ItemStack stack = context.getItemInHand();
        if (!level.isClientSide) {
            final RailShape shape = (state.getBlock() instanceof BaseRailBlock rail)
                ? rail.getRailDirection(state, level, pos, null)
                : RailShape.NORTH_SOUTH;
            final double ascendingOffset = shape.isAscending() ? 0.5D : 0.0D;

            final AbstractMinecart minecart = factory.create(level,
                pos.getX() + 0.5D, pos.getY() + 0.0625D + ascendingOffset, pos.getZ() + 0.5D);
            if (stack.hasCustomHoverName())
                minecart.setCustomName(stack.getHoverName());

            level.addFreshEntity(minecart);
            level.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(context.getPlayer(), level.getBlockState(pos.below())));
        }

        stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
