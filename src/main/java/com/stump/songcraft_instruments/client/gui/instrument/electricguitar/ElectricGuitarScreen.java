package com.stump.songcraft_instruments.client.gui.instrument.electricguitar;

import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.ElectricGuitarSoundType;
import com.stump.songcraft_instruments.client.config.enumType.NoteIconStyle;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ElectricGuitarScreen extends GridInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "electric_guitar");

    @Override
    public SoundTypeOption<ElectricGuitarSoundType> soundTypeOption() {
        return new SoundTypeOption<>(ElectricGuitarSoundType.values(), ModClientConfigs.ELECTRIC_GUITAR_SOUND_TYPE,
            "button.songcraft_instruments.electric_guitar.soundType");
    }

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }

    public static final InstrumentThemeLoader THEME_LOADER = new InstrumentThemeLoader(INSTRUMENT_ID);
    @Override
    public InstrumentThemeLoader getThemeLoader() {
        return THEME_LOADER;
    }

    @Override
    public NoteIconStyle getDefaultNoteIconStyle() {
        return NoteIconStyle.JIANPU;
    }
}