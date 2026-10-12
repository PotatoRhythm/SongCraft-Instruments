package com.stump.songcraft_instruments.client.gui.instrument.microphone;

import com.stump.songcraft_instruments.client.gui.instrument.partial.SoundTypeOption;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.MicrophoneSoundType;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.gui.instrument.partial.IHeldInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.layout.LayoutGridInstrumentScreen;
import com.stump.songcraft_instruments.item.MicrophoneEasterEggs;
import com.stump.songcraft_instruments.item.MicrophoneEasterEggs.Voice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class MicrophoneScreen extends LayoutGridInstrumentScreen implements IHeldInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "microphone");

    /**
     * The regular sound types, plus the {@link MicrophoneEasterEggs hidden voices} unlocked as this screen opened
     */
    private final MicrophoneSoundType[] soundTypes;

    public MicrophoneScreen() {
        final LocalPlayer player = Minecraft.getInstance().player;
        final List<Voice> unlocked = new ArrayList<>();

        final Voice targetVoice = (player == null) ? null : MicrophoneEasterEggs.takeTargetVoice(player);
        if (targetVoice != null)
            unlocked.add(targetVoice);
        if ((player != null) && MicrophoneEasterEggs.hasMeatballManVoice(player))
            unlocked.add(Voice.MEATBALL_MAN);

        soundTypes = MicrophoneSoundType.withEasterEggs(unlocked.toArray(Voice[]::new));

        // Start on an unlocked hidden voice; otherwise, don't keep one that is no longer unlocked
        if (!unlocked.isEmpty())
            initPreferredSoundType(MicrophoneSoundType.of(unlocked.get(0)));
        else if (!Arrays.asList(soundTypes).contains(ModClientConfigs.MICROPHONE_SOUND_TYPE.get()))
            initPreferredSoundType(MicrophoneSoundType.IRINA);
    }

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }

    @Override
    public SoundTypeOption<MicrophoneSoundType> soundTypeOption() {
        return new SoundTypeOption<>(soundTypes, ModClientConfigs.MICROPHONE_SOUND_TYPE,
            "button.songcraft_instruments.microphone.soundType");
    }

    public static final InstrumentThemeLoader THEME_LOADER = new InstrumentThemeLoader(INSTRUMENT_ID);
    @Override
    public InstrumentThemeLoader getThemeLoader() {
        return THEME_LOADER;
    }
}
