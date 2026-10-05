package com.stump.songcraft_instruments.client.gui.instrument.noteblockinstrument;

import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.midi.InstrumentMidiReceiver;
import org.jetbrains.annotations.Nullable;

public class NoteBlockInstrumentMIDIReceiver extends InstrumentMidiReceiver {
    public NoteBlockInstrumentMIDIReceiver(NoteBlockInstrumentScreen instrument) {
        super(instrument);
    }

    public NoteBlockInstrumentScreen self() {
        return (NoteBlockInstrumentScreen) instrument;
    }

    @Override
    protected int maxMidiNote() {
        return self().columns() * self().rows() + self().getSSTILowestNote();
    }

    @Override
    protected @Nullable NoteButton handleMidiPress(int note, int key) {
        final NoteBlockInstrumentScreen instrumentScreen = self();

        note -= instrumentScreen.getSSTILowestNote();
        if (note < 0)
            return null;

        return instrumentScreen.getNoteButtonByMIDINote(note);
    }
}
