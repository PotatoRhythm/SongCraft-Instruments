package com.stump.songcraft_instruments.client.gui.instrument.drum;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.DrumSoundType;
import com.stump.songcraft_instruments.client.config.enumType.SoundType;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.gui.instrument.partial.layout.LayoutGridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.windsonglyre.WindsongLyreScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The Drum's note grid, for the Marching Drums (FF).
 * Its other drums and cymbals are played on pads: choosing one switches over to them.
 */
@OnlyIn(Dist.CLIENT)
public class DrumScreen extends LayoutGridInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "drum");

    /**
     * @return The screen for the chosen sound type: the pads or the note grid
     */
    public static InstrumentScreen create() {
        return LayoutGridInstrumentScreen.create(new DrumScreen());
    }

    public DrumScreen() {}
    public DrumScreen(final SoundType soundType) {
        super(soundType);
    }

    @Override
    protected InstrumentScreen newGridScreen(final SoundType soundType) {
        return new DrumScreen(soundType);
    }

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }

    @Override
    public SoundTypeOption<DrumSoundType> soundTypeOption() {
        return new SoundTypeOption<>(DrumSoundType.values(), ModClientConfigs.DRUM_SOUND_TYPE,
            "button.songcraft_instruments.drum.soundType");
    }


    @Override
    public InstrumentThemeLoader getThemeLoader() {
        // Genshin-style buttons for now
        return WindsongLyreScreen.THEME_LOADER;
    }
}
