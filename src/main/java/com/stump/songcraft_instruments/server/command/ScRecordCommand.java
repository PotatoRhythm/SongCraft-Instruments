package com.stump.songcraft_instruments.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import com.stump.songcraft_instruments.config.ModServerConfigs;
import com.stump.songcraft_instruments.item.record.WritableRecordItem;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.record.S2CRecordFilesListPacket;
import com.stump.songcraft_instruments.recording.RecordTransfers;
import com.stump.songcraft_instruments.recording.RecordingSightings;
import com.stump.songcraft_instruments.recording.RecordingStore;
import com.stump.songcraft_instruments.recording.RecordingStore.StoredRecording;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * {@code /screcord}: exporting records to files on the player's computer, importing them back
 * (into any world or server), and cleaning up the recordings of records nobody has seen in a while.
 */
public class ScRecordCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private static final SimpleCommandExceptionType
        ERROR_NOT_BURNED = new SimpleCommandExceptionType(RecordTransfers.message("failed.not_burned")),
        ERROR_NOT_EMPTY = new SimpleCommandExceptionType(RecordTransfers.message("failed.not_empty")),
        ERROR_MISSING = new SimpleCommandExceptionType(RecordTransfers.message("failed.missing")),
        ERROR_IMPORT_DISABLED = new SimpleCommandExceptionType(RecordTransfers.message("failed.import_disabled")),
        ERROR_IMPORT_PENDING = new SimpleCommandExceptionType(RecordTransfers.message("failed.import_pending")),
        ERROR_IO = new SimpleCommandExceptionType(RecordTransfers.message("failed.io"));
    private static final DynamicCommandExceptionType ERROR_ILLEGAL_NAME = new DynamicCommandExceptionType((name) ->
        RecordTransfers.message("failed.illegal_name", name)
    );


    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("screcord")
            .then(Commands.literal("export")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ScRecordCommand::export)
                )
            )
            .then(Commands.literal("import")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(ScRecordCommand::importRecord)
                )
            )
            .then(Commands.literal("files")
                .executes(ScRecordCommand::listFiles)
            )
            .then(Commands.literal("stats")
                .requires((source) -> source.hasPermission(2))
                .executes(ScRecordCommand::stats)
            )
            .then(Commands.literal("cleanup")
                .requires((source) -> source.hasPermission(3))
                .then(Commands.argument("days", IntegerArgumentType.integer(1))
                    .executes((context) -> cleanup(context, false))
                    .then(Commands.literal("confirm")
                        .executes((context) -> cleanup(context, true))
                    )
                )
            )
        );
    }

    private static String getFileName(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final String name = StringArgumentType.getString(context, "name");
        if (!RecordTransfers.isValidFileName(name))
            throw ERROR_ILLEGAL_NAME.create(name);
        return name;
    }


    private static int export(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final String fileName = getFileName(context);

        final ItemStack record = RecordTransfers.getHeldRecord(player, true);
        if (record == null)
            throw ERROR_NOT_BURNED.create();

        final RecordingStore store = RecordingStore.get(player.server);
        final String id = WritableRecordItem.getRecordingId(record);
        final byte[] file = store.readFile(id).orElseThrow(ERROR_MISSING::create);
        store.markSeen(id);

        context.getSource().sendSuccess(() -> RecordTransfers.message("export.started", fileName), false);
        RecordTransfers.export(player, fileName, file);
        return 1;
    }

    private static int importRecord(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ServerPlayer player = context.getSource().getPlayerOrException();
        final String fileName = getFileName(context);

        if (ModServerConfigs.RECORD_IMPORT_OPERATORS_ONLY.get() && !context.getSource().hasPermission(2))
            throw ERROR_IMPORT_DISABLED.create();
        if (RecordTransfers.getHeldRecord(player, false) == null)
            throw ERROR_NOT_EMPTY.create();
        final MutableComponent limited = RecordTransfers.checkImportLimits(player);
        if (limited != null)
            throw new SimpleCommandExceptionType(limited).create();
        if (!RecordTransfers.requestImport(player, fileName))
            throw ERROR_IMPORT_PENDING.create();

        return 1;
    }

    private static int listFiles(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        SCPacketHandler.sendToClient(new S2CRecordFilesListPacket(), context.getSource().getPlayerOrException());
        return 1;
    }


    private static int stats(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final List<StoredRecording> recordings = listRecordings(context.getSource().getServer());
        final long bytes = recordings.stream().mapToLong(StoredRecording::size).sum();

        context.getSource().sendSuccess(() -> RecordTransfers.message("stats", recordings.size(), kilobytes(bytes)), false);
        return recordings.size();
    }

    /**
     * Deletes the recordings not seen in the given number of days. Previews what would be deleted unless confirmed.
     */
    private static int cleanup(final CommandContext<CommandSourceStack> context, final boolean confirm) throws CommandSyntaxException {
        final MinecraftServer server = context.getSource().getServer();
        final RecordingStore store = RecordingStore.get(server);
        final int days = IntegerArgumentType.getInteger(context, "days");
        final long cutoff = System.currentTimeMillis() - days * DAY_MS;

        List<StoredRecording> stale = listRecordings(server).stream()
            .filter((recording) -> recording.lastSeen() < cutoff && !store.isInUse(recording.id()))
            .toList();

        // Records of players who haven't been online in a while are still theirs
        if (!stale.isEmpty()) {
            final Set<String> held = findPlayersRecordings(server);
            held.forEach(store::markSeen);
            stale = stale.stream().filter((recording) -> !held.contains(recording.id())).toList();
        }

        final long bytes = stale.stream().mapToLong(StoredRecording::size).sum();
        final int count = stale.size();

        if (count == 0) {
            context.getSource().sendSuccess(() -> RecordTransfers.message("cleanup.none", days), false);
            return 0;
        }

        if (!confirm) {
            context.getSource().sendSuccess(() -> RecordTransfers.message("cleanup.preview", count, kilobytes(bytes), days, days), false);
            return count;
        }

        for (final StoredRecording recording : stale) {
            try {
                store.delete(recording.id());
            } catch (IOException e) {
                LOGGER.error("Could not delete recording {}", recording.id(), e);
                throw ERROR_IO.create();
            }
        }
        LOGGER.info("Cleaned up {} recordings not seen in {} days", count, days);

        context.getSource().sendSuccess(() -> RecordTransfers.message("cleanup.done", count, kilobytes(bytes)), true);
        return count;
    }

    private static List<StoredRecording> listRecordings(final MinecraftServer server) throws CommandSyntaxException {
        try {
            return RecordingStore.get(server).list();
        } catch (IOException e) {
            LOGGER.error("Could not list the recordings of the world", e);
            throw ERROR_IO.create();
        }
    }

    /**
     * @return The recordings of every record in an online or offline player's inventory or ender chest
     */
    private static Set<String> findPlayersRecordings(final MinecraftServer server) {
        final Set<String> found = new HashSet<>();
        final Set<UUID> online = new HashSet<>();

        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            RecordingSightings.scanPlayer(player, found::add);
        }

        final Path playerData = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        if (!Files.isDirectory(playerData))
            return found;

        try (final Stream<Path> files = Files.list(playerData)) {
            for (final Path file : (Iterable<Path>) files::iterator) {
                final String name = file.getFileName().toString();
                if (!name.endsWith(".dat"))
                    continue;

                try {
                    if (online.contains(UUID.fromString(name.substring(0, name.length() - 4))))
                        continue;
                    RecordingSightings.findRecordings(NbtIo.readCompressed(file.toFile()), found::add);
                } catch (IllegalArgumentException | IOException e) {
                    LOGGER.warn("Could not check the player data {} for records", file, e);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not check offline players for records", e);
        }

        return found;
    }

    private static String kilobytes(final long bytes) {
        return String.format("%.1f", bytes / 1024.0);
    }
}
