package com.stump.songcraft_instruments.item;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.LooperBlock;
import com.stump.songcraft_instruments.block.PedalBlock;
import com.stump.songcraft_instruments.block.SpeakerBlock;
import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.PedalBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.SpeakerBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.speaker.Speaker;
import com.stump.songcraft_instruments.block.blockentity.speaker.SpeakerAddress;
import com.stump.songcraft_instruments.block.partial.AbstractInstrumentBlock;
import com.stump.songcraft_instruments.block.partial.InstrumentBlockEntity;
import com.stump.songcraft_instruments.pedal.InstrumentPedals;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.pedal.PedalLinks;
import com.stump.songcraft_instruments.pedal.PedalMonitorSync;
import com.stump.songcraft_instruments.util.CommonUtil;
import com.stump.songcraft_instruments.util.SpeakerUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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

/**
 * pairs sources (block instruments and loopers) to speakers. right-click a source and a speaker
 * (in either order) to pair them. a speaker accepts at most one looper.
 * shift + right-click a source to unpair all of its speakers,
 * or only the speaker selected with this cable beforehand.
 * held instruments don't need a cable: they pair by right-clicking a speaker directly (see SpeakerBlock).
 * speakers carried by boats and minecarts work the same way, see {@link #useOnSpeaker}.
 *
 * also chains effects pedals: right-click a pedal, then the next pedal, to plug it in (see {@link PedalLinks}).
 * right-click an instrument block and a pedal (in either order) to plug the instrument into the pedal
 * (see {@link InstrumentPedals}). shift + right-click a pedal to unplug all of its cables,
 * or only the instrument block selected with this cable beforehand.
 */
public class SpeakerCableItem extends Item {
    private static final String CABLE_TAG = "speakerCable",
        SOURCE_POS_TAG = "source",
        // The selected speaker's address
        SPEAKER_TAG = "speaker",
        // The selected pedal's position
        PEDAL_TAG = "pedal";

    public SpeakerCableItem(Properties pProperties) {
        super(pProperties);
    }


    /**
     * Runs before the clicked block's own right-click action (the looper toggling playback,
     * an instrument block opening its screen), so the cable works with or without sneaking.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext pContext) {
        final Level level = pContext.getLevel();
        final BlockPos clickedPos = pContext.getClickedPos();
        final Block block = level.getBlockState(clickedPos).getBlock();

        final Player player = pContext.getPlayer();
        if (player == null)
            return InteractionResult.PASS;

        final boolean isCableTarget = block instanceof AbstractInstrumentBlock
            || block instanceof LooperBlock
            || block instanceof SpeakerBlock
            || block instanceof PedalBlock;
        if (!isCableTarget)
            return InteractionResult.PASS;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        final CompoundTag cableTag = CommonUtil.getOrCreateElementTag(
                SCInstrumentMod.modTag(pContext.getItemInHand()), CABLE_TAG);

        final BlockPos sourcePos;
        final String selectMessage;
        if (block instanceof AbstractInstrumentBlock instrumentBlock) {
            sourcePos = instrumentBlock.getInstrumentPos(level, clickedPos);
            selectMessage = "item.songcraft_instruments.speaker_cable.instrument.select";
        } else if (block instanceof LooperBlock) {
            sourcePos = clickedPos;
            selectMessage = "item.songcraft_instruments.speaker_cable.looper.select";
        } else if (block instanceof PedalBlock) {
            return handlePedal(clickedPos, cableTag, player) ? InteractionResult.SUCCESS : InteractionResult.CONSUME_PARTIAL;
        } else if (block instanceof SpeakerBlock) {
            if (!(level.getBlockEntity(clickedPos) instanceof SpeakerBlockEntity sbe))
                return InteractionResult.CONSUME_PARTIAL;
            return useOnSpeaker(pContext.getItemInHand(), player, sbe.speaker());
        } else {
            return InteractionResult.FAIL;
        }

        // A selected pedal has the instrument plugged into it (or unplugged)
        if (cableTag.contains(PEDAL_TAG, Tag.TAG_COMPOUND))
            return useSourceOnPedal(sourcePos, cableTag, player) ? InteractionResult.SUCCESS : InteractionResult.CONSUME_PARTIAL;

        final boolean succeed = player.isShiftKeyDown()
            ? unpairSpeakersFromSource(sourcePos, cableTag, player)
            : handleSourceBlock(sourcePos, cableTag, player, selectMessage);

        return succeed ? InteractionResult.SUCCESS : InteractionResult.CONSUME_PARTIAL;
    }

    /**
     * right-click (or shift + right-click) on a speaker, be it a block or carried by a boat or minecart.
     * server only.
     */
    public static InteractionResult useOnSpeaker(ItemStack cable, Player player, Speaker speaker) {
        final CompoundTag cableTag = CommonUtil.getOrCreateElementTag(SCInstrumentMod.modTag(cable), CABLE_TAG);

        final boolean succeed = player.isShiftKeyDown()
            ? unpairFromSpeaker(speaker, cableTag, player)
            : handleSpeaker(speaker, cableTag, player);
        return succeed ? InteractionResult.SUCCESS : InteractionResult.CONSUME_PARTIAL;
    }

