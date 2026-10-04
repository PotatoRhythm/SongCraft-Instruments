package com.stump.songcraft_instruments.client.gui.instrument.drumset;

import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.DrumsetSoundType;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.options.partial.InstrumentOptionsScreen;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class DrumsetScreen extends InstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "drumset");

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
        return new DrumsetOptionsScreen(this);
    }

    @Override
    public SoundTypeOption<DrumsetSoundType> soundTypeOption() {
        return new SoundTypeOption<>(DrumsetSoundType.values(), ModClientConfigs.DRUMSET_SOUND_TYPE,
            "button.songcraft_instruments.drumset.soundType");
    }

    @Override
    public void setSoundOption(SoundOption option) {
        super.setSoundOption(option);

        final NoteSound[] sounds = option.getNoteSounds();
        for (int i = 0; i < drumsetButtons.length; i++) {
            if (drumsetButtons[i] != null)
                drumsetButtons[i].setSound(sounds[i]);
        }
    }



    private final DrumsetNoteButton[] drumsetButtons =
            new DrumsetNoteButton[15];

    @Override
    protected void init() {
        initControlBar(height / 2 - 20);

        for (int i = 0; i < drumsetButtons.length; i++) {
            DrumsetNoteButton button = new DrumsetNoteButton(this, i);
            drumsetButtons[i] = button;

            if (button.getKey() != null)
                notes.put(button.getKey(), button);
            addRenderableWidget(button);
        }

        layoutButtons();

        for (DrumsetNoteButton button : drumsetButtons)
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