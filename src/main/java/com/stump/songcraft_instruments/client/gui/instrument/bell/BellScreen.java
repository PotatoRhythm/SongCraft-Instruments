package com.stump.songcraft_instruments.client.gui.instrument.bell;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.BellSoundType;
import com.stump.songcraft_instruments.client.config.enumType.SoundType;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.gui.instrument.partial.layout.LayoutGridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.windsonglyre.WindsongLyreScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The Bell's note grid, for the bells and chimes.
 * Choosing a sound played on pads switches over to its pads.
 */
@OnlyIn(Dist.CLIENT)
public class BellScreen extends LayoutGridInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "bell");

    /**
     * @return The screen for the chosen sound type: the pads or the note grid
     */
    public static InstrumentScreen create() {
        return LayoutGridInstrumentScreen.create(new BellScreen());
    }

    public BellScreen() {}
    public BellScreen(final SoundType soundType) {
        super(soundType);
    }

    @Override
    protected InstrumentScreen newGridScreen(final SoundType soundType) {
        return new BellScreen(soundType);
    }

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }

    @Override
    public SoundTypeOption<BellSoundType> soundTypeOption() {
        return new SoundTypeOption<>(BellSoundType.values(), ModClientConfigs.BELL_SOUND_TYPE,
            "button.songcraft_instruments.bell.soundType");
    }


    @Override
    public InstrumentThemeLoader getThemeLoader() {
        // Genshin-style buttons for now
        return WindsongLyreScreen.THEME_LOADER;
    }
}
