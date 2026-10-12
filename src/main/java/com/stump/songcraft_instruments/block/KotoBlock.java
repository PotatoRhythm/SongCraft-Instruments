package com.stump.songcraft_instruments.block;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.DummyBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.KotoBlockEntity;
import com.stump.songcraft_instruments.block.partial.InstrumentBlockEntity;
import com.stump.songcraft_instruments.block.partial.MultiblockInstrumentBlock;
import com.stump.songcraft_instruments.item.ModItems;
import com.stump.songcraft_instruments.networking.packet.instrument.util.InstrumentPacketUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class KotoBlock extends MultiblockInstrumentBlock {
    private static final BlockPos[] SHAPE = {
            // Controller is (0,0,0)
            new BlockPos(-1, 0, 0)
    };

    private static final VoxelShape LEFT_NS =
            Block.box(0.0D, 0.0D, 4.0D, 15.65D, 4.4D, 12.8D);
    private static final VoxelShape LEFT_EW =
            Block.box(3.5D, 0.0D, 0.0D, 12D, 4.4D, 15.65D);
    private static final VoxelShape RIGHT_NS =
            Block.box(0.3D, 0.0D, 4D, 16.0D, 4.4D, 12.8D);
    private static final VoxelShape RIGHT_EW =
            Block.box(3.5D, 0.0D, 0.3D, 12D, 4.4D, 16D);

    public KotoBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    protected BlockPos[] getLayoutOffsets() {
        return SHAPE;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.KOTO.get();
    }

    @Override
    protected BlockState dummyState(BlockState controllerState) {
        return ModBlocks.KOTO.get().defaultBlockState()
                .setValue(FACING, controllerState.getValue(FACING))
                .setValue(PART, Part.DUMMY);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == Part.CONTROLLER
                ? RenderShape.ENTITYBLOCK_ANIMATED
                : RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        final Direction facing = state.getValue(FACING);
        final boolean ns = facing == Direction.NORTH || facing == Direction.SOUTH;

        if (state.getValue(PART) == Part.CONTROLLER)
            return ns ? LEFT_NS : LEFT_EW;

        return ns ? RIGHT_NS : RIGHT_EW;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    @Override
    public InstrumentBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == Part.CONTROLLER
                ? new KotoBlockEntity(pos, state)
                : new DummyBlockEntity(pos, state);
    }

    @Override
    protected void onInstrumentOpen(ServerPlayer player) {
        InstrumentPacketUtil.sendOpenPacket(player, new ResourceLocation(SCInstrumentMod.MODID, "koto"));
    }
}
