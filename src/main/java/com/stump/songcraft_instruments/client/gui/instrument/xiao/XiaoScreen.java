package com.stump.songcraft_instruments.client.gui.instrument.xiao;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.XiaoSoundType;
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
 * The Xiao's note grid, for the xiaos, panflutes and the krill horn.
 * Choosing a sound played on pads switches over to its pads.
 */
@OnlyIn(Dist.CLIENT)
public class XiaoScreen extends LayoutGridInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "xiao");

    /**
     * @return The screen for the chosen sound type: the pads or the note grid
     */
    public static InstrumentScreen create() {
        return LayoutGridInstrumentScreen.create(new XiaoScreen());
    }

    public XiaoScreen() {}
    public XiaoScreen(final SoundType soundType) {
        super(soundType);
    }

    @Override
    protected InstrumentScreen newGridScreen(final SoundType soundType) {
        return new XiaoScreen(soundType);
    }

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }

    @Override
    public SoundTypeOption<XiaoSoundType> soundTypeOption() {
        return new SoundTypeOption<>(XiaoSoundType.values(), ModClientConfigs.XIAO_SOUND_TYPE,
            "button.songcraft_instruments.xiao.soundType");
    }


    @Override
    public InstrumentThemeLoader getThemeLoader() {
        // Genshin-style buttons for now
        return WindsongLyreScreen.THEME_LOADER;
    }
}
