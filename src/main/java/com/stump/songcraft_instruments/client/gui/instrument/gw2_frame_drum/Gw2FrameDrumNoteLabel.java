package com.stump.songcraft_instruments.client.gui.instrument.gw2_frame_drum;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.INoteLabel;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.NoteLabelSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum Gw2FrameDrumNoteLabel implements INoteLabel {
	KEYBOARD_LAYOUT((note) ->
		INoteLabel.upperComponent(getGw2FrameDrumNoteButton(note).getKey().getDisplayName())
	),
	QWERTY((note) ->
		INoteLabel.getQwerty(getGw2FrameDrumNoteButton(note).getKey())
	),
    NONE(NoteLabelSupplier.EMPTY);


    private final NoteLabelSupplier labelSupplier;
	Gw2FrameDrumNoteLabel(final NoteLabelSupplier supplier) {
        labelSupplier = supplier;
    }

	public static INoteLabel[] availableVals() {
        return INoteLabel.filterQwerty(values(), ModClientConfigs.GW2_FRAME_DRUM_LABEL_TYPE.get(), QWERTY);
    }


	@Override
	public NoteLabelSupplier getLabelSupplier() {
        return labelSupplier;
	}


	private static Gw2FrameDrumNoteButton getGw2FrameDrumNoteButton(final NoteButton btn) {
        return (Gw2FrameDrumNoteButton)btn;
    }
}