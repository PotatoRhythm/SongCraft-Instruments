package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum KeyboardSoundType implements LayoutSoundType {
    EMI(3, () -> new SoundOption(SCSounds.KEYBOARD)),
    YAMAHA_C5(3, () -> new SoundOption(SCSounds.KEYBOARD_YAMAHA_C5)),
    HEARTOPIA(3, () -> new SoundOption(SCSounds.HEARTOPIA)),
    SKY_PIANO(2, () -> new SoundOption(SCSounds.SKY_PIANO)),
    SKY_GRAND_PIANO(2, () -> new SoundOption(SCSounds.SKY_GRAND_PIANO)),
    SKY_WINTER_PIANO(2, () -> new SoundOption(SCSounds.SKY_WINTER_PIANO)),
    FF14_PIANO(3, () -> new SoundOption(SCSounds.FF14_PIANO)),
    ELECTRIC(3, () -> new SoundOption(SCSounds.KEYBOARD_ELECTRIC)),
    HARPSICHORD(3, () -> new SoundOption(SCSounds.KEYBOARD_HARPSICHORD));

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private KeyboardSoundType(final int rows, final Supplier<SoundOption> soundType) {
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
