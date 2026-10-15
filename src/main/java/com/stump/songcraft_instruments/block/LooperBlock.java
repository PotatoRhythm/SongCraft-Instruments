package com.stump.songcraft_instruments.block;

import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.ModBlockEntities;
import com.stump.songcraft_instruments.block.blockentity.looper.LooperOwners;
import com.stump.songcraft_instruments.block.util.LooperInteractionRunnable;
import com.stump.songcraft_instruments.criteria.ModCriteria;
import com.stump.songcraft_instruments.item.record.WritableRecordItem;
import com.stump.songcraft_instruments.util.LooperUtil;
import com.stump.songcraft_instruments.item.InstrumentItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.redstone.Redstone;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Function;

public class LooperBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty PLAYING = BooleanProperty.create("playing");
    public static final BooleanProperty RECORD_IN = BooleanProperty.create("record_in");
    public static final BooleanProperty REDSTONE_TRIGGERED = BooleanProperty.create("redstone_triggered");
    public static final BooleanProperty LOOPING = BooleanProperty.create("looping");

    /**
     * Block events animating the volume buttons being pressed, see {@link LooperBlockEntity#triggerEvent}
     */
    public static final int PLUS_PRESSED_EVENT = 0, MINUS_PRESSED_EVENT = 1;

    public LooperBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(PLAYING, false)
            .setValue(RECORD_IN, false)
            .setValue(FACING, Direction.NORTH)
            .setValue(REDSTONE_TRIGGERED, false)
            .setValue(LOOPING, true)
        );
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        pBuilder.add(PLAYING, FACING, RECORD_IN, REDSTONE_TRIGGERED, LOOPING);
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        final Player player = pContext.getPlayer();
        if (player != null && !pContext.getLevel().isClientSide && LooperOwners.refusePlacement(player))
            return null;

        return defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        if (!pLevel.isClientSide && pPlacer instanceof Player player)
            LooperOwners.get(pLevel.getServer()).addBlock(GlobalPos.of(pLevel.dimension(), pPos), player.getUUID());

        final boolean hasFootage = pLevel
            .getBlockEntity(pPos, ModBlockEntities.LOOPER.get())
            .orElseThrow()
            .hasFootage();

        if (hasFootage) {
            pLevel.setBlockAndUpdate(pPos, pState
                .setValue(RECORD_IN, true)
                .setValue(PLAYING, true)
            );
        }

        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new LooperBlockEntity(pPos, pState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState,
            BlockEntityType<T> pBlockEntityType) {
                
        return (!pLevel.isClientSide && pBlockEntityType == ModBlockEntities.LOOPER.get())
            ? (level, pos, state, be) -> ((LooperBlockEntity)(be)).tick(level, pos, state)
            : null;
    }

    public static BlockState cyclePlaying(LooperBlockEntity lbe, BlockState state) {
        return lbe.setPlaying(!state.getValue(PLAYING), state);
    }

    //#region Looper interaction handlers

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand,
            BlockHitResult pHit) {
        if (pLevel.isClientSide)
            return InteractionResult.CONSUME;

        final BlockEntity be = pLevel.getBlockEntity(pPos);
        if (!(be instanceof LooperBlockEntity lbe))
            return InteractionResult.FAIL;

        final ItemStack heldStack = pPlayer.getItemInHand(pHand);

        return performChainedInteractions(
            List.of(
                this::pressVolumeButton,
                this::insertRecord,
                this::cycleLooping,
                this::validateRecordPresence,
                //   /\ Do not perform all following interactions if there is no record
                this::pairInstrumentItem,
                this::ejectRecord,
                this::cyclePlaying
            ),
            (interaction) -> interaction.run(pState, pLevel, pPos, pPlayer, lbe, heldStack, pHit)
        );
    }

    /**
     * Performs the described interactions one after another, until one does not return
     * {@link InteractionResult#FAIL}.
     * @return The interaction result of the successful interaction, or {@link InteractionResult#FAIL if none.
     */
    protected InteractionResult performChainedInteractions(final List<LooperInteractionRunnable> interactions,
                                                           Function<LooperInteractionRunnable, InteractionResult> performer) {
        for (LooperInteractionRunnable interaction : interactions) {
            final InteractionResult result = performer.apply(interaction);
            if (result != InteractionResult.FAIL)
                return result;
        }

        return InteractionResult.FAIL;
    }
    /**
     * Turns the volume up or down by 10%, provided the player right-clicked on the plus or minus button
     * on {@link LooperBlock#getControlsDir the looper's side}.
     */
    protected InteractionResult pressVolumeButton(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer,
                                                  LooperBlockEntity lbe, ItemStack heldStack, BlockHitResult pHit) {
        if (pHit.getDirection() != getControlsDir(pState))
            return InteractionResult.FAIL;

        // Where the side was clicked, in the model's pixels: z counts from the front face, y from the bottom
        final Vec3 local = pHit.getLocation().subtract(Vec3.atLowerCornerOf(pPos)).subtract(0.5, 0, 0.5);
        final Direction front = pState.getValue(FACING);
        final double z = (0.5 - (local.x * front.getStepX() + local.z * front.getStepZ())) * 16;
        final double y = local.y * 16;

        // The buttons are 1 pixel across; give them a 4 pixel square around that
        if (y < 11 || y > 15)
            return InteractionResult.FAIL;

        final boolean plus;
        if (z >= 0 && z <= 4)
            plus = true;
        else if (z >= 12 && z <= 16)
            plus = false;
        else
            return InteractionResult.FAIL;

        lbe.stepVolume(plus);

        pLevel.blockEvent(pPos, this, plus ? PLUS_PRESSED_EVENT : MINUS_PRESSED_EVENT, 0);
        pLevel.playSound(null, pPos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3f, plus ? 0.7f : 0.6f);

        pPlayer.displayClientMessage(
            Component.translatable("songcraft_instruments.looper.volume", lbe.getVolume()),
            true
        );
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean triggerEvent(BlockState pState, Level pLevel, BlockPos pPos, int pId, int pParam) {
        super.triggerEvent(pState, pLevel, pPos, pId, pParam);
        final BlockEntity be = pLevel.getBlockEntity(pPos);
        return (be != null) && be.triggerEvent(pId, pParam);
    }

    /**
     * Attempts to cycle the {@link LooperBlock#LOOPING looping} state of the looper,
     * provided the player right-clicked on {@link LooperBlock#getLoopDir the correct face}.
     */
    protected InteractionResult cycleLooping(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer,
                                             LooperBlockEntity lbe, ItemStack heldStack, BlockHitResult pHit) {
        if (pHit.getDirection() != getLoopDir(pState))
            return InteractionResult.FAIL;

        pLevel.setBlockAndUpdate(pPos, pState.cycle(LOOPING));
        return InteractionResult.SUCCESS;
    }
    /**
     * Assuming server-only call.
     */
    protected InteractionResult insertRecord(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer,
                                             LooperBlockEntity lbe, ItemStack heldStack, BlockHitResult pHit) {
        if (!(heldStack.getItem() instanceof WritableRecordItem))
            return InteractionResult.FAIL;

        // Eject previously inserted record
        if (pState.getValue(RECORD_IN)) {
            lbe.popRecord();
        }

        lbe.setItem(0, heldStack);
        // Trigger record injected criterion
        ModCriteria.RECORD_INJECTED_TRIGGER.trigger((ServerPlayer)pPlayer, heldStack);

        if (!pPlayer.isCreative())
            heldStack.shrink(1);

        if (lbe.isRecordingMissing()) {
            pPlayer.displayClientMessage(
                Component.translatable("songcraft_instruments.record.missing").withStyle(ChatFormatting.RED),
                true
            );
        } else if (!lbe.hasFootage()) {
            pPlayer.displayClientMessage(
                Component.translatable("songcraft_instruments.record.no_footage"),
                true
            );
        }

        return InteractionResult.SUCCESS;
    }
    protected InteractionResult validateRecordPresence(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer,
                                                       LooperBlockEntity lbe, ItemStack heldStack, BlockHitResult pHit) {
        if (!pState.getValue(RECORD_IN)) {
            pPlayer.displayClientMessage(
                Component.translatable("songcraft_instruments.looper.no_record").withStyle(ChatFormatting.RED),
                true
            );
            return InteractionResult.CONSUME_PARTIAL;
        }

        return InteractionResult.FAIL;
    }

    /**
     * Ejects the present record on the condition it is empty or the player is shifting.
     */
    protected InteractionResult ejectRecord(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer,
                                            LooperBlockEntity lbe, ItemStack heldStack, BlockHitResult pHit) {
        if (!pState.getValue(RECORD_IN))
            return InteractionResult.FAIL;

        if (pPlayer.isShiftKeyDown() || !lbe.hasFootage()) {
            lbe.popRecord();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }
    protected InteractionResult pairInstrumentItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer,
                                                   LooperBlockEntity lbe, ItemStack heldStack, BlockHitResult pHit) {
        if (!(heldStack.getItem() instanceof InstrumentItem))
            return InteractionResult.FAIL;

        if (lbe.isWritable()) {
            if (LooperUtil.performPair(lbe, (connectionId) -> LooperUtil.createLooperTag(heldStack, pPos, connectionId), pPlayer))
                return InteractionResult.SUCCESS;
        } else {
            pPlayer.displayClientMessage(
                Component.translatable("songcraft_instruments.record.not_writable").withStyle(ChatFormatting.YELLOW)
                , true);
            return InteractionResult.FAIL;
        }

        return InteractionResult.FAIL;
    }
    protected InteractionResult cyclePlaying(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer,
                                             LooperBlockEntity lbe, ItemStack heldStack, BlockHitResult pHit) {
        if (!lbe.hasFootage()) {
            if (lbe.isWritable()) {
                pPlayer.displayClientMessage(
                    Component.translatable("songcraft_instruments.record.no_footage"),
                    true
                );
            }

            return InteractionResult.CONSUME_PARTIAL;
        }

        pLevel.setBlockAndUpdate(pPos,
            cyclePlaying(lbe, pState)
                .setValue(REDSTONE_TRIGGERED, false)
        );
        return InteractionResult.SUCCESS;
    }

    //#endregion


    @Override
    public void playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
        super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
        if (pLevel.isClientSide())
            return;

        if (!(pLevel.getBlockEntity(pPos) instanceof LooperBlockEntity lbe))
            return;

        lbe.popRecord();
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        if (!pLevel.isClientSide && !pState.is(pNewState.getBlock()))
            LooperOwners.get(pLevel.getServer()).removeBlock(GlobalPos.of(pLevel.dimension(), pPos));

        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }


    //#region redstone impl

    public Direction getLoopDir(final BlockState state) {
        return state.getValue(FACING).getOpposite();
    }
    public Direction getPlayDir(final BlockState state) {
        return state.getValue(FACING);
    }
    /**
     * @return The side the volume buttons and scale are on
     */
    public Direction getControlsDir(final BlockState state) {
        return state.getValue(FACING).getClockWise();
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pNeighborBlock,
            BlockPos pNeighborPos, boolean pMovedByPiston) {
        if (pLevel.isClientSide)
            return;
        if (!(pLevel.getBlockEntity(pPos) instanceof LooperBlockEntity lbe))
            return;

        boolean wasRedstoneTriggered = pState.getValue(REDSTONE_TRIGGERED);

        if (pLevel.hasNeighborSignal(pPos)) {
            // This mechanism should act alike as a T-flip-flop.
            // We must be sure that it wasn't just some random block update messing up
            // with the toggle.
            if (!wasRedstoneTriggered) {
                lbe.setTicks(0);

                BlockState newState = pState;
                if (pState.getValue(LOOPING))
                    newState = cyclePlaying(lbe, newState);
                else
                    newState = lbe.setPlaying(true, newState);

                pLevel.setBlockAndUpdate(pPos, newState.setValue(REDSTONE_TRIGGERED, true));
            }
        } else if (wasRedstoneTriggered) {
            pLevel.setBlock(pPos, pState.setValue(REDSTONE_TRIGGERED, false), 1);
        }
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return getPlayDir(state).getOpposite() == direction;
    }


    @Override
    public boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }
    /**
     * @return A signal of 15 if this looper is activated. 0 otherwise.
     */
    @Override
    public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
        return (pState.getValue(PLAYING) ? 1 : 0) * Redstone.SIGNAL_MAX;
    }

    //#endregion
}
