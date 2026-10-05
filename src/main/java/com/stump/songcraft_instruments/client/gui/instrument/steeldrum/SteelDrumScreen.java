package com.stump.songcraft_instruments.client.gui.instrument.steeldrum;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.SteelDrumSoundType;
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
 * The Steel Drum's note grid, for the hang drums, hand pans and other tuned percussion.
 * Choosing a sound played on pads switches over to its pads.
 */
@OnlyIn(Dist.CLIENT)
public class SteelDrumScreen extends LayoutGridInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "steel_drum");

    /**
     * @return The screen for the chosen sound type: the pads or the note grid
     */
    public static InstrumentScreen create() {
        return LayoutGridInstrumentScreen.create(new SteelDrumScreen());
    }

    public SteelDrumScreen() {}
    public SteelDrumScreen(final SoundType soundType) {
        super(soundType);
    }

    @Override
    protected InstrumentScreen newGridScreen(final SoundType soundType) {
        return new SteelDrumScreen(soundType);
    }

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }

    @Override
    public SoundTypeOption<SteelDrumSoundType> soundTypeOption() {
        return new SoundTypeOption<>(SteelDrumSoundType.values(), ModClientConfigs.STEEL_DRUM_SOUND_TYPE,
            "button.songcraft_instruments.steel_drum.soundType");
    }


    @Override
    public InstrumentThemeLoader getThemeLoader() {
        // Genshin-style buttons for now
        return WindsongLyreScreen.THEME_LOADER;
    }
}
