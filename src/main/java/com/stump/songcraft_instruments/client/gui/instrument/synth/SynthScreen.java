package com.stump.songcraft_instruments.client.gui.instrument.synth;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.SynthSoundType;
import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.gui.instrument.partial.layout.LayoutPadScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The Synth's pads, for Sky's synths. All of its sound types are played on pads.
 */
@OnlyIn(Dist.CLIENT)
public class SynthScreen extends LayoutPadScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "synth");

    public SynthScreen() {
        super(INSTRUMENT_ID, new SoundTypeOption<>(SynthSoundType.values(), ModClientConfigs.SYNTH_SOUND_TYPE,
            "button.songcraft_instruments.synth.soundType"), null, null);
    }
}
