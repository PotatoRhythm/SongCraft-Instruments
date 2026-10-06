package com.stump.songcraft_instruments.recording;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A finished, immutable recording: the notes a record plays back, kept in the world save by {@link RecordingStore}
 * rather than on the record item, so that its size is unlimited.
 * <p>
 * Notes are sorted by their timestamp and indexed by tick, so that playback only visits the notes of the current tick.
 */
public final class Recording {
    // The fields of a note, by index
    public static final int
        TIMESTAMP = 0,
        // One of the TYPE_ constants
        TYPE = 1,
        PERFORMER = 2,
        // Dampens end here
        PITCH = 3,
        // In percent
        VOLUME = 4,
        PARTICLE_COLOR = 5,
        // Indices into the instrument ID and sound type palettes
        INSTRUMENT_ID = 6,
        SOUND_TYPE = 7,
        SOUND_INDEX = 8,
        // Held notes only: 0 for the attack, 1 for the release
        HELD_PHASE = 9
    ;
    public static final int
        DAMPEN_LENGTH = 3, REGULAR_LENGTH = 9, HELD_LENGTH = 10;

    // Note types, matching the ordinals of WritableNoteType
    public static final int TYPE_REGULAR = 0, TYPE_HELD = 1, TYPE_DAMPEN = 2;
    public static final int PHASE_ATTACK = 0, PHASE_RELEASE = 1;

    /**
     * A player recorded on this recording. Their notes are played back as separate initiators,
     * so that dampening and held notes of one do not affect the others.
     * @param colors The particle colors of the performer when they recorded, for the record's tooltip. Empty if unknown.
     */
    public record Performer(UUID id, String name, int[] colors) {}

    /**
     * The tick at which the recording loops back to its start, or -1 for an unfinished recording
     */
    private final int length;
    private final List<String> instrumentIds, soundTypes;
    private final List<Performer> performers;
    private final int[][] notes;
    /**
     * The index of the first note at each tick; the notes of tick t are {@code [tickStarts[t], tickStarts[t + 1])}
     */
    private final int[] tickStarts;

    /**
     * @param notes Notes sorted by timestamp, with palette and performer indices in range
     */
    Recording(final int length, final List<String> instrumentIds, final List<String> soundTypes,
              final List<Performer> performers, final int[][] notes) {
        this.length = length;
        this.instrumentIds = List.copyOf(instrumentIds);
        this.soundTypes = List.copyOf(soundTypes);
        this.performers = List.copyOf(performers);
        this.notes = notes;

        final int lastTick = (notes.length == 0) ? -1 : notes[notes.length - 1][TIMESTAMP];
        tickStarts = new int[lastTick + 2];
        int note = 0;
        for (int tick = 0; tick < tickStarts.length; tick++) {
            while (note < notes.length && notes[note][TIMESTAMP] < tick)
                note++;
            tickStarts[tick] = note;
        }
    }


    public int length() {
        return length;
    }
    public int noteCount() {
        return notes.length;
    }
    public List<Performer> performers() {
        return performers;
    }
    public List<String> instrumentIds() {
        return instrumentIds;
    }
    public List<String> soundTypes() {
        return soundTypes;
    }

    public String instrumentId(final int[] note) {
        return instrumentIds.get(note[INSTRUMENT_ID]);
    }
    public String soundType(final int[] note) {
        return soundTypes.get(note[SOUND_TYPE]);
    }

    /**
     * @return The note at the index, in timestamp order. Not to be modified.
     */
    public int[] note(final int index) {
        return notes[index];
    }

    /**
     * Passes the notes of the tick in the order they were recorded. Not to be modified.
     */
    public void forEachNoteAt(final int tick, final Consumer<int[]> action) {
        if (tick < 0 || tick + 1 >= tickStarts.length)
            return;

        for (int i = tickStarts[tick]; i < tickStarts[tick + 1]; i++)
            action.accept(notes[i]);
    }


    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Recording other))
            return false;

        if (length != other.length || !instrumentIds.equals(other.instrumentIds) || !soundTypes.equals(other.soundTypes)
                || performers.size() != other.performers.size() || !Arrays.deepEquals(notes, other.notes))
            return false;

        for (int i = 0; i < performers.size(); i++) {
            final Performer a = performers.get(i), b = other.performers.get(i);
            if (!a.id().equals(b.id()) || !a.name().equals(b.name()) || !Arrays.equals(a.colors(), b.colors()))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(notes);
    }
}
