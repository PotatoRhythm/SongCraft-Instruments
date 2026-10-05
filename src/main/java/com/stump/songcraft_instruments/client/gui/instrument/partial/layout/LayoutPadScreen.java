package com.stump.songcraft_instruments.client.gui.instrument.partial.layout;

import com.stump.songcraft_instruments.client.config.enumType.LayoutSoundType;
import com.stump.songcraft_instruments.client.config.enumType.SoundType;
import com.stump.songcraft_instruments.client.gui.instrument.djemdjemdrum.DjemDjemDrumNoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.djemdjemdrum.DjemDjemDrumOptionsScreen;
import com.stump.songcraft_instruments.client.gui.instrument.djemdjemdrum.DjemDjemDrumScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.options.partial.InstrumentOptionsScreen;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The pads of an instrument with {@link LayoutSoundType layout sound types}, for its drums, bells, hand pans and the like.
 * The notes are laid out in 2 rows, lowest first from the top left: 2x4, 2x3 or 2x2.
 * {@link LayoutSoundType#DRUM_PADS} sound types are laid out like the Djem Djem Drum's pads instead.
 * Choosing a sound played on the note grid switches back to the instrument's {@link LayoutGridInstrumentScreen}.
 */
@OnlyIn(Dist.CLIENT)
public class LayoutPadScreen extends InstrumentScreen {
    private static final int ROWS = 2;

    private final ResourceLocation instrumentId;
    private final SoundTypeOption<?> soundTypeOption;
    private final @Nullable Function<SoundType, InstrumentScreen> gridScreen;

    /**
     * @param gridScreen Makes the instrument's note grid with a given sound type, for switching back to it.
     *                   Null for instruments only played on pads.
     * @param soundType The sound type to start with, or null for the one saved in the configs
     */
    public LayoutPadScreen(final ResourceLocation instrumentId, final SoundTypeOption<?> soundTypeOption,
                           final @Nullable Function<SoundType, InstrumentScreen> gridScreen,
                           final @Nullable SoundType soundType) {
        this.instrumentId = instrumentId;
        this.soundTypeOption = soundTypeOption;
        this.gridScreen = gridScreen;

        if (soundType != null)
            initPreferredSoundType(soundType);
    }

    @Override
    public ResourceLocation getInstrumentId() {
        return instrumentId;
    }

    @Override
    public SoundTypeOption<?> soundTypeOption() {
        return soundTypeOption;
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
        return new DjemDjemDrumOptionsScreen(this);
    }

    @Override
    protected void init() {
        notes.clear();
        initControlBar(height/2 + 10);

        final NoteSound[] sounds = getSoundOption().getNoteSounds();
        final boolean drumLayout = (getPreferredSoundType() instanceof LayoutSoundType layout)
            && (layout.columns() == LayoutSoundType.DRUM_PADS);
        final int columns = sounds.length / ROWS;
        final int gap = getNoteSize() / 2;
        final int rowWidth = columns * getNoteSize() + (columns - 1) * gap;
        final int bottomY = (int)(height * .8f);

        for (int row = 0; row < ROWS; row++) {
            final int y = bottomY - (ROWS - 1 - row) * (getNoteSize() + 10);

            for (int column = 0; column < columns; column++) {
                // The Djem Djem Drum's pads come in mirrored pairs, and count their rows from the bottom
                final int padColumn = drumLayout ? (column ^ 1) : column;
                final int index = drumLayout
                    ? padColumn + (ROWS - 1 - row) * columns
                    : row * columns + column;
                final Pad pad = new Pad(this, row, padColumn, index, sounds[index]);

                pad.setPosition((width - rowWidth) / 2 + column * (getNoteSize() + gap), y);
                addRenderableWidget(pad);
                notes.put(pad.getKey(), pad);
            }
        }

        // Initialize all the notes
        notesIterable().forEach(NoteButton::init);

        super.init();
    }

    @Override
    public void setPreferredSoundType(final SoundType soundType) {
        if (!LayoutGridInstrumentScreen.isPads(soundType) && (gridScreen != null)) {
            LayoutGridInstrumentScreen.switchLayout(this, soundType, () -> gridScreen.apply(soundType));
            return;
        }

        super.setPreferredSoundType(soundType);
        // The new sound may have a different number of pads
        rebuildWidgets();
    }


    @Override
    public InstrumentThemeLoader getThemeLoader() {
        return DjemDjemDrumScreen.THEME_LOADER;
    }


    /**
     * A Djem Djem Drum pad playing one of the chosen sound's notes
     */
    private static class Pad extends DjemDjemDrumNoteButton {
        private final int soundIndex;

        /**
         * @param row The pad's row, from the top
         */
        public Pad(final InstrumentScreen screen, final int row, final int column, final int soundIndex, final NoteSound sound) {
            // The drum's rows count from the bottom, which picks the keyboard row
            super(screen, ROWS - 1 - row, column);
            this.soundIndex = soundIndex;
            setSound(sound);
        }

        @Override
        public int getNoteOffset() {
            return soundIndex;
        }
    }
}
