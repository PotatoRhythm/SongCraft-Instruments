package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum SaxophoneSoundType implements LayoutSoundType {
    HATO_SAX(2, () -> new SoundOption(SCSounds.HEARTOPIA_SAX)),
    SKY_SAXOPHONE(2, () -> new SoundOption(SCSounds.SKY_SAXOPHONE)),
    FF14_SAXOPHONE(3, () -> new SoundOption(SCSounds.FF14_SAXOPHONE)),
    TENOR(3, () -> new SoundOption(SCSounds.SAXOPHONE_TENOR)),
    BARITONE(3, () -> new SoundOption(SCSounds.SAXOPHONE_BARITONE));

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private SaxophoneSoundType(final int rows, final Supplier<SoundOption> soundType) {
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
