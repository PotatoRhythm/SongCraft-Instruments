package com.stump.songcraft_instruments.client.gui.instrument.noteblockinstrument;


import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid.NoteGridButton;

public class NoteBlockInstrumentNote extends NoteGridButton {

    public NoteBlockInstrumentNote(int column, int row, GridInstrumentScreen instrumentScreen, int pitch) {
        super(column, row, instrumentScreen, pitch);
    }

    // Layout starts from the bottom in a note block instrument, not the top
    // Hence, perform a row flip
    @Override
    public int getNoteOffset() {
        final GridInstrumentScreen gridInstrument = (GridInstrumentScreen)instrumentScreen;
        return column + gridInstrument.noteGrid.getFlippedRow(row) * gridInstrument.columns();
    }
    
}
