package com.stump.songcraft_instruments.block;

import com.stump.songcraft_instruments.block.blockentity.ModBlockEntities;
import com.stump.songcraft_instruments.block.blockentity.SpeakerBlockEntity;
import com.stump.songcraft_instruments.item.InstrumentItem;
import com.stump.songcraft_instruments.pedal.PedalMonitorSync;
import com.stump.songcraft_instruments.util.SpeakerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import javax.annotation.Nullable;

/**
 * once a speaker is paired to an instrument, the speaker immediately plays that instrument's notes from its own position.
 * Block instruments are paired via the SpeakerCableItem, while held instruments are paired
 * by right-clicking the speaker with the instrument in hand, and unpaired by shift + right-clicking it.
 */
public class SpeakerBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /**
     * whether at least one instrument is paired to this speaker; switches the front texture.
     * managed by the speaker's pair count.
     */
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    public SpeakerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(CONNECTED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, CONNECTED);
    }
    /**
     * places the speaker with its front facing the player
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    // keep the front pointing the right way when structures are rotated/mirrored
    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }
    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        if (!pLevel.isClientSide && pLevel.getBlockEntity(pPos) instanceof SpeakerBlockEntity sbe)
            sbe.speaker().resetPairings();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        final boolean hadPedals = !pLevel.isClientSide && !pState.is(pNewState.getBlock())
            && (pLevel.getBlockEntity(pPos) instanceof SpeakerBlockEntity sbe) && !sbe.speaker().getPedalChain().isEmpty();

        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);

        // Once it's gone, so players playing through it stop hearing its pedals on their own notes
        if (hadPedals)
            PedalMonitorSync.onSpeakerChainChanged(pLevel);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new SpeakerBlockEntity(pPos, pState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState,
            BlockEntityType<T> pBlockEntityType) {

        return (!pLevel.isClientSide && pBlockEntityType == ModBlockEntities.SPEAKER.get())
            ? (level, pos, state, be) -> ((SpeakerBlockEntity)(be)).tick(level)
            : null;
    }

    /**
     * called by {@link InstrumentItem#onItemUseFirst} when a held instrument right-clicks a speaker,
     * which runs before any block interaction and regardless of sneaking.
     * right-click pairs the instrument to this speaker, shift + right-click unpairs it.
     */
    public static InteractionResult onHeldInstrumentUse(Level pLevel, BlockPos pPos, Player pPlayer, ItemStack heldStack) {
        // Consume on the client too, so the instrument's screen doesn't open
        if (pLevel.isClientSide)
            return InteractionResult.SUCCESS;

        if (!(pLevel.getBlockEntity(pPos) instanceof SpeakerBlockEntity sbe))
            return InteractionResult.FAIL;

        SpeakerUtil.useHeldInstrument(pLevel, pPlayer, heldStack, sbe.speaker());
        return InteractionResult.SUCCESS;
    }
}
