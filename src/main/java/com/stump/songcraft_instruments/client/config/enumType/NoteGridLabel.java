package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid.NoteGridButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.INoteLabel;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.NoteLabelSupplier;
import com.stump.songcraft_instruments.util.LabelUtil;
import net.minecraft.network.chat.Component;

/**
 * An enum holding all labels for {@code NoteGridButton}.
 * When getting from their respected suppliers, it is expected you pass
 * an instance of {@code NoteGridButton}.
 */
public enum NoteGridLabel implements INoteLabel {
    // Shows the keys of the current control mode; notes no key plays as is go unlabeled
    KEYBOARD_LAYOUT((note) -> (ng(note).getKey() == null) ? Component.empty() : INoteLabel.upperComponent(
        ng(note).getKey().getDisplayName()
    )),
    QWERTY((note) -> (ng(note).getKey() == null) ? Component.empty() :
        INoteLabel.getQwerty(ng(note).getKey())
    ),
    
    // The note actually played, following both the transposition keys and the transpose setting
    NOTE_NAME((note) -> Component.literal(
        note.getTransposedNoteName()
    )),
    // Fixed: based on the note's position in the grid, unaffected by transpositions
    DO_RE_MI((note) -> Component.translatable(
        INoteLabel.TRANSLATABLE_PATH + LabelUtil.DO_RE_MI[noteGridIndex(note) % 7]
    )),

    NONE(NoteLabelSupplier.EMPTY);


    /**
     * @return The note button's grid index
     */
    private static int noteGridIndex(final NoteButton note) {
        return ng(note).column + ng(note).row * gs(note).columns();
    }
    

    private final NoteLabelSupplier labelSupplier;
    private NoteGridLabel(final NoteLabelSupplier labelSupplier) {
        this.labelSupplier = labelSupplier;
    }

    public static INoteLabel[] availableVals() {
        return INoteLabel.filterQwerty(values(), ModClientConfigs.GRID_LABEL_TYPE.get(), QWERTY);
    }


    @Override
    public NoteLabelSupplier getLabelSupplier() {
        return labelSupplier;
    }
    

    private static NoteGridButton ng(final NoteButton btn) {
        return (NoteGridButton)btn;
    }
    private static GridInstrumentScreen gs(final NoteButton btn) {
        return ng(btn).gridInstrument();
    }
}