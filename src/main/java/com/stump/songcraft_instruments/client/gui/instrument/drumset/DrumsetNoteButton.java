package com.stump.songcraft_instruments.client.gui.instrument.drumset;

import com.mojang.blaze3d.platform.InputConstants.Key;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.render.NoteButtonRenderer;
import com.stump.songcraft_instruments.client.keyMaps.InstrumentKeyMappings;
import com.stump.songcraft_instruments.networking.buttonidentifier.DrumsetNoteIdentifier;
import com.stump.songcraft_instruments.networking.buttonidentifier.NoteButtonIdentifier;

public class DrumsetNoteButton extends NoteButton {

    public final int index;

    public DrumsetNoteButton(DrumsetScreen screen, int index) {
        super(
                screen.getSoundOption().getNoteSounds()[index],
                ModClientConfigs.DRUMSET_LABEL_TYPE.get().getLabelSupplier(),
                screen
        );

        this.index = index;
    }

    public Key getKey() {
        return InstrumentKeyMappings.DRUMSET_MAPPINGS[index].getKey();
    }

    @Override
    public NoteButtonIdentifier getIdentifier() {
        return new DrumsetNoteIdentifier(this);
    }

    @Override
    public int getNoteOffset() {
        return index;
    }

    @Override
    protected NoteButtonRenderer initNoteRenderer() {
        return new NoteButtonRenderer(this, () ->
                instrumentScreen.getResourceFromRoot(
                        "note/label/" + (index % 4) + ".png",
                        false
                )
        );
    }
}