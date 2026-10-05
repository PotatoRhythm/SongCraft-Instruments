package com.stump.songcraft_instruments.client.gui.instrument.ukelele;

import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.render.NoteButtonRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class UkuleleNoteButtonRenderer extends NoteButtonRenderer {
    protected final ResourceLocation
        topRowNotePressedLocation,
        topRowNoteReleasedLocation,
        topRowNoteHoverLocation
    ;

    public UkuleleNoteButtonRenderer(NoteButton noteButton, Supplier<ResourceLocation> labelTextureProvider) {
        super(noteButton, labelTextureProvider);

        topRowNotePressedLocation = getResourceFromRoot("note/top_pressed.png");
        topRowNoteReleasedLocation = getResourceFromRoot("note/top_released.png");
        topRowNoteHoverLocation = getResourceFromRoot("note/top_hovered.png");
    }


    private UkuleleNoteButton getButton() {
        return (UkuleleNoteButton) noteButton;
    }


    @Override
    protected ResourceLocation getNoteReleasedLocation() {
        return getTopRowOverride(topRowNoteReleasedLocation, super.getNoteReleasedLocation());
    }
    @Override
    protected ResourceLocation getNotePressedLocation() {
        return getTopRowOverride(topRowNotePressedLocation, super.getNotePressedLocation());
    }
    @Override
    protected ResourceLocation getNoteHoverLocation() {
        return getTopRowOverride(topRowNoteHoverLocation, super.getNoteHoverLocation());
    }


    private ResourceLocation getTopRowOverride(final ResourceLocation newLocation, final ResourceLocation superLocation) {
        if (getButton().ukuleleScreen().isTopRegular())
            return superLocation;

        if (getButton().row == 0) {
            return newLocation;
        }

        return superLocation;
    }


    @Override
    protected void renderNoteSymbol(GuiGraphics gui, InstrumentThemeLoader themeLoader) {
        if (getButton().ukuleleScreen().isTopRegular()) {
            super.renderNoteSymbol(gui, themeLoader);
            return;
        }

        if (getButton().row != 0) {
            super.renderNoteSymbol(gui, themeLoader);
            return;
        }

        final int noteWidth = noteButton.getWidth(), noteHeight = noteButton.getHeight();
        final int noteX = noteButton.getX(), noteY = noteButton.getY();

        final float scaleMultiplier = noteButton.getWidth() / ((float)noteButton.instrumentScreen.getNoteSize()/2);

        gui.pose().pushPose();
        gui.pose().scale(scaleMultiplier, scaleMultiplier, scaleMultiplier);

        gui.drawCenteredString(
            MINECRAFT.font, getButton().getChordNameOfColumn(),
            (int)((noteX + noteWidth/2f) / scaleMultiplier),
            (int)((noteY + noteHeight/4f + 2) / scaleMultiplier),

            ((noteButton.isPlaying() && !foreignPlaying)
                ? themeLoader.labelPressed(noteButton)
                : themeLoader.labelReleased(noteButton)
            ).getRGB()
        );

        gui.pose().popPose();
    }
}
