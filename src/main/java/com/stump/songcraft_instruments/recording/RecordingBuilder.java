package com.stump.songcraft_instruments.recording;

import com.stump.songcraft_instruments.recording.Recording.Performer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The notes of a recording in progress, kept by the looper recording them until it is finalized with {@link #build}.
 */
public final class RecordingBuilder {
    private final List<String> instrumentIds = new ArrayList<>(), soundTypes = new ArrayList<>();
    private final List<Performer> performers = new ArrayList<>();
    private final List<int[]> notes = new ArrayList<>();

    public RecordingBuilder() {}

    /**
     * Continues an unfinished recording, as saved by the looper
     */
    public RecordingBuilder(final Recording draft) {
        instrumentIds.addAll(draft.instrumentIds());
        soundTypes.addAll(draft.soundTypes());
        performers.addAll(draft.performers());
        for (int i = 0; i < draft.noteCount(); i++)
            notes.add(draft.note(i).clone());
    }


    public int noteCount() {
        return notes.size();
    }
    public boolean isEmpty() {
        return notes.isEmpty();
    }

    public void clear() {
        instrumentIds.clear();
        soundTypes.clear();
        performers.clear();
        notes.clear();
    }

    /**
     * @param name Looked up only if the performer is new
     * @param colors Looked up only if the performer is new
     * @return The index of the performer, added if not yet present
     */
    public int performerIndex(final UUID id, final Supplier<String> name, final Supplier<int[]> colors) {
        for (int i = 0; i < performers.size(); i++) {
            if (performers.get(i).id().equals(id))
                return i;
        }

        performers.add(new Performer(id, name.get(), colors.get()));
        return performers.size() - 1;
    }


    public void addDampen(final int timestamp, final int performer) {
        notes.add(new int[] {timestamp, Recording.TYPE_DAMPEN, performer});
    }

    public void addNote(final int timestamp, final int performer,
                        final int pitch, final int volume, final int particleColor,
                        final String instrumentId, final String soundType, final int soundIndex) {
        notes.add(new int[] {
            timestamp, Recording.TYPE_REGULAR, performer,
            pitch, volume, particleColor,
            paletteIndex(instrumentIds, instrumentId), paletteIndex(soundTypes, soundType), soundIndex
        });
    }

    /**
     * @param phase {@link Recording#PHASE_ATTACK} or {@link Recording#PHASE_RELEASE}
     */
    public void addHeldNote(final int timestamp, final int performer,
                            final int pitch, final int volume, final int particleColor,
                            final String instrumentId, final String soundType, final int soundIndex,
                            final int phase) {
        notes.add(new int[] {
            timestamp, Recording.TYPE_HELD, performer,
            pitch, volume, particleColor,
            paletteIndex(instrumentIds, instrumentId), paletteIndex(soundTypes, soundType), soundIndex,
            phase
        });
    }

    private static int paletteIndex(final List<String> palette, final String value) {
        final int index = palette.indexOf(value);
        if (index != -1)
            return index;

        palette.add(value);
        return palette.size() - 1;
    }


    /**
     * @param length The tick the recording loops back at, or -1 if it is unfinished
     */
    public Recording build(final int length) {
        final List<int[]> sorted = new ArrayList<>(notes.size());
        for (final int[] note : notes)
            sorted.add(note.clone());
        // Stable, so notes of the same tick keep the order they were played in
        sorted.sort(Comparator.comparingInt((note) -> note[Recording.TIMESTAMP]));

        return new Recording(length, instrumentIds, soundTypes, performers, sorted.toArray(int[][]::new));
    }
}
