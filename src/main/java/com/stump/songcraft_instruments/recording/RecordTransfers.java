package com.stump.songcraft_instruments.recording;

import com.mojang.logging.LogUtils;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.config.ModServerConfigs;
import com.stump.songcraft_instruments.gamerule.ModGameRules;
import com.stump.songcraft_instruments.item.ModItems;
import com.stump.songcraft_instruments.item.record.WritableRecordItem;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.record.S2CRecordFilePartPacket;
import com.stump.songcraft_instruments.networking.packet.record.S2CRecordImportRequestPacket;
import com.stump.songcraft_instruments.recording.RecordingCodec.InvalidRecordingException;
import com.stump.songcraft_instruments.recording.RecordingCodec.TooManyNotesException;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sends record files between the server and players' computers for {@code /screcord export} and {@code import},
 * in parts small enough for any packet.
 * Exported files are saved in the player's {@code .minecraft/songcraft_records} folder.
 */
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public final class RecordTransfers {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Below the 32 KB limit of packets sent to the server
     */
    public static final int PART_SIZE = 30000;
    public static final int MAX_FILE_NAME_LENGTH = 64;
    public static final String FOLDER_NAME = "songcraft_records";
    private static final long IMPORT_TIMEOUT_MS = 60 * 1000;
    /**
     * The largest single import, bounding the memory an upload takes while it arrives
     * (for operators and servers without a daily limit; otherwise the player's remaining daily limit is used)
     */
    public static final int MAX_UPLOAD_BYTES = 16 * 1024 * 1024;

    private record PendingImport(String fileName, int maxBytes, long startedAt, ByteArrayOutputStream data, int[] nextPart) {}
    private static final Map<UUID, PendingImport> PENDING_IMPORTS = new HashMap<>();

    private RecordTransfers() {}


    public static boolean isValidFileName(final String name) {
        return name != null && name.matches("[A-Za-z0-9_\\-]{1," + MAX_FILE_NAME_LENGTH + "}");
    }

    public static MutableComponent message(final String key, final Object... args) {
        return Component.translatable("commands.songcraft_instruments.screcord." + key, args);
    }


    /**
     * Sends the recording's file to the player, to be saved under the file name
     */
    public static void export(final ServerPlayer player, final String fileName, final byte[] file) {
        final int parts = Math.max(1, (file.length + PART_SIZE - 1) / PART_SIZE);
        for (int part = 0; part < parts; part++) {
            final byte[] data = Arrays.copyOfRange(file, part * PART_SIZE, Math.min(file.length, (part + 1) * PART_SIZE));
            SCPacketHandler.sendToClient(new S2CRecordFilePartPacket(fileName, part, parts, data), player);
        }
    }


    /**
     * Operators may import as much as they like
     */
    private static boolean isExemptFromLimits(final ServerPlayer player) {
        return player.hasPermissions(2);
    }

    /**
     * @return Why the player may not import right now, or null if they may
     */
    public static @Nullable MutableComponent checkImportLimits(final ServerPlayer player) {
        if (isExemptFromLimits(player))
            return null;

        final long now = System.currentTimeMillis();

        final long limit = ModServerConfigs.RECORD_IMPORT_DAILY_LIMIT_KB.get() * 1024L;
        if (limit > 0) {
            final List<RecordingIndex.Import> recent = RecordingStore.get(player.server).index().getRecentImports(player.getUUID(), now);
            final long used = recent.stream().mapToLong(RecordingIndex.Import::bytes).sum();
            if (used >= limit) {
                // Until the oldest counted import is a day old
                final long waitMs = recent.stream().filter((entry) -> entry.bytes() > 0).findFirst()
                    .map((entry) -> entry.time() + RecordingIndex.IMPORT_WINDOW - now).orElse(0L);
                return message("failed.import_daily_limit", used / 1024, limit / 1024, Math.max(1, (waitMs + 3599999) / 3600000));
            }
        }

        return null;
    }

    /**
     * @return How many bytes the player may still import today, at most {@link #MAX_UPLOAD_BYTES}
     */
    private static int getUploadAllowance(final ServerPlayer player) {
        final long limit = ModServerConfigs.RECORD_IMPORT_DAILY_LIMIT_KB.get() * 1024L;
        if (isExemptFromLimits(player) || limit <= 0)
            return MAX_UPLOAD_BYTES;

        final long used = RecordingStore.get(player.server).index()
            .getRecentImports(player.getUUID(), System.currentTimeMillis()).stream()
            .mapToLong(RecordingIndex.Import::bytes).sum();
        return (int) Math.max(0, Math.min(MAX_UPLOAD_BYTES, limit - used));
    }

    /**
     * Asks the player's client to upload their record file
     * @return Whether the import started; false if the player already has one in progress
     */
    public static boolean requestImport(final ServerPlayer player, final String fileName) {
        final PendingImport pending = PENDING_IMPORTS.get(player.getUUID());
        if (pending != null && !isTimedOut(pending))
            return false;

        final int maxBytes = getUploadAllowance(player);
        PENDING_IMPORTS.put(player.getUUID(),
            new PendingImport(fileName, maxBytes, System.currentTimeMillis(), new ByteArrayOutputStream(), new int[1]));
        SCPacketHandler.sendToClient(new S2CRecordImportRequestPacket(fileName, maxBytes), player);
        return true;
    }

    private static boolean isTimedOut(final PendingImport pending) {
        return System.currentTimeMillis() - pending.startedAt() > IMPORT_TIMEOUT_MS;
    }

    public static void receiveImportPart(final ServerPlayer player, final String fileName,
                                         final int part, final int parts, final byte[] data) {
        final PendingImport pending = PENDING_IMPORTS.get(player.getUUID());
        // Only files the player was asked for
        if (pending == null || !pending.fileName().equals(fileName))
            return;

        // The client could not send it, and told the player why
        if (parts == 0) {
            PENDING_IMPORTS.remove(player.getUUID());
            return;
        }

        if (isTimedOut(pending)) {
            fail(player, message("import.timed_out"));
            return;
        }

        final int maxParts = (pending.maxBytes() + PART_SIZE - 1) / PART_SIZE;
        if (part != pending.nextPart()[0] || parts > maxParts || part >= parts
                || pending.data().size() + data.length > pending.maxBytes()) {
            fail(player, message("import.invalid"));
            return;
        }

        pending.data().write(data, 0, data.length);
        pending.nextPart()[0]++;

        if (part == parts - 1) {
            PENDING_IMPORTS.remove(player.getUUID());
            completeImport(player, pending.data().toByteArray());
        }
    }

    private static void fail(final ServerPlayer player, final Component reason) {
        PENDING_IMPORTS.remove(player.getUUID());
        player.sendSystemMessage(reason.copy().withStyle(ChatFormatting.RED));
    }

    private static void completeImport(final ServerPlayer player, final byte[] file) {
        // Recordings are held to the same note limit as loopers
        final int cap = player.serverLevel().getGameRules().getInt(ModGameRules.RULE_LOOPER_MAX_NOTES);
        final Recording recording;
        try {
            recording = RecordingCodec.decode(file, cap);
        } catch (TooManyNotesException e) {
            fail(player, message("import.too_many_notes", e.noteCount, cap));
            return;
        } catch (InvalidRecordingException e) {
            fail(player, message("import.invalid"));
            return;
        }

        final ItemStack emptyRecord = getHeldRecord(player, false);
        if (emptyRecord == null) {
            fail(player, message("import.no_empty_record"));
            return;
        }

        // Only recordings new to this world count towards the daily limit
        final RecordingStore store = RecordingStore.get(player.server);
        final long now = System.currentTimeMillis();
        final long newBytes = store.has(RecordingCodec.id(RecordingCodec.encodeRaw(recording))) ? 0 : file.length;
        final long limit = ModServerConfigs.RECORD_IMPORT_DAILY_LIMIT_KB.get() * 1024L;
        if (limit > 0 && newBytes > 0 && !isExemptFromLimits(player)) {
            final long used = store.index().getRecentImports(player.getUUID(), now).stream()
                .mapToLong(RecordingIndex.Import::bytes).sum();
            if (used + newBytes > limit) {
                fail(player, message("import.daily_limit", (newBytes + 1023) / 1024, used / 1024, limit / 1024));
                return;
            }
        }

        final String id;
        try {
            id = store.save(recording);
        } catch (IOException e) {
            LOGGER.error("Could not save the recording imported by {}", player.getGameProfile().getName(), e);
            fail(player, message("failed.io"));
            return;
        }
        store.index().addImport(player.getUUID(), new RecordingIndex.Import(now, newBytes));

        if (emptyRecord.getCount() == 1) {
            WritableRecordItem.burn(emptyRecord, id, recording);
        } else {
            emptyRecord.shrink(1);
            final ItemStack burned = new ItemStack(ModItems.RECORD_WRITABLE.get());
            WritableRecordItem.burn(burned, id, recording);
            if (!player.getInventory().add(burned))
                player.drop(burned, false);
        }

        player.sendSystemMessage(message("import.success").withStyle(ChatFormatting.GREEN));
    }

    /**
     * @param burned Whether to look for a burned record, or an empty one
     * @return The record the player holds in their main hand, or else their off hand. Null if neither holds one.
     */
    public static @Nullable ItemStack getHeldRecord(final ServerPlayer player, final boolean burned) {
        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof WritableRecordItem record && record.isBurned(stack) == burned)
                return stack;
        }
        return null;
    }


    @SubscribeEvent
    public static void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING_IMPORTS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        PENDING_IMPORTS.clear();
    }

    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_IMPORTS.isEmpty())
            return;

        PENDING_IMPORTS.entrySet().removeIf((entry) -> {
            if (!isTimedOut(entry.getValue()))
                return false;

            final ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null)
                player.sendSystemMessage(message("import.timed_out").withStyle(ChatFormatting.RED));
            return true;
        });
    }
}
