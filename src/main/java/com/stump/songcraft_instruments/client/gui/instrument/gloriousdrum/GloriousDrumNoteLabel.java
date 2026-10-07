package com.stump.songcraft_instruments.client.gui.instrument.gloriousdrum;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.INoteLabel;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.NoteLabelSupplier;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum GloriousDrumNoteLabel implements INoteLabel {
	KEYBOARD_LAYOUT((note) ->
		INoteLabel.upperComponent(dn(note).getKey().getDisplayName())
	),
	QWERTY((note) ->
		INoteLabel.getQwerty(dn(note).getKey())
	),
	DON_KA((note) ->
		Component.translatable(dn(note).btnType.getTransKey())
	),
    NONE(NoteLabelSupplier.EMPTY);


    private final NoteLabelSupplier labelSupplier;
    private GloriousDrumNoteLabel(final NoteLabelSupplier supplier) {
        labelSupplier = supplier;
    }

	public static INoteLabel[] availableVals() {
        return INoteLabel.filterQwerty(values(), ModClientConfigs.GLORIOUS_DRUM_LABEL_TYPE.get(), QWERTY);
    }


	@Override
	public NoteLabelSupplier getLabelSupplier() {
        return labelSupplier;
	}


	private static GloriousDrumNoteButton dn(final NoteButton btn) {
        return (GloriousDrumNoteButton)btn;
    }
}