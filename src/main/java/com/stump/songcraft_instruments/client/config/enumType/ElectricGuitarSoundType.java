package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum ElectricGuitarSoundType implements LayoutSoundType {
    CLEAN(3, () -> new SoundOption(SCSounds.GUITAR_CLEAN)),
    FF14_CLEAN_GUITAR(3, () -> new SoundOption(SCSounds.FF14_CLEAN_GUITAR)),
    SKY_LIGHT_GUITAR(2, () -> new SoundOption(SCSounds.SKY_LIGHT_GUITAR)),
    FF14_MUTED_GUITAR(3, () -> new SoundOption(SCSounds.FF14_MUTED_GUITAR)),
    DISTORTION(3, () -> new SoundOption(SCSounds.GUITAR_DISTORTION)),
    FF14_OVERDRIVEN_GUITAR(3, () -> new SoundOption(SCSounds.FF14_OVERDRIVEN_GUITAR)),
    FF14_POWER_CHORDS_GUITAR(3, () -> new SoundOption(SCSounds.FF14_POWER_CHORDS_GUITAR)),
    HARMONICS(3, () -> new SoundOption(SCSounds.GUITAR_HARMONICS)),
    FF14_SPECIAL_GUITAR(3, () -> new SoundOption(SCSounds.FF14_SPECIAL_GUITAR));

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private ElectricGuitarSoundType(final int rows, final Supplier<SoundOption> soundType) {
        this.rows = rows;
        this.soundArr = soundType;
    }

    @Override
    public Supplier<SoundOption> getSoundArr() {
        return soundArr;
    }

    @Override
    public int rows() {
        return rows;
    }
}
