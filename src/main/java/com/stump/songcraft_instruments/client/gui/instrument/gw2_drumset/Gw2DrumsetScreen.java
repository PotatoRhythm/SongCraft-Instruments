package com.stump.songcraft_instruments.client.gui.instrument.gw2_drumset;

import com.mojang.blaze3d.platform.InputConstants.Key;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.options.partial.InstrumentOptionsScreen;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class Gw2DrumsetScreen extends InstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "gw2_drumset");

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }


    /**
     * Maps keycodes to their respected note button
     */
    private final HashMap<Key, NoteButton> notes = new HashMap<>();
    @Override
    public Map<Key, NoteButton> getNoteMap() {
        return notes;
    }

    @Override
    protected InstrumentOptionsScreen initInstrumentOptionsScreen() {
        return new Gw2DrumsetOptionsScreen(this);
    }


    private final Gw2DrumsetNoteButton[] drumsetButtons =
            new Gw2DrumsetNoteButton[15];

    @Override
    protected void init() {
        initControlBar(height / 2 - 20);

        for (int i = 0; i < drumsetButtons.length; i++) {
            Gw2DrumsetNoteButton button = new Gw2DrumsetNoteButton(this, i);
            drumsetButtons[i] = button;

            if (button.getKey() != null)
                notes.put(button.getKey(), button);
            addRenderableWidget(button);
        }

        layoutButtons();

        for (Gw2DrumsetNoteButton button : drumsetButtons)
            button.init();

        super.init();
    }

    private void layoutButtons() {

        int size = getNoteSize();
        int gap = 20;
        int spacing = size + gap;

        int row3Y = (int) (height * 0.80f);
        int row2Y = row3Y - size + 2;
        int row1Y = row2Y - size - 10;

        // bottom row
        int bottomWidth = 6 * size + 5 * gap;
        int bottomStartX = (width - bottomWidth) / 2;

        for (int i = 0; i < 6; i++) {
            drumsetButtons[i].setPosition(
                    bottomStartX + i * (size + gap),
                    row3Y
            );
        }

        // middle row
        int[] midBottomIndices = {0, 1, 3, 4};

        for (int i = 0; i < 4; i++) {
            int bottomIndex = midBottomIndices[i];

            drumsetButtons[6 + i].setPosition(
                    bottomStartX + bottomIndex * spacing + spacing / 2,
                    row2Y
            );
        }

        // top row
        int[] topBottomIndices = {0, 1, 3, 4, 5};

        for (int i = 0; i < 5; i++) {
            int bottomIndex = topBottomIndices[i];

            drumsetButtons[10 + i].setPosition(
                    bottomStartX + bottomIndex * spacing,
                    row1Y
            );
        }
    }
    public static final InstrumentThemeLoader THEME_LOADER = new InstrumentThemeLoader(INSTRUMENT_ID);
    @Override
    public InstrumentThemeLoader getThemeLoader() {
        return THEME_LOADER;
    }
}