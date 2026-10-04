package com.stump.songcraft_instruments.client.gui.instrument.ukelele;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.NoteGrid;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid.NoteGridButton;
import com.stump.songcraft_instruments.client.gui.options.partial.InstrumentOptionsScreen;
import com.stump.songcraft_instruments.client.midi.InstrumentMidiReceiver;
import com.stump.songcraft_instruments.sound.SCSounds;
import com.mojang.blaze3d.systems.RenderSystem;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UkuleleScreen extends GridInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "ukulele");

    private static final String[] NOTE_LAYOUT = {
        "C", "Dm", "Em", "F", "G", "Am", "G7",
        "C", "D", "E", "F", "G", "A", "B",
        "C", "D", "E", "F", "G", "A", "B"
    };

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }


    @Override
    public String[] noteLayout() {
        return NOTE_LAYOUT;
    }

    @Override
    public SoundOption getSoundOption() {
        return new SoundOption(SCSounds.UKULELE);
    }


    public Ukulele3rdOctaveType octaveType = ModClientConfigs.UKULELE_3RD_OCTAVE_TYPE.get();

    /**
     * @return Whether the 3rd octave is not used as the chord octave,
     * but a regular 3rd octave.
     */
    public boolean isTopRegular() {
        return octaveType == Ukulele3rdOctaveType.TREBLE;
    }


    @Override
    public NoteGridButton createNoteButton(int row, int column) {
        return new UkuleleNoteButton(row, column, this);
    }

    @Override
    protected InstrumentOptionsScreen initInstrumentOptionsScreen() {
        return new UkuleleOptionsScreen(this);
    }

    public static final UkuleleThemeLoader THEME_LOADER = new UkuleleThemeLoader(INSTRUMENT_ID);
    @Override
    public UkuleleThemeLoader getThemeLoader() {
        return THEME_LOADER;
    }


    @Override
    public InstrumentMidiReceiver initMidiReceiver() {
        return new UkuleleMidiReceiver(this);
    }

    // Render the chord "clef" atop and the rest to go down an octave.
    @Override
    protected void renderClef(GuiGraphics gui, int index, int x, String clefName) {
        if (isTopRegular()) {
            super.renderClef(gui, index, x, clefName);
            return;
        }

        switch (index) {
            case 0 -> renderClefChord(gui, index, x);
            case 1 -> super.renderClef(gui, index, x, "treble");
            case 2 -> super.renderClef(gui, index, x, "alto");
        }
    }

    private void renderClefChord(GuiGraphics gui, int index, int x) {
        RenderSystem.enableBlend();

        gui.blit(getResourceFromRoot("background/clef/chord.png"),
            x, grid.getY() + NoteGrid.getPaddingVert() + getLayerAddition(index) - 5,
            0, 0,

            CLEF_WIDTH, CLEF_HEIGHT,
            CLEF_WIDTH, CLEF_HEIGHT
        );

        RenderSystem.disableBlend();
    }
}
