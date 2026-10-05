package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum TromboneSoundType implements LayoutSoundType {
    EMI(3, () -> new SoundOption(SCSounds.TROMBONE)),
    PHGM(3, () -> new SoundOption(SCSounds.TROMBONE_PHGM)),
    FF14_TROMBONE(3, () -> new SoundOption(SCSounds.FF14_TROMBONE)),
    WESTGATE(3, () -> new SoundOption(SCSounds.TRUMPET_WESTGATE_STUDIOS)),
    SKY_TRUMPET(2, () -> new SoundOption(SCSounds.SKY_TRUMPET)),
    FF14_TRUMPET(3, () -> new SoundOption(SCSounds.FF14_TRUMPET)),
    FF14_TUBA(3, () -> new SoundOption(SCSounds.FF14_TUBA));

    private final int columns;
    private final Supplier<SoundOption> soundArr;
    private TromboneSoundType(final int columns, final Supplier<SoundOption> soundType) {
        this.columns = columns;
        this.soundArr = soundType;
    }

    @Override
    public Supplier<SoundOption> getSoundArr() {
        return soundArr;
    }

    @Override
    public int columns() {
        return columns;
    }
}