    private static boolean handleSourceBlock(BlockPos sourcePos, CompoundTag cableTag, Player player, String selectMessage) {
        final Speaker selectedSpeaker = getSelectedSpeaker(player.level(), cableTag);
        if (selectedSpeaker != null)
            return pairSpeakerToSource(cableTag, selectedSpeaker, sourcePos, player);

        clearCable(cableTag);
        cableTag.put(SOURCE_POS_TAG, NbtUtils.writeBlockPos(sourcePos));
        player.displayClientMessage(
            Component.translatable(selectMessage).withStyle(ChatFormatting.GREEN)
        , true);
        return true;
    }
    private static boolean handleSpeaker(Speaker speaker, CompoundTag cableTag, Player player) {
        if (cableTag.contains(PEDAL_TAG, Tag.TAG_COMPOUND)) {
            clearCable(cableTag);
            sendNotIntoSpeakerMessage(player);
            return false;
        }
        if (cableTag.contains(SOURCE_POS_TAG, Tag.TAG_COMPOUND))
            return pairSpeakerToSource(cableTag, speaker, NbtUtils.readBlockPos(cableTag.getCompound(SOURCE_POS_TAG)), player);

        clearCable(cableTag);
        final CompoundTag speakerTag = new CompoundTag();
        speaker.getAddress().write(speakerTag);
        cableTag.put(SPEAKER_TAG, speakerTag);
        player.displayClientMessage(
            Component.translatable("item.songcraft_instruments.speaker_cable.speaker.select").withStyle(ChatFormatting.GREEN)
        , true);
        return true;
    }


    /**
     * return the block instrument or looper at the given position, or null if there is neither
     */
    @Nullable
    private static BlockEntity getSource(Level level, BlockPos sourcePos) {
        final BlockEntity be = level.getBlockEntity(sourcePos);
        return (be instanceof InstrumentBlockEntity || be instanceof LooperBlockEntity) ? be : null;
    }

    /**
     * return the speaker selected with this cable, or null if there is none (or it isn't loaded)
     */
    @Nullable
    private static Speaker getSelectedSpeaker(Level level, CompoundTag cableTag) {
        final SpeakerAddress address = SpeakerAddress.read(cableTag.getCompound(SPEAKER_TAG));
        return (address == null) ? null : address.resolve(level);
    }

    private static boolean pairSpeakerToSource(CompoundTag cableTag, Speaker speaker, BlockPos sourcePos, Player player) {
        final Level level = player.level();
        clearCable(cableTag);

        final BlockEntity source = getSource(level, sourcePos);
        if (source == null)
            return false;

        // also mirrors the pairing onto the other half of linked blocks (like the Keyboard), and syncs it to clients
        final SpeakerUtil.PairResult result = SpeakerUtil.pair(level, source, speaker);
        SpeakerUtil.sendPairMessage(player, result, SpeakerUtil.speakerCount(level, source));

        return true;
    }

