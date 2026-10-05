package com.stump.songcraft_instruments.client.gui.instrument.partial.note.render;

import com.stump.songcraft_instruments.client.gui.instrument.gw2_drumset.Gw2DrumsetNoteButton;
import com.stump.songcraft_instruments.util.CommonUtil;

public class Gw2DrumsetNoteButtonRenderer extends NoteButtonRenderer {

    private static final String[] NOTE_LETTERS = {
            "c", "d", "e", "f", "g", "b", "c", "d",
            "f", "a", "c_sharp", "d_sharp", "f_sharp", "g_sharp", "a_sharp"
    };

    public Gw2DrumsetNoteButtonRenderer(Gw2DrumsetNoteButton noteButton) {
        super(noteButton, null);

        int index = noteButton.index;
        String noteLetter = NOTE_LETTERS[index];

        gw2AccidentalsLocation = CommonUtil.getResourceFrom(GW2_NOTE_ROOT, "accidentals_" + noteLetter + ".png");
        gw2ReleasedLocation = CommonUtil.getResourceFrom(GW2_NOTE_ROOT, "released_" + noteLetter + ".png");
        gw2PressedLocation = CommonUtil.getResourceFrom(GW2_NOTE_ROOT, "pressed_" + noteLetter + ".png");
        gw2HoverLocation = CommonUtil.getResourceFrom(GW2_NOTE_ROOT, "hovered_" + noteLetter + ".png");
    }
}