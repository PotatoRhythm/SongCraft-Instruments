package com.stump.songcraft_instruments.item;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.LooperBlock;
import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.SyncModTagPacket;
import com.stump.songcraft_instruments.util.CommonUtil;
import com.stump.songcraft_instruments.util.LooperUtil;
import com.stump.songcraft_instruments.block.partial.AbstractInstrumentBlock;
import com.stump.songcraft_instruments.block.partial.InstrumentBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public class LooperAdapterItem extends Item {
    private static final String BLOCK_INSTRUMENT_POS_TAG = "instrument_block",
        LOOPER_POS_TAG = "looper";

    public LooperAdapterItem(Properties pProperties) {
        super(pProperties);
    }
    

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if (pContext.getLevel().isClientSide)
            return InteractionResult.CONSUME_PARTIAL;

        final Level level = pContext.getLevel();
        final BlockPos clickedPos = pContext.getClickedPos();
        final Block block = level.getBlockState(clickedPos).getBlock();

        BlockPos pos = clickedPos;

        if (block instanceof AbstractInstrumentBlock instrumentBlock) {
            pos = instrumentBlock.getInstrumentPos(level, clickedPos);
        }

        final CompoundTag adapterTag = CommonUtil.getOrCreateElementTag(
                SCInstrumentMod.modTag(pContext.getItemInHand()), "looperAdapter");
        final Player player = pContext.getPlayer();

        boolean pairSucceed;
        if (block instanceof AbstractInstrumentBlock)
            pairSucceed = handleInstrumentBlock(pos, adapterTag, player);
        else if (block instanceof LooperBlock)
            pairSucceed = handleLooperBlock(pos, adapterTag, player);
        else
            return InteractionResult.FAIL;

        return pairSucceed ? InteractionResult.SUCCESS : InteractionResult.CONSUME_PARTIAL;
    }

    private static boolean handleInstrumentBlock(BlockPos blockPos, CompoundTag adapterTag, Player player) {
        if (adapterTag.contains(LOOPER_POS_TAG, Tag.TAG_COMPOUND))
            return pairLooperToInstrument(adapterTag, NbtUtils.readBlockPos(adapterTag.getCompound(LOOPER_POS_TAG)), blockPos, player);

        adapterTag.put(BLOCK_INSTRUMENT_POS_TAG, NbtUtils.writeBlockPos(blockPos));
        player.displayClientMessage(
            Component.translatable("item.songcraft_instruments.looper_adapter.looper.select")
        , true);
        return true;
    }
    private static boolean handleLooperBlock(BlockPos blockPos, CompoundTag adapterTag, Player player) {
        final BlockEntity be = player.level().getBlockEntity(blockPos);
        if (!(be instanceof LooperBlockEntity lbe))
            return false;

        if (!validateRecordPresence(lbe, player))
            return false;

        if (adapterTag.contains(BLOCK_INSTRUMENT_POS_TAG, Tag.TAG_COMPOUND))
            return pairLooperToInstrument(adapterTag, blockPos, NbtUtils.readBlockPos(adapterTag.getCompound(BLOCK_INSTRUMENT_POS_TAG)), player);
        if (adapterTag.contains(LOOPER_POS_TAG, Tag.TAG_COMPOUND))
            return syncLoopers(adapterTag, NbtUtils.readBlockPos(adapterTag.getCompound(LOOPER_POS_TAG)), blockPos, player);

        adapterTag.put(LOOPER_POS_TAG, NbtUtils.writeBlockPos(blockPos));
        player.displayClientMessage(
            Component.translatable("item.songcraft_instruments.looper_adapter.instrument.select").withStyle(ChatFormatting.GREEN),
            true
        );
        return true;
    }

    /**
     * Instruments may only be paired to, and loopers synced with, a looper holding a record
     */
    private static boolean validateRecordPresence(LooperBlockEntity lbe, Player player) {
        if (lbe.isRecordIn())
            return true;

        player.displayClientMessage(
            Component.translatable("songcraft_instruments.looper.no_record")
                .withStyle(ChatFormatting.RED),
            true
        );
        return false;
    }


    private static boolean pairLooperToInstrument(CompoundTag adapterTag, InstrumentBlockEntity ibe, LooperBlockEntity lbe, Player player) {
        // Clear all compound keys after pairing
        for (final String key : adapterTag.getAllKeys())
            adapterTag.remove(key);

        // The player using the adapter owns the block instrument's connection
        return LooperUtil.performPair(lbe, (connectionId) -> {

            final BlockPos instrumentBlockPos = ibe.getBlockPos(),
                looperBlockPos = lbe.getBlockPos();

            LooperUtil.createLooperTag(ibe, looperBlockPos, connectionId);
            ibe.setChanged();

            // Handle syncing data to client
            if (player instanceof ServerPlayer serverPlayer)
                SCPacketHandler.sendToClient(new SyncModTagPacket(SCInstrumentMod.modTag(ibe), instrumentBlockPos), serverPlayer);

        }, player);
    }
    private static boolean pairLooperToInstrument(CompoundTag adapterTag, BlockPos looperPos, BlockPos instrumentPos, Player player) {
        final Level level = player.level();

        final BlockEntity lbe = level.getBlockEntity(looperPos),
            ibe = level.getBlockEntity(instrumentPos);
        if (!(lbe instanceof LooperBlockEntity) || !(ibe instanceof InstrumentBlockEntity))
            return false;

        return pairLooperToInstrument(adapterTag, (InstrumentBlockEntity)ibe, (LooperBlockEntity)lbe, player);
    }

    /**
     * Syncs the 2 loopers, or unsyncs them if they already were. While synced, the burned record in one
     * plays as the empty record in the other is recorded on, and is burned into it along with the new part.
     */
    private static boolean syncLoopers(CompoundTag adapterTag, LooperBlockEntity lbe1, LooperBlockEntity lbe2, Player player) {
        // Clear all compound keys after pairing
        for (final String key : adapterTag.getAllKeys())
            adapterTag.remove(key);

        if (isInSession(lbe1) || isInSession(lbe2)) {
            player.displayClientMessage(
                Component.translatable("songcraft_instruments.looper.connections_locked").withStyle(ChatFormatting.RED),
                true
            );
            return true;
        }

        final boolean unsync = (lbe1.getSyncedLooper() == lbe2);

        // The record of one is recorded over onto the other's empty record
        if (!unsync && !lbe1.isWritable() && !lbe2.isWritable()) {
            player.displayClientMessage(
                Component.translatable("item.songcraft_instruments.looper_adapter.looper.no_empty_record").withStyle(ChatFormatting.RED),
                true
            );
            return true;
        }

        // A looper may only be synced with one other
        unsync(lbe1);
        unsync(lbe2);

        if (!unsync) {
            lbe1.setSyncedLooperPos(lbe2.getBlockPos());
            lbe2.setSyncedLooperPos(lbe1.getBlockPos());
        }

        // Update their players on what they would record over
        lbe1.connections().sync();
        lbe2.connections().sync();

        player.displayClientMessage(
            Component.translatable(unsync
                ? "item.songcraft_instruments.looper_adapter.looper.unsync"
                : "item.songcraft_instruments.looper_adapter.looper.success_pair"
            ).withStyle(ChatFormatting.GREEN),
            true
        );
        return true;
    }

    private static boolean isInSession(LooperBlockEntity lbe) {
        return lbe.session().isGroupSession() || lbe.session().isRecording();
    }

    /**
     * Removes the looper's link to its synced looper, on both ends
     */
    private static void unsync(LooperBlockEntity lbe) {
        final LooperBlockEntity other = lbe.getSyncedLooper();
        lbe.setSyncedLooperPos(null);

        if (other != null) {
            other.setSyncedLooperPos(null);
            other.connections().sync();
        }
    }

    private static boolean syncLoopers(CompoundTag adapterTag, BlockPos looper1Pos, BlockPos looper2Pos, Player player) {
        if (looper2Pos.equals(looper1Pos))
            return false;

        final Level level = player.level();

        BlockEntity lbe1 = level.getBlockEntity(looper1Pos),
            lbe2 = level.getBlockEntity(looper2Pos);

        if (!(lbe1 instanceof LooperBlockEntity) || !(lbe2 instanceof LooperBlockEntity))
            return false;

        return syncLoopers(adapterTag, (LooperBlockEntity)lbe1, (LooperBlockEntity)lbe2, player);
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (!Screen.hasShiftDown()) {
            tooltipComponents.add(
                Component.translatable("item.shift.hint.show")
                    .withStyle(ChatFormatting.YELLOW)
            );
            return;
        } else {
            tooltipComponents.add(
                Component.translatable("item.shift.hint.hide")
                    .withStyle(ChatFormatting.YELLOW)
            );
        }

        tooltipComponents.add(
            Component.translatable("item.songcraft_instruments.looper_adapter.instrument.description")
                .withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(
            Component.translatable("item.songcraft_instruments.looper_adapter.looper.description")
                .withStyle(ChatFormatting.GRAY)
        );

        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
    
}
