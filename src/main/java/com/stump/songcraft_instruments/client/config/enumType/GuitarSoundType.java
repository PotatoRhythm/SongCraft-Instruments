package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum GuitarSoundType implements LayoutSoundType {
    EMI(3, () -> new SoundOption(SCSounds.GUITAR)),
    STEEL(3, () -> new SoundOption(SCSounds.GUITAR_STEEL)),
    SKY_GUITAR(2, () -> new SoundOption(SCSounds.SKY_GUITAR)),
    SKY_TOY_UKULELE(2, () -> new SoundOption(SCSounds.SKY_TOY_UKULELE)),
    HATO_LUNGHE(2, () -> new SoundOption(SCSounds.HEARTOPIA_LUNGHE)),
    FF14_LUTE(3, () -> new SoundOption(SCSounds.FF14_LUTE));

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private GuitarSoundType(final int rows, final Supplier<SoundOption> soundType) {
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