    /**
     * shift + right-click on a source. If a speaker was selected with this cable first,
     * only that speaker is unpaired, otherwise every speaker paired to the source is.
     */
    private static boolean unpairSpeakersFromSource(BlockPos sourcePos, CompoundTag cableTag, Player player) {
        final Level level = player.level();
        final BlockEntity source = getSource(level, sourcePos);
        if (source == null)
            return false;

        final Speaker selectedSpeaker = getSelectedSpeaker(level, cableTag);
        clearCable(cableTag);

        if (selectedSpeaker != null) {
            final boolean removed = SpeakerUtil.unpair(level, source, selectedSpeaker);
            SpeakerUtil.sendUnpairMessage(player, removed, SpeakerUtil.speakerCount(level, source), selectedSpeaker);
        } else {
            SpeakerUtil.sendUnpairAllMessage(player, SpeakerUtil.unpairAll(level, source));
        }

        return true;
    }

    /**
     * shift + right-click on a speaker. If a source (block instrument or looper) was selected with this cable first,
     * only that source is unpaired, otherwise everything paired to the speaker is (including held instruments).
     */
    private static boolean unpairFromSpeaker(Speaker speaker, CompoundTag cableTag, Player player) {
        final Level level = player.level();

        final BlockEntity selectedSource = cableTag.contains(SOURCE_POS_TAG, Tag.TAG_COMPOUND)
            ? getSource(level, NbtUtils.readBlockPos(cableTag.getCompound(SOURCE_POS_TAG)))
            : null;
        clearCable(cableTag);

        if (selectedSource != null) {
            final boolean removed = SpeakerUtil.unpair(level, selectedSource, speaker);
            SpeakerUtil.sendUnpairMessage(player, removed, SpeakerUtil.speakerCount(level, selectedSource), speaker);
        } else {
            SpeakerUtil.sendSpeakerUnpairAllMessage(player, SpeakerUtil.unpairAllFromSpeaker(level, speaker));
        }

        return true;
    }

    //#region Pedals

    /**
     * right-click (or shift + right-click) on a pedal. server only.
     * a pedal selected beforehand plugs into this one; a selected instrument block is plugged into this pedal.
     * shift + right-click unplugs the selected instrument block from this pedal, or every cable of the pedal.
     */
    private static boolean handlePedal(BlockPos pedalPos, CompoundTag cableTag, Player player) {
        final Level level = player.level();
        final PedalBlockEntity pedal = PedalLinks.pedalAt(level, pedalPos);
        if (pedal == null)
            return false;

        final BlockPos selectedSource = cableTag.contains(SOURCE_POS_TAG, Tag.TAG_COMPOUND)
            ? NbtUtils.readBlockPos(cableTag.getCompound(SOURCE_POS_TAG)) : null;

        if (player.isShiftKeyDown()) {
            clearCable(cableTag);
            if (selectedSource != null)
                return unplugSource(selectedSource, pedal, player);

            final boolean wasConnected = PedalLinks.isConnected(level, pedal);
            PedalLinks.disconnect(level, pedal);
            player.displayClientMessage(wasConnected
                ? Component.translatable("songcraft_instruments.pedal.disconnected").withStyle(ChatFormatting.GREEN)
                : Component.translatable("songcraft_instruments.pedal.not_connected").withStyle(ChatFormatting.YELLOW)
            , true);
            return true;
        }

        if (cableTag.contains(PEDAL_TAG, Tag.TAG_COMPOUND)) {
            final BlockPos selected = NbtUtils.readBlockPos(cableTag.getCompound(PEDAL_TAG));
            if (!selected.equals(pedalPos) && (PedalLinks.pedalAt(level, selected) != null)) {
                connectPedal(selected, pedalPos, cableTag, player);
                return true;
            }
        }
        if (selectedSource != null) {
            clearCable(cableTag);
            return plugSource(selectedSource, pedal, player);
        }
        if (cableTag.contains(SPEAKER_TAG, Tag.TAG_COMPOUND)) {
            clearCable(cableTag);
            sendNotIntoSpeakerMessage(player);
            return false;
        }

        clearCable(cableTag);
        cableTag.put(PEDAL_TAG, NbtUtils.writeBlockPos(pedalPos));
        player.displayClientMessage(
            Component.translatable("item.songcraft_instruments.speaker_cable.pedal.select").withStyle(ChatFormatting.GREEN)
        , true);
        return true;
    }

