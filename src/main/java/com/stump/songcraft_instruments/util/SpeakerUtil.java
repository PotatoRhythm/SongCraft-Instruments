package com.stump.songcraft_instruments.util;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.speaker.Speaker;
import com.stump.songcraft_instruments.block.blockentity.speaker.SpeakerAddress;
import com.stump.songcraft_instruments.block.partial.InstrumentBlockEntity;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.event.InstrumentPlayedEvent;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.SyncModTagPacket;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * handles pairing an instrument to up to MAX_SPEAKERS speakers and resolving
 * those pairings when the instrument is played.
 *
 * a pairing is recorded on both sides: the instrument stores each speaker's address and ID,
 * and the speaker stores the instrument's ID. a pairing only counts while both sides agree,
 * so a broken speaker (or a new speaker placed where an old one was) is dropped from
 * the instrument's list the next time that list is read.
 * a speaker's address is its block's position, or the UUID of the boat or minecart carrying it (see SpeakerAddress).
 *
 * loopers pair the same way as block instruments (the "instrument" data lives on the looper),
 * except that a speaker accepts at most one looper. the looper relays its own playback to its speakers.
 *
 * instrument tag layout: speaker: { instrument_id: UUID, speakers: [ { pos: {X,Y,Z} | entity: UUID, id: UUID }, ... ] }
 */
public class SpeakerUtil {
    public static final String SPEAKER_TAG = "speaker", SPEAKERS_TAG = "speakers",
        INSTRUMENT_ID_TAG = "instrument_id", ID_TAG = "id";
    public static final int MAX_SPEAKERS = 8;

    public enum PairResult { PAIRED, ALREADY_PAIRED, FULL, LOOPER_TAKEN }

    private record SpeakerEntry(SpeakerAddress address, UUID speakerId) {
        static SpeakerEntry of(final Speaker speaker) {
            return new SpeakerEntry(speaker.getAddress(), speaker.getSpeakerId());
        }
    }


    //#region Pairing

    public static PairResult pair(final Level level, final ItemStack instrument, final Speaker speaker) {
        return pair(level, SCInstrumentMod.modTag(instrument), speaker, null, false);
    }
    /**
     * pairs a block source: a block instrument
     * or a looper. a speaker accepts at most one looper.
     */
    public static PairResult pair(final Level level, final BlockEntity source, final Speaker speaker) {
        final boolean isLooper = source instanceof LooperBlockEntity;
        final PairResult result = pair(level, SCInstrumentMod.modTag(source), speaker, source.getBlockPos(), isLooper);
        onBlockInstrumentChanged(level, source);
        return result;
    }
    private static PairResult pair(final Level level, final CompoundTag modTag, final Speaker speaker,
                                   @Nullable final BlockPos instrumentPos, final boolean isLooper) {
        final List<SpeakerEntry> entries = getValidEntries(level, modTag);
        final UUID instrumentId = getOrCreateInstrumentId(modTag);

        final SpeakerEntry newEntry = SpeakerEntry.of(speaker);
        if (entries.contains(newEntry)) {
            writeEntries(modTag, entries);
            return PairResult.ALREADY_PAIRED;
        }
        if (entries.size() >= MAX_SPEAKERS) {
            writeEntries(modTag, entries);
            return PairResult.FULL;
        }
        if (isLooper && speaker.hasOtherLooper(level, instrumentId)) {
            writeEntries(modTag, entries);
            return PairResult.LOOPER_TAKEN;
        }

        entries.add(newEntry);
        writeEntries(modTag, entries);
        speaker.addInstrument(instrumentId, instrumentPos, isLooper);
        return PairResult.PAIRED;
    }

    /**
     * return whether the speaker was paired to the instrument
     */
    public static boolean unpair(final Level level, final ItemStack instrument, final Speaker speaker) {
        return unpair(level, SCInstrumentMod.modTag(instrument), speaker);
    }
    public static boolean unpair(final Level level, final BlockEntity instrument, final Speaker speaker) {
        final boolean removed = unpair(level, SCInstrumentMod.modTag(instrument), speaker);
        onBlockInstrumentChanged(level, instrument);
        return removed;
    }
    private static boolean unpair(final Level level, final CompoundTag modTag, final Speaker speaker) {
        final UUID instrumentId = getInstrumentId(modTag);
        final List<SpeakerEntry> entries = getValidEntries(level, modTag);

        final boolean removed = entries.remove(SpeakerEntry.of(speaker));
        writeEntries(modTag, entries);

        if (instrumentId != null)
            speaker.removeInstrument(instrumentId);
        return removed;
    }

