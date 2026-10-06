package com.stump.songcraft_instruments.networking.packet.record;

import com.mojang.logging.LogUtils;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.recording.RecordTransfers;
import com.stump.songcraft_instruments.recording.RecordingStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The player's record files, in {@code .minecraft/songcraft_records}. Client only.
 */
public final class RecordFilesClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Exported files still arriving, by file name. Null when a part arrived for a file that can't be saved.
     */
    private static final Map<String, ByteArrayOutputStream> INCOMING = new HashMap<>();

    private RecordFilesClient() {}


    private static Path folder() {
        return FMLPaths.GAMEDIR.get().resolve(RecordTransfers.FOLDER_NAME);
    }
    private static Path file(final String fileName) {
        return folder().resolve(fileName + RecordingStore.FILE_EXTENSION);
    }

    private static void tell(final Component message) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null)
            minecraft.player.sendSystemMessage(message);
    }
    private static void fail(final Component message) {
        tell(message.copy().withStyle(ChatFormatting.RED));
    }


    public static void receiveExportPart(final String fileName, final int part, final int parts, final byte[] data) {
        if (!RecordTransfers.isValidFileName(fileName))
            return;

        if (part == 0) {
            if (Files.exists(file(fileName))) {
                INCOMING.put(fileName, null);
                fail(RecordTransfers.message("export.exists", fileName));
                return;
            }
            INCOMING.put(fileName, new ByteArrayOutputStream());
        }

        final ByteArrayOutputStream incoming = INCOMING.get(fileName);
        if (incoming == null)
            return;
        incoming.write(data, 0, data.length);

        if (part != parts - 1)
            return;

        INCOMING.remove(fileName);
        try {
            Files.createDirectories(folder());
            Files.write(file(fileName), incoming.toByteArray());
            tell(RecordTransfers.message("export.success", pathLink(file(fileName))).withStyle(ChatFormatting.GREEN));
        } catch (IOException e) {
            LOGGER.error("Could not save record file {}", file(fileName), e);
            fail(RecordTransfers.message("export.failed", e.getMessage()));
        }
    }


    public static void uploadFile(final String fileName, final int maxBytes) {
        if (!RecordTransfers.isValidFileName(fileName))
            return;

        final Path file = file(fileName);
        if (!Files.isRegularFile(file)) {
            fail(RecordTransfers.message("import.not_found", fileName, pathLink(folder())));
            SCPacketHandler.sendToServer(C2SRecordFilePartPacket.cancel(fileName));
            return;
        }

        final byte[] data;
        try {
            if (Files.size(file) > maxBytes) {
                fail(RecordTransfers.message("import.too_large", fileName, Files.size(file) / 1024, maxBytes / 1024));
                SCPacketHandler.sendToServer(C2SRecordFilePartPacket.cancel(fileName));
                return;
            }
            data = Files.readAllBytes(file);
        } catch (IOException e) {
            fail(RecordTransfers.message("import.read_failed", fileName, e.getMessage()));
            SCPacketHandler.sendToServer(C2SRecordFilePartPacket.cancel(fileName));
            return;
        }

        tell(RecordTransfers.message("import.started", fileName).withStyle(ChatFormatting.GRAY));

        final int parts = Math.max(1, (data.length + RecordTransfers.PART_SIZE - 1) / RecordTransfers.PART_SIZE);
        for (int part = 0; part < parts; part++) {
            SCPacketHandler.sendToServer(new C2SRecordFilePartPacket(fileName, part, parts, Arrays.copyOfRange(data,
                part * RecordTransfers.PART_SIZE, Math.min(data.length, (part + 1) * RecordTransfers.PART_SIZE))));
        }
    }


    public static void listFiles() {
        final List<Path> files;
        try (final Stream<Path> list = Files.isDirectory(folder()) ? Files.list(folder()) : Stream.empty()) {
            files = list
                .filter((file) -> file.getFileName().toString().endsWith(RecordingStore.FILE_EXTENSION))
                .sorted()
                .toList();
        } catch (IOException e) {
            fail(RecordTransfers.message("import.read_failed", folder(), e.getMessage()));
            return;
        }

        if (files.isEmpty()) {
            tell(RecordTransfers.message("files.none", pathLink(folder())));
            return;
        }

        tell(RecordTransfers.message("files.header", pathLink(folder())));
        for (final Path file : files) {
            final String fullName = file.getFileName().toString();
            final String name = fullName.substring(0, fullName.length() - RecordingStore.FILE_EXTENSION.length());
            long size;
            try {
                size = Files.size(file);
            } catch (IOException e) {
                size = 0;
            }

            final String command = "/screcord import " + name;
            tell(Component.literal(" - " + name).withStyle((style) -> style
                    .withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))))
                .append(Component.literal(String.format(" (%.1f KB)", size / 1024f)).withStyle(ChatFormatting.GRAY)));
        }
    }

    /**
     * @return The path, opening its folder when clicked
     */
    private static Component pathLink(final Path path) {
        final Path folder = Files.isDirectory(path) ? path : path.getParent();
        return Component.literal(FMLPaths.GAMEDIR.get().relativize(path).toString()).withStyle((style) -> style
            .withUnderlined(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, folder.toAbsolutePath().toString())));
    }
}
