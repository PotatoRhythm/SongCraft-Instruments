package com.stump.songcraft_instruments.client.gui.instrument.ukelele;

import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.*;

@OnlyIn(Dist.CLIENT)
public class UkuleleThemeLoader extends InstrumentThemeLoader {
    private Color
        topRowNoteReleasedColor = Color.BLACK,
        topRowLabelReleasedColor = Color.BLACK,
        topRowNotePressedColor = Color.BLACK,
        topRowLabelPressedColor = Color.BLACK
    ;

    public UkuleleThemeLoader(ResourceLocation instrumentId) {
        super(instrumentId);
        addListener(this::loadColorTheme);
    }

    private void loadColorTheme(final JsonObject theme) {
        topRowNoteReleasedColor = getRawNotePressed();
        topRowNotePressedColor = getRawNotePressed();
        topRowLabelPressedColor = getRawLabelPressed();
        topRowLabelReleasedColor = getRawLabelPressed();

        if (!theme.has("ukulele"))
            return;


        final JsonObject ukuleleMeta = theme.getAsJsonObject("ukulele");

        final JsonObject noteMeta = ukuleleMeta.getAsJsonObject("note");
        topRowNoteReleasedColor = getTheme(noteMeta, "released", topRowNoteReleasedColor);
        topRowNotePressedColor = getTheme(noteMeta, "pressed", topRowNotePressedColor);

        final JsonObject labelMeta = ukuleleMeta.getAsJsonObject("label");
        topRowLabelReleasedColor = getTheme(labelMeta, "released", topRowLabelReleasedColor);
        topRowLabelPressedColor = getTheme(labelMeta, "pressed", topRowLabelPressedColor);
    }


    public Color topRowNoteReleasedColor(final NoteButton noteButton) {
        return topRowNoteReleasedColor;
    }
    public Color topRowLabelReleasedColor(final NoteButton noteButton) {
        return topRowLabelReleasedColor;
    }
    public Color topRowNotePressedColor(final NoteButton noteButton) {
        return topRowNotePressedColor;
    }
    public Color topRowLabelPressedColor(final NoteButton noteButton) {
        return topRowLabelPressedColor;
    }

    // Override all defaults for top row
    @Override
    public Color noteReleased(NoteButton noteButton) {
        return overrideTopRow(
            noteButton,
            topRowNoteReleasedColor(noteButton),
            super.noteReleased(noteButton)
        );
    }
    @Override
    public Color notePressed(NoteButton noteButton) {
        return overrideTopRow(
            noteButton,
            topRowNotePressedColor(noteButton),
            super.notePressed(noteButton)
        );
    }
    @Override
    public Color labelReleased(NoteButton noteButton) {
        return overrideTopRow(
            noteButton,
            topRowLabelReleasedColor(noteButton),
            super.labelReleased(noteButton)
        );
    }
    @Override
    public Color labelPressed(NoteButton noteButton) {
        return overrideTopRow(
            noteButton,
            topRowLabelPressedColor(noteButton),
            super.labelPressed(noteButton)
        );
    }

    private Color overrideTopRow(final NoteButton noteButton, final Color newColor, final Color superColor) {
        final UkuleleNoteButton unb = (UkuleleNoteButton) noteButton;

        if (unb.ukuleleScreen().isTopRegular())
            return superColor;

        if (unb.row == 0) {
            return newColor;
        }

        return superColor;
    }
}
