package com.stump.songcraft_instruments.block.partial;

import com.stump.songcraft_instruments.block.blockentity.DummyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.*;

public abstract class MultiblockInstrumentBlock extends AbstractInstrumentBlock {

    public enum Part implements StringRepresentable {
        CONTROLLER,
        DUMMY;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    // prevent double break recursion
    private static final Set<BlockPos> ACTIVE_BREAKS = new HashSet<>();


    protected abstract BlockPos[] getLayoutOffsets();
    protected abstract BlockState dummyState(BlockState controllerState);
    protected abstract Item getDropItem();

    public MultiblockInstrumentBlock(Properties props) {
        super(props);

        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.CONTROLLER));
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();

        BlockState state = defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(PART, Part.CONTROLLER);

        Direction facing = state.getValue(FACING);

        // Check all required positions BEFORE placing anything
        for (BlockPos local : getLayoutOffsets()) {
            BlockPos target = pos.offset(rotate(facing, local));

            if (!level.getBlockState(target).canBeReplaced(ctx)) {
                return null; // cancel placement
            }
        }

        return state;
    }
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            LivingEntity placer, ItemStack stack) {

        if (state.getValue(PART) != Part.CONTROLLER) return;

        Direction facing = state.getValue(FACING);

        List<BlockPos> placed = new ArrayList<>();

        // place dummy blocks
        for (BlockPos local : getLayoutOffsets()) {
            BlockPos target = pos.offset(rotate(facing, local));

            level.setBlock(target, dummyState(state), 3);
            placed.add(target);
        }

        // link dummies to controller
        for (BlockPos p : placed) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof DummyBlockEntity dummy) {
                dummy.setControllerPos(pos);
            }
        }

        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof InstrumentBlockEntity instrument) {
            CompoundTag tag = stack.getTag();

            if (tag != null && tag.contains("DyeColor")) {
                instrument.setDyeColor(tag.getInt("DyeColor"));
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        BlockPos controllerPos = getController(level, pos);
        return onControllerUse(level.getBlockState(controllerPos), level, controllerPos, player, hand, hit);
    }

    protected InteractionResult onControllerUse(BlockState state, Level level, BlockPos controllerPos,
                                                Player player, InteractionHand hand, BlockHitResult hit) {

        InteractionResult dyeResult =
                tryApplyDye(level, controllerPos, player, hand);

        if (dyeResult != null)
            return dyeResult;

        return super.use(state, level, controllerPos, player, hand, hit);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        breakMultiblock(level, pos, player);
        super.playerWillDestroy(level, pos, state, player);
    }

    private void breakMultiblock(Level level, BlockPos pos, Player player) {
        BlockPos controller = getController(level, pos);

        if (!ACTIVE_BREAKS.add(controller)) return;

        try {
            BlockState controllerState = level.getBlockState(controller);
            Direction facing = controllerState.getValue(FACING);

            List<BlockPos> toBreak = new ArrayList<>();

            // collect all parts
            for (BlockPos local : getLayoutOffsets()) {
                toBreak.add(controller.offset(rotate(facing, local)));
            }
            toBreak.add(controller);

            // drop item once
            if (player != null && !player.isCreative()) {
                ItemStack stack = new ItemStack(getDropItem());

                BlockEntity be = level.getBlockEntity(controller);

                if (be instanceof InstrumentBlockEntity instrument) {
                    CompoundTag tag = stack.getOrCreateTag();
                    tag.putInt("DyeColor", instrument.getDyeColor());
                }

                Block.popResource(level, controller, stack);
            }

            // remove all blocks
            for (BlockPos p : toBreak) {
                if (level.getBlockState(p).getBlock() == this) {
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }

        } finally {
            ACTIVE_BREAKS.remove(controller);
        }
    }

    public BlockPos getController(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof DummyBlockEntity dummy) {
            BlockPos controller = dummy.getControllerPos();
            if (controller != null)
                return controller;
        }

        return pos;
    }

    @Override
    public BlockPos getInstrumentPos(Level level, BlockPos pos) {
        return getController(level, pos);
    }

    protected BlockPos rotate(Direction facing, BlockPos local) {
        return switch (facing) {
            case NORTH -> new BlockPos(local.getX(), local.getY(), local.getZ());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case WEST  -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case EAST  -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            default -> throw new IllegalStateException("Unexpected value: " + facing);
        };
    }
}