    /**
     * unpairs every speaker from a block instrument or a looper.
     * speakers in unloaded chunks drop the instrument on their own once loaded,
     * since the instrument no longer lists them.
     * return how many speakers were unpaired
     */
    public static UnpairAllResult unpairAll(final Level level, final BlockEntity instrument) {
        final CompoundTag modTag = SCInstrumentMod.modTag(instrument);
        final UUID instrumentId = getInstrumentId(modTag);
        final List<SpeakerEntry> entries = getValidEntries(level, modTag);

        int stillConnected = 0;
        if (instrumentId != null) {
            for (final SpeakerEntry entry : entries) {
                final Speaker speaker = entry.address().resolve(level);
                if (speaker != null) {
                    speaker.removeInstrument(instrumentId);
                    if (speaker.isConnected())
                        stillConnected++;
                }
            }
        }

        modTag.remove(SPEAKER_TAG);
        onBlockInstrumentChanged(level, instrument);
        return new UnpairAllResult(entries.size(), stillConnected);
    }
    /**
     * @param removed How many speakers were unpaired
     * @param stillConnected How many of them are still connected to another instrument or looper
     */
    public record UnpairAllResult(int removed, int stillConnected) {}

    /**
     * unpairs everything from a speaker's end: its block instruments, looper and held instruments.
     * held instruments in online players' inventories are updated right away; any others
     * drop the speaker the next time they're used, since the speaker no longer lists them.
     * return how many sources were unpaired
     */
    public static int unpairAllFromSpeaker(final Level level, final Speaker speaker) {
        final Map<UUID, BlockPos> sources = speaker.getPairedSources();

        sources.forEach((sourceId, sourcePos) -> {
            if (sourcePos == null) {
                removeFromHeldInstruments(level, sourceId, speaker);
            } else if (level.isLoaded(sourcePos)) {
                final BlockEntity source = level.getBlockEntity(sourcePos);
                if (source != null)
                    unpair(level, source, speaker);
            }

            // Also covers sources that couldn't be reached (unloaded, or held by an offline player)
            speaker.removeInstrument(sourceId);
        });

        return sources.size();
    }
    private static void removeFromHeldInstruments(final Level level, final UUID instrumentId, final Speaker speaker) {
        final SpeakerEntry speakerEntry = SpeakerEntry.of(speaker);

        for (final Player player : level.players()) {
            final Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                final CompoundTag modTag = inventory.getItem(i).getTagElement(SCInstrumentMod.MODID);
                if ((modTag == null) || !instrumentId.equals(getInstrumentId(modTag)))
                    continue;

                final List<SpeakerEntry> entries = readEntries(modTag);
                if (entries.remove(speakerEntry))
                    writeEntries(modTag, entries);
            }
        }
    }

    /**
     * right-clicking a speaker (block or vehicle) with a held instrument:
     * right-click pairs the instrument to the speaker, shift + right-click unpairs it.
     */
    public static void useHeldInstrument(final Level level, final Player player, final ItemStack heldStack, final Speaker speaker) {
        if (player.isShiftKeyDown()) {
            final boolean removed = unpair(level, heldStack, speaker);
            sendUnpairMessage(player, removed, speakerCount(level, heldStack), speaker);
        } else {
            final PairResult result = pair(level, heldStack, speaker);
            sendPairMessage(player, result, speakerCount(level, heldStack));
        }
    }

    /**
     * return how many speakers are currently paired to the instrument. drops stale pairings.
     */
    public static int speakerCount(final Level level, final ItemStack instrument) {
        return getSpeakers(level, SCInstrumentMod.modTag(instrument)).size();
    }
    public static int speakerCount(final Level level, final BlockEntity instrument) {
        final int count = getSpeakers(level, SCInstrumentMod.modTag(instrument)).size();
        onBlockInstrumentChanged(level, instrument);
        return count;
    }

    /**
     * return whether the block instrument or looper at the given position is still paired to the given speaker.
     * used by speakers to verify their own list of block sources.
     */
    public static boolean isBlockInstrumentPaired(final Level level, final BlockPos instrumentPos,
                                                  final UUID instrumentId, final Speaker speaker) {
        final BlockEntity be = level.getBlockEntity(instrumentPos);
        if (be == null)
            return false;

        return listsSpeaker(SCInstrumentMod.modTag(be), instrumentId, speaker);
    }

    /**
     * return whether a held instrument with the given ID, still listing the given speaker,
     * is in an online player's inventory. used for the speaker's connected light, since a
     * speaker can't otherwise tell whether a held instrument still exists.
     */
    public static boolean isHeldInstrumentPresent(final Level level, final UUID instrumentId, final Speaker speaker) {
        for (final Player player : level.players()) {
            final Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                final CompoundTag modTag = inventory.getItem(i).getTagElement(SCInstrumentMod.MODID);
                if ((modTag != null) && listsSpeaker(modTag, instrumentId, speaker))
                    return true;
            }
        }
        return false;
    }

    private static boolean listsSpeaker(final CompoundTag modTag, final UUID instrumentId, final Speaker speaker) {
        return instrumentId.equals(getInstrumentId(modTag))
            && readEntries(modTag).contains(SpeakerEntry.of(speaker));
    }

    //#endregion


    //#region Resolving on play

    /**
     * return every speaker paired to the instrument that produced the event.
     * stale pairings are dropped along the way.
     */
    public static List<Speaker> getFromEvent(final InstrumentPlayedEvent<?> event) {
        if (!event.isByPlayer())
            return List.of();

        final InstrumentPlayedEvent<?>.EntityInfo entityInfo = event.entityInfo().get();
        final Player player = (Player) entityInfo.entity;
        final Level level = event.level();

        if (entityInfo.isItemInstrument())
            return getSpeakers(level, SCInstrumentMod.modTag(player.getItemInHand(entityInfo.hand.get())));

        // Prefer the block instrument the player has open: some notes (e.g. the releases sent when
        // closing the instrument) carry the player's position rather than the instrument's
        final BlockPos openInstrumentPos = InstrumentOpenProvider.isOpen(player) ? InstrumentOpenProvider.getBlockPos(player) : null;
        if ((openInstrumentPos != null) && (level.getBlockEntity(openInstrumentPos) instanceof InstrumentBlockEntity ibe))
            return getFromBlock(level, ibe);

        if (entityInfo.isBlockInstrument())
            return getFromBlock(level, level.getBlockEntity(event.soundMeta().pos()));

        return List.of();
    }

    /**
     * return every speaker paired to the instrument the player has open, held or block
     */
    public static List<Speaker> getFromOpenInstrument(final Player player) {
        final Level level = player.level();
        if (!InstrumentOpenProvider.isOpen(player))
            return List.of();

        if (InstrumentOpenProvider.isItem(player)) {
            final InteractionHand hand = InstrumentOpenProvider.getHand(player);
            return (hand == null) ? List.of() : getSpeakers(level, SCInstrumentMod.modTag(player.getItemInHand(hand)));
        }

        final BlockPos instrumentPos = InstrumentOpenProvider.getBlockPos(player);
        final BlockEntity instrument = (instrumentPos == null) ? null : level.getBlockEntity(instrumentPos);
        return (instrument == null) ? List.of() : getFromBlock(level, instrument);
    }
    /**
     * releases every held note the player is sustaining through the speakers of their open instrument.
     * used when their notes stop all at once without individual releases reaching the server, e.g. on dampen.
     */
    public static void releaseHeldNotes(final Player player) {
        final InitiatorID initiatorID = InitiatorID.fromEntity(player);
        getFromOpenInstrument(player).forEach((speaker) -> speaker.releaseHeldNotesFrom(initiatorID));
    }

    /**
     * return every speaker paired to a block source: a block instrument or a looper.
     * stale pairings are dropped along the way.
     */
    public static List<Speaker> getFromBlock(final Level level, final BlockEntity source) {
        final CompoundTag modTag = SCInstrumentMod.modTag(source);
        if (pruneStaleEntries(level, modTag))
            onBlockInstrumentChanged(level, source);
        return resolveSpeakers(level, modTag);
    }

    /**
     * drops stale pairings from the instrument's data,
     * return the loaded speakers that are still paired.
     */
    private static List<Speaker> getSpeakers(final Level level, final CompoundTag modTag) {
        pruneStaleEntries(level, modTag);
        return resolveSpeakers(level, modTag);
    }
    /**
     * only rewrites the data when something was actually dropped,
     * so playing a note doesn't touch the instrument's NBT every time
     * return whether any pairing was dropped
     */
    private static boolean pruneStaleEntries(final Level level, final CompoundTag modTag) {
        final List<SpeakerEntry> valid = getValidEntries(level, modTag);
        if (valid.size() == readEntries(modTag).size())
            return false;

        writeEntries(modTag, valid);
        return true;
    }
    private static List<Speaker> resolveSpeakers(final Level level, final CompoundTag modTag) {
        final List<Speaker> speakers = new ArrayList<>();
        for (final SpeakerEntry entry : readEntries(modTag)) {
            final Speaker speaker = entry.address().resolve(level);
            if (speaker != null)
                speakers.add(speaker);
        }
        return speakers;
    }

    //#endregion


    //#region Instrument data

    /**
     * return the instrument's entries that are still valid pairings.
     * an entry is valid when its address holds the same speaker (by ID), and that speaker
     * still lists this instrument. entries that aren't loaded can't be checked, so they are kept,
     * unless the vehicle carrying the speaker is known to be destroyed.
     */
    private static List<SpeakerEntry> getValidEntries(final Level level, final CompoundTag modTag) {
        final UUID instrumentId = getInstrumentId(modTag);
        final List<SpeakerEntry> valid = new ArrayList<>();
        if (instrumentId == null)
            return valid;

        for (final SpeakerEntry entry : readEntries(modTag)) {
            if (!entry.address().isLoaded(level)) {
                if (entry.address().mayStillExist(level, entry.speakerId()))
                    valid.add(entry);
                continue;
            }

            final Speaker speaker = entry.address().resolve(level);
            if ((speaker != null)
                && speaker.getSpeakerId().equals(entry.speakerId())
                && speaker.hasInstrument(instrumentId))
                valid.add(entry);
        }

        return valid;
    }

    private static List<SpeakerEntry> readEntries(final CompoundTag modTag) {
        final List<SpeakerEntry> entries = new ArrayList<>();
        final CompoundTag speakerTag = modTag.getCompound(SPEAKER_TAG);

        final ListTag list = speakerTag.getList(SPEAKERS_TAG, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entryTag = list.getCompound(i);
            final SpeakerAddress address = SpeakerAddress.read(entryTag);
            if (entryTag.hasUUID(ID_TAG) && (address != null))
                entries.add(new SpeakerEntry(address, entryTag.getUUID(ID_TAG)));
        }

        return entries;
    }
    private static void writeEntries(final CompoundTag modTag, final List<SpeakerEntry> entries) {
        if (entries.isEmpty()) {
            modTag.remove(SPEAKER_TAG);
            return;
        }

        final ListTag list = new ListTag();
        for (final SpeakerEntry entry : entries) {
            final CompoundTag entryTag = new CompoundTag();
            entry.address().write(entryTag);
            entryTag.putUUID(ID_TAG, entry.speakerId());
            list.add(entryTag);
        }

        CommonUtil.getOrCreateElementTag(modTag, SPEAKER_TAG).put(SPEAKERS_TAG, list);
    }

    @Nullable
    private static UUID getInstrumentId(final CompoundTag modTag) {
        final CompoundTag speakerTag = modTag.getCompound(SPEAKER_TAG);
        return speakerTag.hasUUID(INSTRUMENT_ID_TAG) ? speakerTag.getUUID(INSTRUMENT_ID_TAG) : null;
    }
    private static UUID getOrCreateInstrumentId(final CompoundTag modTag) {
        final UUID existing = getInstrumentId(modTag);
        if (existing != null)
            return existing;

        final UUID id = UUID.randomUUID();
        CommonUtil.getOrCreateElementTag(modTag, SPEAKER_TAG).putUUID(INSTRUMENT_ID_TAG, id);
        return id;
    }

    /**
     * marks a block instrument as changed,
     * and syncs it to nearby clients so the instrument screen's speaker counter stays accurate.
     */
    private static void onBlockInstrumentChanged(final Level level, final BlockEntity instrument) {
        instrument.setChanged();
        syncToClients(level, instrument);
    }
    private static void syncToClients(final Level level, final BlockEntity instrument) {
        // Loopers don't show the counter, so only block instruments need syncing
        if (!(level instanceof ServerLevel serverLevel) || !(instrument instanceof InstrumentBlockEntity))
            return;

        SCPacketHandler.sendToTracking(
            new SyncModTagPacket(SCInstrumentMod.modTag(instrument), instrument.getBlockPos()),
            serverLevel, instrument.getBlockPos()
        );
    }

    /**
     * return how many speakers the instrument's data lists, without checking them.
     * safe to call on the client, e.g. for the instrument screen's speaker counter.
     */
    public static int getListedSpeakerCount(final CompoundTag modTag) {
        return readEntries(modTag).size();
    }

    //#endregion


    /**
     * shows the player the outcome of a pairing attempt, e.g. "1 of 8 speakers connected!"
     */
    public static void sendPairMessage(final Player player, final PairResult result, final int count) {
        final Component message = switch (result) {
            case PAIRED -> Component.translatable("songcraft_instruments.speaker.paired", count, MAX_SPEAKERS)
                .withStyle(ChatFormatting.GREEN);
            case ALREADY_PAIRED -> Component.translatable("songcraft_instruments.speaker.already_paired", count, MAX_SPEAKERS)
                .withStyle(ChatFormatting.YELLOW);
            case FULL -> Component.translatable("songcraft_instruments.speaker.full", MAX_SPEAKERS)
                .withStyle(ChatFormatting.RED);
            case LOOPER_TAKEN -> Component.translatable("songcraft_instruments.speaker.looper_taken")
                .withStyle(ChatFormatting.RED);
        };
        player.displayClientMessage(message, true);
    }
    /**
     * @param speaker The speaker that was unpaired, to mention when it's still connected to something else
     */
    public static void sendUnpairMessage(final Player player, final boolean removed, final int count, final Speaker speaker) {
        if (!removed) {
            player.displayClientMessage(
                Component.translatable("songcraft_instruments.speaker.not_paired").withStyle(ChatFormatting.YELLOW)
            , true);
            return;
        }

        final MutableComponent message = Component.translatable("songcraft_instruments.speaker.unpaired", count, MAX_SPEAKERS);
        if (speaker.isConnected())
            message.append(" ").append(stillConnectedNote(Component.translatable("songcraft_instruments.speaker.still_connected")));
        player.displayClientMessage(message.withStyle(ChatFormatting.GREEN), true);
    }
    public static void sendUnpairAllMessage(final Player player, final UnpairAllResult result) {
        if (result.removed() <= 0) {
            player.displayClientMessage(
                Component.translatable("songcraft_instruments.speaker.none_paired").withStyle(ChatFormatting.YELLOW)
            , true);
            return;
        }

        final MutableComponent message = Component.translatable("songcraft_instruments.speaker.unpaired_all", result.removed());
        if (result.stillConnected() > 0)
            message.append(" ").append(stillConnectedNote(Component.translatable("songcraft_instruments.speaker.still_connected_count", result.stillConnected())));
        player.displayClientMessage(message.withStyle(ChatFormatting.GREEN), true);
    }
    /**
     * dark orange, so the "still connected" note stands out from the green message.
     * a child's own color overrides the parent's, so the rest of the message stays green.
     */
    private static final TextColor STILL_CONNECTED_COLOR = TextColor.fromRgb(0xFF8C00);
    private static MutableComponent stillConnectedNote(final MutableComponent note) {
        return note.withStyle((style) -> style.withColor(STILL_CONNECTED_COLOR));
    }

    public static void sendSpeakerUnpairAllMessage(final Player player, final int removedCount) {
        player.displayClientMessage((removedCount > 0)
            ? Component.translatable("songcraft_instruments.speaker.speaker_unpaired_all", removedCount)
                .withStyle(ChatFormatting.GREEN)
            : Component.translatable("songcraft_instruments.speaker.speaker_none_paired")
                .withStyle(ChatFormatting.YELLOW)
        , true);
    }
}
