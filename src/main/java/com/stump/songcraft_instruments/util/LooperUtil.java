package com.stump.songcraft_instruments.util;

import com.stump.songcraft_instruments.block.blockentity.looper.RecordingSession;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.block.partial.IDoubleBlock;
import com.stump.songcraft_instruments.capability.recording.RecordingCapabilityProvider;
import com.stump.songcraft_instruments.item.emirecord.EMIRecordItem;
import com.stump.songcraft_instruments.event.InstrumentPlayedEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import com.stump.songcraft_instruments.networking.packet.SyncModTagPacket;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Map.entry;

public class LooperUtil {
    public static final String LOOPER_TAG = "looper", POS_TAG = "pos", CONNECTIONS_TAG = "connections";
    

    // Handle instrument's looper tag
    public static boolean hasLooperTag(final ItemStack instrument) {
        return hasLooperTag(SCInstrumentMod.modTag(instrument));
    }
    public static boolean hasLooperTag(final BlockEntity instrument) {
        return hasLooperTag(SCInstrumentMod.modTag(instrument));
    }
    private static boolean hasLooperTag(final CompoundTag modTag) {
        return modTag.contains(LOOPER_TAG, CompoundTag.TAG_COMPOUND) && !modTag.getCompound(LOOPER_TAG).isEmpty();
    }

    public static void remLooperTag(final ItemStack instrument) {
        SCInstrumentMod.modTag(instrument).remove(LOOPER_TAG);
    }
    public static void remLooperTag(final BlockEntity instrument) {
        SCInstrumentMod.modTag(instrument).remove(LOOPER_TAG);
    }

    /**
     * Connects an item instrument; it belongs to a single player, and so holds a single connection.
     */
    public static void createLooperTag(final ItemStack instrument, final BlockPos looperPos, final UUID connectionId) {
        SCInstrumentMod.modTag(instrument).put(LOOPER_TAG, new CompoundTag());
        constructLooperTag(looperTag(instrument), looperPos, List.of(connectionId));
    }
    /**
     * Adds a connection to a block instrument. Block instruments are shared, so multiple players may connect them
     * to the same looper. Connecting to a different looper replaces all connections to the previous one.
     */
    public static void createLooperTag(final BlockEntity instrument, final BlockPos looperPos, final UUID connectionId) {
        final CompoundTag oldTag = looperTag(instrument);
        final Level level = instrument.getLevel();
        final List<UUID> connectionIds = new ArrayList<>();

        if (!oldTag.isEmpty() && (level != null)) {
            if (looperPos.equals(getLooperPos(oldTag))) {
                // Keep the connections the looper still holds; e.g. not the connecting player's previous one
                final LooperBlockEntity lbe = getFromPos(level, looperPos);
                if (lbe != null)
                    getConnectionIds(oldTag).stream().filter(lbe.connections()::has).forEach(connectionIds::add);
            } else {
                removeFromLooper(level, oldTag);
            }
        }
        connectionIds.add(connectionId);

        SCInstrumentMod.modTag(instrument).put(LOOPER_TAG, new CompoundTag());
        constructLooperTag(looperTag(instrument), looperPos, connectionIds);
    }
    private static void constructLooperTag(final CompoundTag looperTag, final BlockPos looperPos, final List<UUID> connectionIds) {
        looperTag.put(POS_TAG, NbtUtils.writeBlockPos(looperPos));

        final ListTag connections = new ListTag();
        connectionIds.forEach((connectionId) -> connections.add(NbtUtils.createUUID(connectionId)));
        looperTag.put(CONNECTIONS_TAG, connections);
    }

    /**
     * @return The IDs of the connections made with this instrument; one for each player that connected it
     */
    public static List<UUID> getConnectionIds(final CompoundTag looperTag) {
        return looperTag.getList(CONNECTIONS_TAG, Tag.TAG_INT_ARRAY).stream()
            .map(NbtUtils::loadUUID)
            .toList();
    }

    public static CompoundTag looperTag(final ItemStack instrument) {
        return looperTag(SCInstrumentMod.modTag(instrument));
    }
    public static CompoundTag looperTag(final BlockEntity instrument) {
        return looperTag(SCInstrumentMod.modTag(instrument));
    }
    public static CompoundTag looperTag(final CompoundTag parentTag) {
        return parentTag.contains(LOOPER_TAG, CompoundTag.TAG_COMPOUND)
            ? parentTag.getCompound(LOOPER_TAG)
            : new CompoundTag();
    }


    public static CompoundTag getLooperTagFromEvent(final InstrumentPlayedEvent<?> event) {
        if (!event.isByPlayer())
            return new CompoundTag();

        final InstrumentPlayedEvent<?>.EntityInfo entityInfo = event.entityInfo().get();
        final Player player = (Player) entityInfo.entity;

        return (!entityInfo.isBlockInstrument())
            ? looperTag(player.getItemInHand(entityInfo.hand.get()))
            : looperTag(event.level().getBlockEntity(event.soundMeta().pos()));
    }

