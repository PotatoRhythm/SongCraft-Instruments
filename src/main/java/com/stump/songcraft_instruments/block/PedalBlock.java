package com.stump.songcraft_instruments.block;

import com.stump.songcraft_instruments.block.blockentity.PedalBlockEntity;
import com.stump.songcraft_instruments.pedal.PedalEffectType;
import com.stump.songcraft_instruments.pedal.PedalLinks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * An effects pedal. Instruments plug into it, and pedals chain into each other, with the speaker cable
 * (see {@link PedalLinks}); held instruments plug in by right-clicking it.
 * Right-click with an empty hand to turn its knobs, shift + right-click with an empty hand to stomp it on or off.
 */
public class PedalBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /**
     * whether the pedal's effect is on; a bypassed pedal passes notes through untouched
     */
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");

    private static final VoxelShape SHAPE_NS = Block.box(3, 0, 1, 13, 5, 15),
        SHAPE_EW = Block.box(1, 0, 3, 15, 5, 13);

    public final PedalEffectType effectType;

    public PedalBlock(final PedalEffectType effectType, final Properties properties) {
        super(properties);
        this.effectType = effectType;
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(ENABLED, true)
        );
    }

    @Override
    protected void createBlockStateDefinition(final Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, ENABLED);
    }
    /**
     * places the pedal with its footswitch towards the player
     */
    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext pContext) {
        return defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(final BlockState pState, final Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }
    @Override
    public BlockState mirror(final BlockState pState, final Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(final BlockState pState, final BlockGetter pLevel, final BlockPos pPos, final CollisionContext pContext) {
        return (pState.getValue(FACING).getAxis() == Direction.Axis.Z) ? SHAPE_NS : SHAPE_EW;
    }


    @Override
    public InteractionResult use(final BlockState pState, final Level pLevel, final BlockPos pPos, final Player pPlayer,
                                 final InteractionHand pHand, final BlockHitResult pHit) {
        if (pLevel.isClientSide)
            return InteractionResult.SUCCESS;
        if (!(pLevel.getBlockEntity(pPos) instanceof PedalBlockEntity pedal))
            return InteractionResult.PASS;

        if (pPlayer.isShiftKeyDown()) {
            pedal.toggleBypassed();
        } else if (pPlayer instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, pedal, pPos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(final Level pLevel, final BlockPos pPos, final BlockState pState,
                            @Nullable final LivingEntity pPlacer, final ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        // A new pedal never starts plugged in, even if the item carried a copied pedal's data
        if (!pLevel.isClientSide && pLevel.getBlockEntity(pPos) instanceof PedalBlockEntity pedal)
            pedal.resetAsNew();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(final BlockState pState, final Level pLevel, final BlockPos pPos, final BlockState pNewState,
                         final boolean pMovedByPiston) {
        if (!pLevel.isClientSide && !pState.is(pNewState.getBlock())
            && pLevel.getBlockEntity(pPos) instanceof PedalBlockEntity pedal)
            PedalLinks.disconnect(pLevel, pedal);

        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pPos, final BlockState pState) {
        return new PedalBlockEntity(pPos, pState);
    }
}
