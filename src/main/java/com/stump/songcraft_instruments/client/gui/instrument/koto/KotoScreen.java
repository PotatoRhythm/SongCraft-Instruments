package com.stump.songcraft_instruments.client.gui.instrument.koto;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.gui.instrument.pipa.PipaScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.layout.LayoutGridInstrumentScreen;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.KotoSoundType;
import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KotoScreen extends LayoutGridInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "koto");

    @Override
    public SoundTypeOption<KotoSoundType> soundTypeOption() {
        return new SoundTypeOption<>(KotoSoundType.values(), ModClientConfigs.KOTO_SOUND_TYPE,
            "button.songcraft_instruments.koto.soundType");
    }

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }

    public static final InstrumentThemeLoader THEME_LOADER = InstrumentThemeLoader.fromOther(
        PipaScreen.THEME_LOADER,
        INSTRUMENT_ID
    );
    @Override
    public InstrumentThemeLoader getThemeLoader() {
        return THEME_LOADER;
    }
    
}