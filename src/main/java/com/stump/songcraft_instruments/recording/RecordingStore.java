package com.stump.songcraft_instruments.recording;

import com.mojang.logging.LogUtils;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.recording.RecordingCodec.InvalidRecordingException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The recordings of the world, one {@code .screc} file each in {@code <world>/songcraft_instruments/recordings}.
 * Records only hold the ID of their recording, so copies share one file.
 * <p>
 * Recordings are never deleted automatically, as copies of a record may be anywhere (or nowhere) without the mod knowing.
 * Operators clean up the ones not seen in a while with {@code /screcord cleanup}; see {@link RecordingSightings}.
 * <p>
 * Server thread only.
 */
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public final class RecordingStore {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String FILE_EXTENSION = ".screc";

    private static RecordingStore instance;

    private final MinecraftServer server;
    private final Path dir;
    /**
     * Loaded recordings, kept for as long as a looper holds on to them
     */
    private final Map<String, WeakReference<Recording>> loaded = new HashMap<>();

    private RecordingStore(final MinecraftServer server) {
        this.server = server;
        dir = server.getWorldPath(LevelResource.ROOT).resolve(SCInstrumentMod.MODID).resolve("recordings").normalize();
    }

    public static RecordingStore get(final MinecraftServer server) {
        if (instance == null || instance.server != server)
            instance = new RecordingStore(server);
        return instance;
    }
    /**
     * @return The store of the running server, or null if there is none
     */
    public static @Nullable RecordingStore get() {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return (server == null) ? null : get(server);
    }

    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        instance = null;
    }


    RecordingIndex index() {
        return server.overworld().getDataStorage().computeIfAbsent(RecordingIndex::load, RecordingIndex::new, RecordingIndex.NAME);
    }

    private Path path(final String id) {
        return dir.resolve(id + FILE_EXTENSION);
    }


    /**
     * @return The recording, or empty if there is no valid recording by this ID
     */
    public Optional<Recording> get(final String id) {
        if (!RecordingCodec.isValidId(id))
            return Optional.empty();

        final WeakReference<Recording> cached = loaded.get(id);
        if (cached != null && cached.get() != null)
            return Optional.of(cached.get());

        final Path path = path(id);
        if (!Files.isRegularFile(path))
            return Optional.empty();

        try {
            final Recording recording = RecordingCodec.decode(Files.readAllBytes(path));
            loaded.put(id, new WeakReference<>(recording));
            return Optional.of(recording);
        } catch (IOException e) {
            LOGGER.error("Could not read recording {}", path, e);
            return Optional.empty();
        }
    }

    /**
     * @return Whether the recording is stored in this world
     */
    public boolean has(final String id) {
        return RecordingCodec.isValidId(id) && Files.isRegularFile(path(id));
    }

    /**
     * @return The contents of the recording's file, as exported
     */
    public Optional<byte[]> readFile(final String id) {
        if (!RecordingCodec.isValidId(id) || !Files.isRegularFile(path(id)))
            return Optional.empty();

        try {
            return Optional.of(Files.readAllBytes(path(id)));
        } catch (IOException e) {
            LOGGER.error("Could not read recording {}", path(id), e);
            return Optional.empty();
        }
    }

    /**
     * Stores the recording, unless an identical one is already stored.
     * @return The ID of the recording
     */
    public String save(final Recording recording) throws IOException {
        final byte[] raw = RecordingCodec.encodeRaw(recording);
        final String id = RecordingCodec.id(raw);
        final Path path = path(id);

        if (!Files.isRegularFile(path)) {
            Files.createDirectories(dir);
            // Written whole before it appears, so a crash never leaves a broken recording behind
            final Path temp = dir.resolve(id + ".tmp");
            Files.write(temp, RecordingCodec.compress(raw));
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        loaded.put(id, new WeakReference<>(recording));
        markSeen(id);
        return id;
    }

    /**
     * Reads and stores the contents of a {@code .screc} file
     * @return The ID of the recording, and the recording
     * @throws InvalidRecordingException If the file is not a valid recording
     */
    public Map.Entry<String, Recording> importFile(final byte[] file) throws IOException {
        final Recording recording = RecordingCodec.decode(file);
        return Map.entry(save(recording), recording);
    }


    /**
     * Notes that a record of the recording exists, as used by {@code /screcord cleanup}
     */
    public void markSeen(final String id) {
        if (RecordingCodec.isValidId(id))
            index().markSeen(id, System.currentTimeMillis());
    }

    /**
     * @return Whether a loaded looper is holding on to the recording
     */
    public boolean isInUse(final String id) {
        final WeakReference<Recording> cached = loaded.get(id);
        return cached != null && cached.get() != null;
    }


    /**
     * @param lastSeen When a record of it was last seen, or when the file was written if never
     */
    public record StoredRecording(String id, long size, long lastSeen) {}

    public List<StoredRecording> list() throws IOException {
        if (!Files.isDirectory(dir))
            return List.of();

        final RecordingIndex index = index();
        final List<StoredRecording> result = new ArrayList<>();

        try (final Stream<Path> files = Files.list(dir)) {
            for (final Path file : (Iterable<Path>) files::iterator) {
                final String name = file.getFileName().toString();
                if (!name.endsWith(FILE_EXTENSION))
                    continue;

                final String id = name.substring(0, name.length() - FILE_EXTENSION.length());
                if (!RecordingCodec.isValidId(id))
                    continue;

                final Long seen = index.getLastSeen(id);
                result.add(new StoredRecording(id, Files.size(file),
                    (seen != null) ? seen : Files.getLastModifiedTime(file).toMillis()));
            }
        }
        return result;
    }

    public void delete(final String id) throws IOException {
        if (!RecordingCodec.isValidId(id))
            return;

        Files.deleteIfExists(path(id));
        loaded.remove(id);
        index().remove(id);
    }
}
