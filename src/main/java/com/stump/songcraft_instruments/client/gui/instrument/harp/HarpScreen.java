package com.stump.songcraft_instruments.client.gui.instrument.harp;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.HarpSoundType;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.gui.instrument.partial.layout.LayoutGridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.windsonglyre.WindsongLyreScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The Harp's note grid, for the harps and lyres.
 */
@OnlyIn(Dist.CLIENT)
public class HarpScreen extends LayoutGridInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "harp");

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }

    @Override
    public SoundTypeOption<HarpSoundType> soundTypeOption() {
        return new SoundTypeOption<>(HarpSoundType.values(), ModClientConfigs.HARP_SOUND_TYPE,
            "button.songcraft_instruments.harp.soundType");
    }


    @Override
    public InstrumentThemeLoader getThemeLoader() {
        // Genshin-style buttons for now
        return WindsongLyreScreen.THEME_LOADER;
    }
}
