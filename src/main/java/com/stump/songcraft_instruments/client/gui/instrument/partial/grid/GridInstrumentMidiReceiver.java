package com.stump.songcraft_instruments.client.gui.instrument.partial.grid;

import com.mojang.logging.LogUtils;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid.NoteGridButton;
import com.stump.songcraft_instruments.client.midi.InstrumentMidiReceiver;
import com.stump.songcraft_instruments.client.midi.MidiOverflowResult;
import com.stump.songcraft_instruments.client.midi.PressedMIDINote;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.util.LabelUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class GridInstrumentMidiReceiver extends InstrumentMidiReceiver {

    public GridInstrumentMidiReceiver(GridInstrumentScreen instrument) {
        super(instrument);
    }
    protected GridInstrumentScreen gridInstrument() {
        return (GridInstrumentScreen) instrument;
    }

    @Override
    public boolean allowMidiOverflow() {
        return true;
    }

    protected int maxMidiNote() {
        return LabelUtil.NOTES_PER_SCALE * gridInstrument().rows();
    }

    @Override
    protected NoteButton getHighestNote() {
        return gridInstrument().getNoteButton(gridInstrument().columns() - 1, gridInstrument().rows() - 1);
    }
    @Override
    protected NoteButton getLowestNote() {
        return gridInstrument().getNoteButton(0, 0);
    }


    @Override
    protected @Nullable NoteButton handleMidiPress(int note, int key) {
        final GridInstrumentScreen instrumentScreen = (GridInstrumentScreen) instrument;

        final int layoutNote = note % 12;
        final boolean higherThan3 = layoutNote > key + 4;

        // Handle transposition
        final boolean shouldSharpen = shouldSharpen(layoutNote, key);
        final boolean shouldFlatten = shouldFlatten(shouldSharpen);

        transposeMidi(shouldSharpen, shouldFlatten);

        // A sharpened/flattened note is still the same note - just pitched up/down.
        // Thus, go backwards/forwards to stay on the same note.

        int playedNote = note + (shouldFlatten ? 1 : shouldSharpen ? -1 : 0);

        playedNote = ((playedNote + (higherThan3 ? 1 : 0)) / 2)
            // 12th note should go to the next row
            + playedNote / (12 + key);

        return instrumentScreen.getNoteButtonByMIDINote(playedNote);
    }

    @Override
    public PressedMIDINote playNote(NoteButton noteBtn, @Nullable MidiOverflowResult midiOverflow, int basePitch) {
        if (midiOverflow == null) {
            noteBtn.play();

            if (noteBtn instanceof NoteGridButton gridButton) {
                return new PressedMIDINote(
                        gridButton.getLastPlayedPitch(),
                        noteBtn,
                        gridButton.getLastPlayedSound()
                );
            }

            return new PressedMIDINote(
                    noteBtn.getPitch(),
                    noteBtn,
                    noteBtn.getSound()
            );
        }

        final GridInstrumentScreen screen = gridInstrument();
        final NoteSound[] sounds = screen.getInitSounds();

        if (sounds == null || sounds.length == 0) {
            int newPitch = NoteSound.clampPitch(
                    basePitch
                            + midiOverflow.pitchOffset()
                            + ModClientConfigs.TRANSPOSE.get()
            );

            noteBtn.play(midiOverflow.newNoteSound(), newPitch);

            return new PressedMIDINote(
                    newPitch,
                    noteBtn,
                    midiOverflow.newNoteSound()
            );
        }

        final int overflowSoundPitch =
                midiOverflow.type() == MidiOverflowResult.OverflowType.BOTTOM
                        ? getSampleChromaticPitch(0)
                        : getSampleChromaticPitch(sounds.length - 1);

        final int targetPitch =
                overflowSoundPitch
                        + midiOverflow.pitchOffset()
                        + basePitch
                        + ModClientConfigs.TRANSPOSE.get();

        // Find the available sample closest to the desired pitch.
        NoteSound closestSound = null;
        int closestPitch = 0;
        int closestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < sounds.length; i++) {
            if ((noteBtn instanceof NoteGridButton gridButton) && !gridButton.canTransposeTo(i))
                continue;

            final int samplePitch = getSampleChromaticPitch(i);
            final int distance = Math.abs(targetPitch - samplePitch);

            if (distance < closestDistance) {
                closestSound = sounds[i];
                closestPitch = samplePitch;
                closestDistance = distance;
            }
        }

        if (closestSound == null)
            return null;

        int newPitch = targetPitch - closestPitch;
        newPitch = NoteSound.clampPitch(newPitch);

        noteBtn.play(closestSound, newPitch);

        return new PressedMIDINote(newPitch, noteBtn, closestSound);
    }

    private int getSampleChromaticPitch(int index) {
        final int[] naturalNotePitches = {
                0, 2, 4, 5, 7, 9, 11
        };

        final int column = index % gridInstrument().columns();
        final int row = index / gridInstrument().columns();

        return row * 12 + naturalNotePitches[column];
    }
}
