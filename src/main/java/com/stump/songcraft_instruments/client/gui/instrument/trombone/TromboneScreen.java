package com.stump.songcraft_instruments.client.gui.instrument.trombone;

import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.TromboneSoundType;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.gui.instrument.nightwind_horn.NightwindHornScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.IHeldInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.layout.LayoutGridInstrumentScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TromboneScreen extends LayoutGridInstrumentScreen implements IHeldInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "trombone");

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }

    @Override
    public SoundTypeOption<TromboneSoundType> soundTypeOption() {
        return new SoundTypeOption<>(TromboneSoundType.values(), ModClientConfigs.TROMBONE_SOUND_TYPE,
            "button.songcraft_instruments.trombone.soundType");
    }

    public static final InstrumentThemeLoader THEME_LOADER = InstrumentThemeLoader.fromOther(
            NightwindHornScreen.THEME_LOADER,
            INSTRUMENT_ID
    );

    @Override
    public InstrumentThemeLoader getThemeLoader() {
        return THEME_LOADER;
    }
}