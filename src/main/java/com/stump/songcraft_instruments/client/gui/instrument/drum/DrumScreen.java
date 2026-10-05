package com.stump.songcraft_instruments.client.gui.instrument.drum;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.DrumSoundType;
import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.gui.instrument.partial.layout.LayoutPadScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The Drum's pads, for the drums and cymbals. All of its sound types are played on pads.
 */
@OnlyIn(Dist.CLIENT)
public class DrumScreen extends LayoutPadScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "drum");

    public DrumScreen() {
        super(INSTRUMENT_ID, new SoundTypeOption<>(DrumSoundType.values(), ModClientConfigs.DRUM_SOUND_TYPE,
            "button.songcraft_instruments.drum.soundType"), null, null);
    }
}
