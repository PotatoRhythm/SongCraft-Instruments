package com.stump.songcraft_instruments.client.gui.instrument.flute;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.FluteSoundType;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.gui.instrument.partial.layout.LayoutGridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.windsonglyre.WindsongLyreScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The Flute's note grid, for the flutes, recorders and reeds.
 */
@OnlyIn(Dist.CLIENT)
public class FluteScreen extends LayoutGridInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "flute");

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }

    @Override
    public SoundTypeOption<FluteSoundType> soundTypeOption() {
        return new SoundTypeOption<>(FluteSoundType.values(), ModClientConfigs.FLUTE_SOUND_TYPE,
            "button.songcraft_instruments.flute.soundType");
    }


    @Override
    public InstrumentThemeLoader getThemeLoader() {
        // Genshin-style buttons for now
        return WindsongLyreScreen.THEME_LOADER;
    }
}
