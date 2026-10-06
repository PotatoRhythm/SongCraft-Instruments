package com.stump.songcraft_instruments.recording;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * When each recording's record was last seen, for {@code /screcord cleanup},
 * and what each player imported in the last day, for the import limits.
 * Saved with the world as {@code data/songcraft_instruments_recordings.dat}.
 */
public class RecordingIndex extends SavedData {
    public static final String NAME = "songcraft_instruments_recordings";
    private static final String LAST_SEEN_TAG = "LastSeen", IMPORTS_TAG = "Imports";

    /**
     * Last seen times are only saved again once they are this old, so that records in use don't keep the index dirty
     */
    private static final long SEEN_RESOLUTION = 10 * 60 * 1000;
    public static final long IMPORT_WINDOW = 24 * 60 * 60 * 1000;

    /**
     * A player's import: when, and how many bytes of new recordings it stored
     */
    public record Import(long time, long bytes) {}

    private final Map<String, Long> lastSeen = new HashMap<>();
    private final Map<UUID, List<Import>> imports = new HashMap<>();

    public RecordingIndex() {}

    public static RecordingIndex load(final CompoundTag tag) {
        final RecordingIndex index = new RecordingIndex();

        final CompoundTag seen = tag.getCompound(LAST_SEEN_TAG);
        for (final String id : seen.getAllKeys())
            index.lastSeen.put(id, seen.getLong(id));

        final CompoundTag imports = tag.getCompound(IMPORTS_TAG);
        for (final String player : imports.getAllKeys()) {
            final List<Import> playerImports = new ArrayList<>();
            for (final Tag entry : imports.getList(player, Tag.TAG_LONG_ARRAY)) {
                final long[] values = ((LongArrayTag) entry).getAsLongArray();
                if (values.length == 2)
                    playerImports.add(new Import(values[0], values[1]));
            }
            try {
                index.imports.put(UUID.fromString(player), playerImports);
            } catch (IllegalArgumentException ignored) {}
        }

        return index;
    }

    @Override
    public CompoundTag save(final CompoundTag tag) {
        final CompoundTag seen = new CompoundTag();
        lastSeen.forEach(seen::putLong);
        tag.put(LAST_SEEN_TAG, seen);

        final CompoundTag importsTag = new CompoundTag();
        final long now = System.currentTimeMillis();
        imports.forEach((player, playerImports) -> {
            final ListTag list = new ListTag();
            for (final Import entry : playerImports) {
                if (now - entry.time() < IMPORT_WINDOW)
                    list.add(new LongArrayTag(new long[] {entry.time(), entry.bytes()}));
            }
            if (!list.isEmpty())
                importsTag.put(player.toString(), list);
        });
        tag.put(IMPORTS_TAG, importsTag);

        return tag;
    }


    /**
     * @param now The current time in milliseconds
     */
    public void markSeen(final String id, final long now) {
        final Long previous = lastSeen.get(id);
        if (previous != null && now - previous < SEEN_RESOLUTION)
            return;

        lastSeen.put(id, now);
        setDirty();
    }

    /**
     * @return When the recording was last seen in milliseconds, or null if never
     */
    public Long getLastSeen(final String id) {
        return lastSeen.get(id);
    }

    public void remove(final String id) {
        if (lastSeen.remove(id) != null)
            setDirty();
    }


    /**
     * @return The player's imports in the last 24 hours, oldest first
     */
    public List<Import> getRecentImports(final UUID player, final long now) {
        final List<Import> playerImports = imports.get(player);
        if (playerImports == null)
            return List.of();

        playerImports.removeIf((entry) -> now - entry.time() >= IMPORT_WINDOW);
        return List.copyOf(playerImports);
    }

    public void addImport(final UUID player, final Import entry) {
        imports.computeIfAbsent(player, (key) -> new ArrayList<>()).add(entry);
        setDirty();
    }
}
