package com.stump.songcraft_instruments.block;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.item.ModItems;
import com.stump.songcraft_instruments.block.partial.AbstractInstrumentBlock;
import com.stump.songcraft_instruments.block.partial.InstrumentBlockEntity;
import com.stump.songcraft_instruments.networking.packet.instrument.util.InstrumentPacketUtil;
import com.stump.songcraft_instruments.util.LooperUtil;
import com.stump.songcraft_instruments.util.SpeakerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.stump.songcraft_instruments.block.blockentity.KeyboardStandBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Can hold water, so it can be placed underwater (and isn't washed away by flowing water or lava)
 */
public class KeyboardStandBlock extends AbstractInstrumentBlock implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_KEYBOARD = BooleanProperty.create("has_keyboard");
    /**
     * Whether the keyboard faces the back of the stand, rather than its front
     */
    public static final BooleanProperty KEYBOARD_FLIPPED = BooleanProperty.create("keyboard_flipped");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;


    // The stand is as wide and deep as the keyboard
    private static final VoxelShape STAND_NS =
            Block.box(0.0D, 0.0D, 4.0D, 16.0D, 9.0D, 12.8D);

    private static final VoxelShape STAND_EW =
            Block.box(3.5D, 0.0D, 0.0D, 12.0D, 9.0D, 16.0D);

    private static final VoxelShape KEYBOARD_NS =
            Shapes.or(
                    Block.box(0.0D, 9.0D, 4.0D, 15.65D, 13.4D, 12.8D),
                    Block.box(0.3D, 9.0D, 4.0D, 16.0D, 13.4D, 12.8D)
            );

    private static final VoxelShape KEYBOARD_EW =
            Shapes.or(
                    Block.box(3.5D, 9.0D, 0.0D, 12.0D, 13.4D, 15.65D),
                    Block.box(3.5D, 9.0D, 0.3D, 12.0D, 13.4D, 16.0D)
            );

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        final VoxelShape standShape = isNorthSouth(pState.getValue(FACING)) ? STAND_NS : STAND_EW;
        if (!pState.getValue(HAS_KEYBOARD))
            return standShape;

        // Include the stand, so it can be targeted (and broken along with the keyboard)
        return Shapes.or(standShape, getKeyboardShape(pState));
    }

    private static VoxelShape getKeyboardShape(final BlockState state) {
        return isNorthSouth(getKeyboardFacing(state)) ? KEYBOARD_NS : KEYBOARD_EW;
    }
    private static boolean isNorthSouth(final Direction facing) {
        return facing == Direction.NORTH || facing == Direction.SOUTH;
    }

    /**
     * @return The direction the keyboard on the stand faces; the stand's front or back
     */
    public static Direction getKeyboardFacing(final BlockState state) {
        final Direction facing = state.getValue(FACING);
        return state.getValue(KEYBOARD_FLIPPED) ? facing.getOpposite() : facing;
    }

    public KeyboardStandBlock(Properties pProperties) {
        super(pProperties);
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(HAS_KEYBOARD, false)
            .setValue(KEYBOARD_FLIPPED, false)
            .setValue(WATERLOGGED, false)
        );
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {

        ItemStack stack = player.getItemInHand(hand);

        InteractionResult dyeResult = tryApplyDye(level, pos, player, hand);

        if (dyeResult != null)
            return dyeResult;

        // place a keyboard onto the stand
        if (!state.getValue(HAS_KEYBOARD) && stack.is(ModItems.KEYBOARD.get())) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);

                if (be instanceof KeyboardStandBlockEntity stand) {
                    CompoundTag tag = stack.getTag();
                    if (tag != null && tag.contains("DyeColor")) {
                        stand.setDyeColor(tag.getInt("DyeColor"));
                    } else {
                        stand.setDyeColor(0xFFFFFF);
                    }
                    // Face the player placing the keyboard, along the stand's front or back only
                    final Direction front = state.getValue(FACING);
                    final Vec3 toPlayer = player.position().subtract(Vec3.atCenterOf(pos));
                    final boolean playerBehind = (toPlayer.x * front.getStepX() + toPlayer.z * front.getStepZ()) < 0;

                    level.setBlock(pos, state
                        .setValue(HAS_KEYBOARD, true)
                        .setValue(KEYBOARD_FLIPPED, playerBehind),
                    3);

                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // empty stand
        if (!state.getValue(HAS_KEYBOARD)) {
            return InteractionResult.FAIL;
        }

        // keyboard interaction
        return super.use(state, level, pos, player, hand, hit);
    }

    /**
     * Breaking the keyboard part only takes the keyboard off the stand.
     * Breaking the stand part breaks both.
     */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos,
            Player player, boolean willHarvest, FluidState fluid) {

        if (state.getValue(HAS_KEYBOARD)) {
            dropKeyboard(level, pos, player);

            if (isTargetingKeyboard(state, pos, player)) {
                // The stand holds the keyboard's connections; they go with the keyboard
                final BlockEntity be = level.getBlockEntity(pos);
                if (!level.isClientSide && be != null) {
                    LooperUtil.disconnectBlockInstrument(level, be);
                    SpeakerUtil.unpairAll(level, be);
                }

                level.setBlock(pos, state.setValue(HAS_KEYBOARD, false), 3);
                // Keep the stand
                return false;
            }
        }

        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    private static void dropKeyboard(final Level level, final BlockPos pos, final Player player) {
        if (level.isClientSide || player.isCreative())
            return;

        ItemStack keyboard = new ItemStack(ModItems.KEYBOARD.get());
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof KeyboardStandBlockEntity stand) {
            keyboard.getOrCreateTag().putInt("DyeColor", stand.getDyeColor());
        }

        level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), keyboard));
    }

    /**
     * @return Whether the player is looking at the keyboard part of the stand
     */
    private static boolean isTargetingKeyboard(final BlockState state, final BlockPos pos, final Player player) {
        final HitResult hit = player.pick(player.getBlockReach(), 1, false);
        if (!(hit instanceof BlockHitResult blockHit) || !blockHit.getBlockPos().equals(pos))
            return false;

        // The hit point lies on the surface of the part that was hit
        final Vec3 local = blockHit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
        return getKeyboardShape(state).toAabbs().stream()
            .anyMatch((box) -> box.inflate(1.0E-4).contains(local));
    }


    @Override
    protected void onInstrumentOpen(ServerPlayer player) {
        InstrumentPacketUtil.sendOpenPacket(player, new ResourceLocation(SCInstrumentMod.MODID, "keyboard"));
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }


    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return defaultBlockState()
            .setValue(FACING, pContext.getHorizontalDirection().getOpposite())
            .setValue(WATERLOGGED, pContext.getLevel().getFluidState(pContext.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public InstrumentBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KeyboardStandBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, HAS_KEYBOARD, KEYBOARD_FLIPPED, WATERLOGGED);
    }

}