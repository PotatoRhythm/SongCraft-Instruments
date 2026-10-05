package com.stump.songcraft_instruments.client.gui.instrument.kalimba;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.KalimbaSoundType;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.gui.instrument.partial.layout.LayoutGridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.windsonglyre.WindsongLyreScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The Kalimba's note grid, for the kalimbas and the xylophone.
 */
@OnlyIn(Dist.CLIENT)
public class KalimbaScreen extends LayoutGridInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "kalimba");

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }

    @Override
    public SoundTypeOption<KalimbaSoundType> soundTypeOption() {
        return new SoundTypeOption<>(KalimbaSoundType.values(), ModClientConfigs.KALIMBA_SOUND_TYPE,
            "button.songcraft_instruments.kalimba.soundType");
    }


    @Override
    public InstrumentThemeLoader getThemeLoader() {
        // Genshin-style buttons for now
        return WindsongLyreScreen.THEME_LOADER;
    }
}
