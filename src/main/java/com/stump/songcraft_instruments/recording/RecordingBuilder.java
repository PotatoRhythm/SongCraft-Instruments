package com.stump.songcraft_instruments.recording;

import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.recording.Recording.Performer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The notes of a recording in progress, kept by the looper recording them until it is finalized with {@link #build}.
 */
public final class RecordingBuilder {
    private final List<String> instrumentIds = new ArrayList<>(), soundTypes = new ArrayList<>(),
        pedalChains = new ArrayList<>();
    private final List<Performer> performers = new ArrayList<>();
    private final List<int[]> notes = new ArrayList<>();

    public RecordingBuilder() {}

    /**
     * Continues an unfinished recording, as saved by the looper
     */
    public RecordingBuilder(final Recording draft) {
        instrumentIds.addAll(draft.instrumentIds());
        soundTypes.addAll(draft.soundTypes());
        pedalChains.addAll(draft.pedalChains());
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
        pedalChains.clear();
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

    /**
     * @param pedalChain The effects pedals the note was played through
     * @param tape The Lo-fi pedal (the tape) it went through, or null
     */
    public void addNote(final int timestamp, final int performer,
                        final int pitch, final int volume, final int particleColor,
                        final String instrumentId, final String soundType, final int soundIndex,
                        final PedalChain pedalChain, @Nullable final UUID tape) {
        notes.add(new int[] {
            timestamp, Recording.TYPE_REGULAR, performer,
            pitch, volume, particleColor,
            paletteIndex(instrumentIds, instrumentId), paletteIndex(soundTypes, soundType), soundIndex,
            paletteIndex(pedalChains, Recording.pedalKey(pedalChain, tape))
        });
    }

    /**
     * @param pedalChain The effects pedals the note was played through
     * @param tape The Lo-fi pedal (the tape) it went through, or null
     * @param phase {@link Recording#PHASE_ATTACK} or {@link Recording#PHASE_RELEASE}
     */
    public void addHeldNote(final int timestamp, final int performer,
                            final int pitch, final int volume, final int particleColor,
                            final String instrumentId, final String soundType, final int soundIndex,
                            final PedalChain pedalChain, @Nullable final UUID tape, final int phase) {
        notes.add(new int[] {
            timestamp, Recording.TYPE_HELD, performer,
            pitch, volume, particleColor,
            paletteIndex(instrumentIds, instrumentId), paletteIndex(soundTypes, soundType), soundIndex,
            paletteIndex(pedalChains, Recording.pedalKey(pedalChain, tape)),
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
     * Lays these notes over {@code base}'s, as when recording a new part over a synced looper's record.
     * Each part keeps its own performers, even when the same player recorded both, so that every part
     * is credited with the colors it was recorded with.
     * @param length The tick the recording loops back at
     */
    public Recording buildOver(final Recording base, final int length) {
        final RecordingBuilder merged = new RecordingBuilder();

        final int[] basePerformers = new int[base.performers().size()];
        for (int i = 0; i < basePerformers.length; i++) {
            merged.performers.add(base.performers().get(i));
            basePerformers[i] = i;
        }
        final int[] newPerformers = new int[performers.size()];
        for (int i = 0; i < newPerformers.length; i++) {
            merged.performers.add(performers.get(i));
            newPerformers[i] = basePerformers.length + i;
        }

        // Added first, so that base notes play before new ones of the same tick
        for (int i = 0; i < base.noteCount(); i++)
            merged.notes.add(merged.remap(base.note(i), basePerformers,
                base.instrumentIds(), base.soundTypes(), base.pedalChains()));
        for (final int[] note : notes)
            merged.notes.add(merged.remap(note, newPerformers, instrumentIds, soundTypes, pedalChains));

        return merged.build(length);
    }

    /**
     * @return A copy of a note from another recording, with its performer and palette indices pointing into this one
     */
    private int[] remap(final int[] note, final int[] performerMap,
                        final List<String> fromInstrumentIds, final List<String> fromSoundTypes,
                        final List<String> fromPedalChains) {
        final int[] copy = note.clone();
        copy[Recording.PERFORMER] = performerMap[note[Recording.PERFORMER]];

        if (note[Recording.TYPE] != Recording.TYPE_DAMPEN) {
            copy[Recording.INSTRUMENT_ID] = paletteIndex(instrumentIds, fromInstrumentIds.get(note[Recording.INSTRUMENT_ID]));
            copy[Recording.SOUND_TYPE] = paletteIndex(soundTypes, fromSoundTypes.get(note[Recording.SOUND_TYPE]));
            copy[Recording.PEDALS] = paletteIndex(pedalChains, fromPedalChains.get(note[Recording.PEDALS]));
        }
        return copy;
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

        return new Recording(length, instrumentIds, soundTypes, pedalChains, performers, sorted.toArray(int[][]::new));
    }
}
