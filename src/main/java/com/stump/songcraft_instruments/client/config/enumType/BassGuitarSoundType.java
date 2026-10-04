package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum BassGuitarSoundType implements SoundType {
    ACOUSTIC(() -> new SoundOption(SCSounds.BASS_ACOUSTIC, true)),
    FINGER(() -> new SoundOption(SCSounds.BASS_FINGER, true)),
    SLAP(() -> new SoundOption(SCSounds.BASS_SLAP, true)),
    PICKED(() -> new SoundOption(SCSounds.BASS_PICKED, true));

    private final Supplier<SoundOption> soundArr;
    private BassGuitarSoundType(final Supplier<SoundOption> soundType) {
        this.soundArr = soundType;
    }

    @Override
    public Supplier<SoundOption> getSoundArr() {
        return soundArr;
    }
}