package com.stump.songcraft_instruments.client.gui.instrument.noteblockinstrument;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.NoteBlockSoundType;
import com.stump.songcraft_instruments.client.gui.instrument.floralzither.FloralZitherScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.gui.instrument.partial.layout.LayoutGridInstrumentScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The Note Block Instrument's note grid, for every tunable note block sound.
 */
@OnlyIn(Dist.CLIENT)
public class NoteBlockInstrumentScreen extends LayoutGridInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "note_block_instrument");

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }

    @Override
    public SoundTypeOption<NoteBlockSoundType> soundTypeOption() {
        return new SoundTypeOption<>(NoteBlockSoundType.values(), ModClientConfigs.NOTE_BLOCK_SOUND_TYPE,
            "button.songcraft_instruments.note_block_instrument.soundType");
    }


    @Override
    public InstrumentThemeLoader getThemeLoader() {
        return FloralZitherScreen.THEME_LOADER;
    }
}