    @Nullable
    public static LooperBlockEntity getFromEvent(final InstrumentPlayedEvent<?> event) {
        if (!event.isByPlayer())
            return null;

        final InstrumentPlayedEvent<?>.EntityInfo entityInfo = event.entityInfo().get();
        final Player player = (Player) entityInfo.entity;
        final Level level = event.level();

        if (entityInfo.isItemInstrument())
            return getFromItemInstrument(level, player.getItemInHand(entityInfo.hand.get()));
        else if (entityInfo.isBlockInstrument())
            return getFromBlockInstrument(level, level.getBlockEntity(event.soundMeta().pos()));

        return null;
    }

    @Nullable
    public static LooperBlockEntity getFromItemInstrument(final Level level, final ItemStack instrument) {
        return getFromInstrument(level, LooperUtil.looperTag(instrument), () -> LooperUtil.remLooperTag(instrument));
    }
    @Nullable
    public static LooperBlockEntity getFromBlockInstrument(final Level level, final BlockEntity instrument) {
        return getFromInstrument(level, LooperUtil.looperTag(instrument), () -> {
            LooperUtil.remLooperTag(instrument);

            final BlockPos pos = instrument.getBlockPos();
            final BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof IDoubleBlock doubleBlock)
                LooperUtil.remLooperTag(level.getBlockEntity(doubleBlock.getOtherBlock(state, pos, level)));
        });
    }
    /**
     * Attempts to get the looper pointed out by {@code looperData}. Removes its reference if not found,
     * or if the instrument's connection is no longer held by the looper.
     * @return The Looper's block entity as pointed in the {@code instrument}'s data.
     * Null if not found
     */
    @Nullable
    private static LooperBlockEntity getFromInstrument(Level level, CompoundTag looperData, Runnable onInvalid) {
        if (looperData.isEmpty())
            return null;

        final LooperBlockEntity looperBE = getFromPos(level, LooperUtil.getLooperPos(looperData));

        if (looperBE == null || getConnectionIds(looperData).stream().noneMatch(looperBE.connections()::has)) {
            onInvalid.run();
            return null;
        }

        return looperBE;
    }

    /**
     * @return Whether the instrument holding {@code looperTag} is the player's current connection to the looper.
     * False for players using a block instrument only other players connected.
     */
    public static boolean isConnectedBy(final LooperBlockEntity lbe, final CompoundTag looperTag, final Player player) {
        return getConnectionIds(looperTag).stream()
            .anyMatch((connectionId) -> lbe.connections().isConnectedBy(player, connectionId));
    }

    /**
     * Disconnects a block instrument from its looper on both ends, and syncs the change to nearby clients.
     */
    public static void disconnectBlockInstrument(final Level level, final BlockEntity instrument) {
        final CompoundTag looperTag = looperTag(instrument);
        if (looperTag.isEmpty())
            return;

        removeFromLooper(level, looperTag);

        remLooperTag(instrument);
        instrument.setChanged();

        if (level instanceof ServerLevel serverLevel) {
            SCPacketHandler.sendToTracking(
                new SyncModTagPacket(SCInstrumentMod.modTag(instrument), instrument.getBlockPos()),
                serverLevel, instrument.getBlockPos()
            );
        }
    }

    /**
     * Removes the connections made with an instrument from its looper's end.
     */
    private static void removeFromLooper(final Level level, final CompoundTag looperTag) {
        final BlockPos looperPos = getLooperPos(looperTag);
        if (looperPos == null || !level.isLoaded(looperPos))
            return;

        final LooperBlockEntity lbe = getFromPos(level, looperPos);
        // Group participants stay in the session until it ends, recording on any instrument they play
        if (lbe != null && !lbe.session().isGroupSession())
            getConnectionIds(looperTag).forEach(lbe.connections()::removeByConnectionId);
    }

    public static LooperBlockEntity getFromPos(final Level level, final BlockPos pos) {
        return (level.getBlockEntity(pos) instanceof LooperBlockEntity lbe) ? lbe : null;
    }


    /**
     * Connects the player to the looper, and passes the new connection ID to {@code pairPerformer}
     * to be stored within the connected instrument.
     */
    public static boolean performPair(LooperBlockEntity lbe, Consumer<UUID> pairPerformer, Player pairingPlayer) {
        if (!validateFootagePresence(lbe, pairingPlayer))
            return false;

        if (!lbe.session().canAcceptConnections()) {
            pairingPlayer.displayClientMessage(
                Component.translatable("songcraft_instruments.looper.connections_locked").withStyle(ChatFormatting.RED)
            , true);
            return false;
        }

        pairPerformer.accept(connect(lbe, pairingPlayer));

        pairingPlayer.displayClientMessage(
            Component.translatable("item.songcraft_instruments.looper_adapter.instrument.success_pair").withStyle(ChatFormatting.GREEN)
        , true);

        return true;
    }
    /**
     * Connects the player to the looper, disconnecting their previous instrument (from this or any other looper).
     * @return The new connection's ID
     */
    private static UUID connect(final LooperBlockEntity lbe, final Player player) {
        final UUID oldConnectionId = RecordingCapabilityProvider.getConnectionId(player);
        final BlockPos oldLooperPos = RecordingCapabilityProvider.getConnectedLooperPos(player);

        // Unloaded loopers will prune the stale connection themselves when accessed
        if (oldConnectionId != null && oldLooperPos != null && player.level().isLoaded(oldLooperPos)) {
            final LooperBlockEntity oldLooper = getFromPos(player.level(), oldLooperPos);
            if (oldLooper != null)
                oldLooper.connections().remove(player.getUUID(), oldConnectionId);
        }

        final UUID connectionId = UUID.randomUUID();
        // Must be set before adding the connection, as the looper prunes connections
        // that don't match their player's current one
        RecordingCapabilityProvider.setConnection(player, lbe.getBlockPos(), connectionId);
        lbe.connections().add(player, connectionId);

        return connectionId;
    }

    public static boolean validateFootagePresence(final LooperBlockEntity lbe, final Player pairingPlayer) {
        if (!lbe.hasFootage())
            return true;

        pairingPlayer.displayClientMessage(
            Component.translatable("songcraft_instruments.looper.pair_conflict").withStyle(ChatFormatting.GREEN)
        , true);

        return false;
    }


    /**
     * @param pos The position of the block to check for
     * @return Whether {@code looperTag} contains any position, and if it's equal to {@code pos}
     */
    public static boolean isSameBlock(final CompoundTag looperTag, final BlockPos pos) {
        try {
            return getLooperPos(looperTag).equals(pos);
        } catch (NullPointerException e) {
            return false;
        }
    }

    @Nullable
    public static BlockPos getLooperPos(final CompoundTag looperTag) {
        final CompoundTag looperPosTag = looperTag.getCompound(POS_TAG);
        return (looperPosTag == null) ? null : NbtUtils.readBlockPos(looperPosTag);
    }

    public static void setRecording(final Player player, final BlockPos looperPos) {
        RecordingCapabilityProvider.setRecording(player, looperPos);
    }
    public static void setNotRecording(final Player player) {
        RecordingCapabilityProvider.setNotRecording(player);
    }
    public static boolean isRecording(final Player player) {
        return RecordingCapabilityProvider.isRecording(player);
    }


    //#region Legacy Looper Migration

    /**
     * Maps the old keys to the new ones
     */
    private static final Map<String, String> LOOPER_LEGACY_MAPPER = Map.ofEntries(
        // Record
        entry("instrumentId", EMIRecordItem.INSTRUMENT_ID_TAG),
        entry("notes", EMIRecordItem.NOTES_TAG),
        entry("volume", EMIRecordItem.VOLUME_TAG),
        entry("pitch", EMIRecordItem.PITCH_TAG),
        entry("soundIndex", EMIRecordItem.SOUND_INDEX_TAG),
        entry("soundType", EMIRecordItem.SOUND_TYPE_TAG),
        entry("timestamp", EMIRecordItem.TIMESTAMP_TAG),
        // Looper
        entry("recording", RecordingSession.RECORDING_TAG),
        entry("ticks", LooperBlockEntity.TICKS_TAG),
        // Looper -> Record
        entry("channel", EMIRecordItem.CHANNEL_TAG),
        entry("repeatTick", EMIRecordItem.REPEAT_TICK_TAG)
    );

    /**
     * Migrates all keys of a looper, if it is a legacy one.
     * @return A new record channel compound data containing the old looper's data.
     * To be burned into a record.
     */
    public static Optional<CompoundTag> migrateLegacyLooper(final LooperBlockEntity lbe) {
        final CompoundTag lbed = lbe.getPersistentData();

        if (!lbed.contains("channel", Tag.TAG_COMPOUND))
            return Optional.empty();

        final CompoundTag looperData = CommonUtil.deepConvertCompound(lbed, LOOPER_LEGACY_MAPPER);
        final CompoundTag channel = looperData.getCompound(EMIRecordItem.CHANNEL_TAG);
        // Writable is a new tag. This record will be burned:
        looperData.putBoolean(EMIRecordItem.WRITABLE_TAG, false);
        // RepeatTick moved from looper to channel
        CommonUtil.moveTags(looperData, channel, EMIRecordItem.REPEAT_TICK_TAG);

        // Remove all old looper tags
        lbed.getAllKeys()
            .stream().toList() // Convert to list as to not mess with the internal map
            .forEach(lbed::remove);

        // Add everything back except for the channel; which belongs to a record
        looperData.getAllKeys().stream()
            .filter((key) -> !key.equals(EMIRecordItem.CHANNEL_TAG))
            .forEach((key) -> lbed.put(key, looperData.get(key)));

        return Optional.of(channel);
    }

    public static BlockPos getRecordingLooperPos(final Player player) {
        return RecordingCapabilityProvider.getLooperPos(player);
    }
    
}
