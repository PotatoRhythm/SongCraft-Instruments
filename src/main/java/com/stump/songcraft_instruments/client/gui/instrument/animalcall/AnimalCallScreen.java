package com.stump.songcraft_instruments.client.gui.instrument.animalcall;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.AnimalCallSoundType;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.gui.instrument.partial.layout.LayoutGridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.windsonglyre.WindsongLyreScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The Animal Call's note grid, for Sky's animal calls.
 */
@OnlyIn(Dist.CLIENT)
public class AnimalCallScreen extends LayoutGridInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "animal_call");

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }

    @Override
    public SoundTypeOption<AnimalCallSoundType> soundTypeOption() {
        return new SoundTypeOption<>(AnimalCallSoundType.values(), ModClientConfigs.ANIMAL_CALL_SOUND_TYPE,
            "button.songcraft_instruments.animal_call.soundType");
    }


    @Override
    public InstrumentThemeLoader getThemeLoader() {
        // Genshin-style buttons for now
        return WindsongLyreScreen.THEME_LOADER;
    }
}
