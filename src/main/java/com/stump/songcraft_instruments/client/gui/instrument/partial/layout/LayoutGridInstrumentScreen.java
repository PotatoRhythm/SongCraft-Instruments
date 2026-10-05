package com.stump.songcraft_instruments.client.gui.instrument.partial.layout;

import com.stump.songcraft_instruments.client.config.enumType.LayoutSoundType;
import com.stump.songcraft_instruments.client.config.enumType.SoundType;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentScreen;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A note grid instrument whose sound types come in different layouts, as given by their {@link LayoutSoundType}:
 * the number of columns follows the chosen sound type, and choosing one played on pads
 * switches over to a {@link LayoutPadScreen}.
 */
@OnlyIn(Dist.CLIENT)
public abstract class LayoutGridInstrumentScreen extends GridInstrumentScreen {

    public LayoutGridInstrumentScreen() {}
    /**
     * @param soundType The sound type to start with, for when switching over from the pads
     */
    public LayoutGridInstrumentScreen(final SoundType soundType) {
        initPreferredSoundType(soundType);
    }

    /**
     * @return The given note grid, or the instrument's pads if its chosen sound type is played on them
     */
    public static InstrumentScreen create(final LayoutGridInstrumentScreen grid) {
        final SoundType soundType = grid.getPreferredSoundType();
        return isPads(soundType) ? grid.padScreen(soundType) : grid;
    }

    /**
     * A new note grid of this instrument, starting with the given sound type.
     * Only instruments with sound types played on pads need it, for switching back from them.
     */
    protected InstrumentScreen newGridScreen(final SoundType soundType) {
        throw new UnsupportedOperationException(getInstrumentId() + " has no sound types played on pads");
    }

    /**
     * @return This instrument's pads, starting with the given sound type
     */
    public LayoutPadScreen padScreen(final SoundType soundType) {
        return new LayoutPadScreen(getInstrumentId(), soundTypeOption(), this::newGridScreen, soundType);
    }


    static boolean isPads(final @Nullable SoundType soundType) {
        return (soundType instanceof LayoutSoundType layout) && layout.isPads();
    }

    /**
     * Switches between the note grid and the pads, handing the chosen sound type over directly:
     * the configs only hold it once the queued options are saved.
     */
    @SuppressWarnings("unchecked")
    static <T extends Enum<T> & SoundType> void switchLayout(final InstrumentScreen from, final SoundType soundType,
                                                             final Supplier<InstrumentScreen> to) {
        final SoundTypeOption<T> option = (SoundTypeOption<T>) from.soundTypeOption();
        from.optionsScreen.queueToSave(from.getInstrumentId().getPath() + "_sound_type",
            () -> option.config().set((T) soundType));
        from.replaceWith(to);
    }


    @Override
    public int columns() {
        return (getPreferredSoundType() instanceof LayoutSoundType layout && !layout.isPads())
            ? layout.columns()
            : DEF_COLUMNS;
    }

    @Override
    public void setPreferredSoundType(final SoundType soundType) {
        if (isPads(soundType)) {
            closeHeldScreen();
            switchLayout(this, soundType, () -> padScreen(soundType));
            return;
        }

        super.setPreferredSoundType(soundType);
    }

    @Override
    public void setSoundOption(final SoundOption option) {
        // The new sound type may have a different number of columns
        updateOctaveRange();
        super.setSoundOption(option);
    }

    @Override
    protected void renderInstrumentBackground(final GuiGraphics gui) {
        if (columns() == DEF_COLUMNS) {
            super.renderInstrumentBackground(gui);
            return;
        }

        final int clefX = grid.getX() - getNoteSize() + 8;

        renderClef(gui, 0, clefX, "treble");
        if (columns() == 2)
            renderClef(gui, 1, clefX, "bass");

        for (int i = 0; i < columns(); i++)
            renderStaff(gui, i);
    }
}
