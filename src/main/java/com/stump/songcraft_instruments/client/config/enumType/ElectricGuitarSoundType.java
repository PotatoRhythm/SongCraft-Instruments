package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum ElectricGuitarSoundType implements SoundType {
    CLEAN(() -> new SoundOption(SCSounds.GUITAR_CLEAN)),
    DISTORTION(() -> new SoundOption(SCSounds.GUITAR_DISTORTION)),
    HARMONICS(() -> new SoundOption(SCSounds.GUITAR_HARMONICS));

    private final Supplier<SoundOption> soundArr;
    private ElectricGuitarSoundType(final Supplier<SoundOption> soundType) {
        this.soundArr = soundType;
    }

    @Override
    public Supplier<SoundOption> getSoundArr() {
        return soundArr;
    }
}