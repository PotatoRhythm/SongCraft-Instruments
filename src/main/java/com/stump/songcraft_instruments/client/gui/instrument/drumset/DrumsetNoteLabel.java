package com.stump.songcraft_instruments.client.gui.instrument.drumset;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.INoteLabel;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.NoteLabelSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum DrumsetNoteLabel implements INoteLabel {
    KEYBOARD_LAYOUT((note) ->
            INoteLabel.upperComponent(getDrumsetNoteButton(note).getKey().getDisplayName())
    ),
    QWERTY((note) ->
            INoteLabel.getQwerty(getDrumsetNoteButton(note).getKey())
    ),

    NONE(NoteLabelSupplier.EMPTY);


    private final NoteLabelSupplier labelSupplier;
    DrumsetNoteLabel(final NoteLabelSupplier supplier) {
        labelSupplier = supplier;
    }

    public static INoteLabel[] availableVals() {
        return INoteLabel.filterQwerty(values(), ModClientConfigs.DRUMSET_LABEL_TYPE.get(), QWERTY);
    }


    @Override
    public NoteLabelSupplier getLabelSupplier() {
        return labelSupplier;
    }


    private static DrumsetNoteButton getDrumsetNoteButton(final NoteButton btn) {
        return (DrumsetNoteButton)btn;
    }
}