    /**
     * right-click (or shift + right-click) on an instrument block or looper while a pedal is selected:
     * plugs the instrument into the pedal, or unplugs it
     */
    private static boolean useSourceOnPedal(BlockPos sourcePos, CompoundTag cableTag, Player player) {
        final PedalBlockEntity pedal = PedalLinks.pedalAt(player.level(), NbtUtils.readBlockPos(cableTag.getCompound(PEDAL_TAG)));
        clearCable(cableTag);
        if (pedal == null)
            return false;

        return player.isShiftKeyDown()
            ? unplugSource(sourcePos, pedal, player)
            : plugSource(sourcePos, pedal, player);
    }

    private static boolean plugSource(BlockPos sourcePos, PedalBlockEntity pedal, Player player) {
        final Level level = player.level();
        final BlockEntity source = getSource(level, sourcePos);
        if (source == null)
            return false;
        // Loopers play back the pedals each note was recorded through
        if (source instanceof LooperBlockEntity) {
            player.displayClientMessage(
                Component.translatable("songcraft_instruments.pedal.looper").withStyle(ChatFormatting.RED)
            , true);
            return false;
        }

        final InstrumentPedals.PlugResult result = InstrumentPedals.plug(SCInstrumentMod.modTag(source), pedal);
        source.setChanged();
        InstrumentPedals.sendPlugMessage(player, result);
        PedalMonitorSync.onPedalsChanged(level);
        return true;
    }
    private static boolean unplugSource(BlockPos sourcePos, PedalBlockEntity pedal, Player player) {
        final Level level = player.level();
        final BlockEntity source = getSource(level, sourcePos);
        if (source == null)
            return false;

        final CompoundTag modTag = SCInstrumentMod.modTag(source);
        final boolean wasPlugged = InstrumentPedals.isPluggedInto(modTag, pedal);
        if (wasPlugged) {
            InstrumentPedals.unplug(modTag);
            source.setChanged();
            PedalMonitorSync.onPedalsChanged(level);
        }
        InstrumentPedals.sendUnplugMessage(player, wasPlugged);
        return true;
    }

    /**
     * plugs the pedal at {@code pedalPos} into the pedal at {@code targetPos}
     */
    private static void connectPedal(BlockPos pedalPos, BlockPos targetPos, CompoundTag cableTag, Player player) {
        clearCable(cableTag);

        final Component message = switch (PedalLinks.connect(player.level(), pedalPos, targetPos)) {
            case CONNECTED -> Component.translatable("songcraft_instruments.pedal.connected")
                .withStyle(ChatFormatting.GREEN);
            case ALREADY_CONNECTED -> Component.translatable("songcraft_instruments.pedal.already_connected")
                .withStyle(ChatFormatting.YELLOW);
            case LOOP -> Component.translatable("songcraft_instruments.pedal.loop")
                .withStyle(ChatFormatting.RED);
            case TOO_LONG -> Component.translatable("songcraft_instruments.pedal.too_long", PedalChain.MAX_LENGTH)
                .withStyle(ChatFormatting.RED);
            case SAME_BLOCK, NOT_A_PEDAL -> Component.translatable("songcraft_instruments.pedal.not_a_target")
                .withStyle(ChatFormatting.RED);
        };
        player.displayClientMessage(message, true);
    }

    private static void sendNotIntoSpeakerMessage(Player player) {
        player.displayClientMessage(
            Component.translatable("songcraft_instruments.pedal.speaker").withStyle(ChatFormatting.RED)
        , true);
    }

    //#endregion

    private static void clearCable(CompoundTag cableTag) {
        for (final String key : List.copyOf(cableTag.getAllKeys()))
            cableTag.remove(key);
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
            Component.translatable("item.songcraft_instruments.speaker_cable.pair.description")
                .withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(
            Component.translatable("item.songcraft_instruments.speaker_cable.unpair.description")
                .withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(
            Component.translatable("item.songcraft_instruments.speaker_cable.held.description")
                .withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(
            Component.translatable("item.songcraft_instruments.speaker_cable.pedal.description")
                .withStyle(ChatFormatting.GRAY)
        );

        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }

}
