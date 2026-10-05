package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum ViolinSoundType implements LayoutSoundType {
    SLOW(3, () -> new SoundOption(SCSounds.VIOLIN_SLOW)),
    FAST(3, () -> new SoundOption(SCSounds.VIOLIN_FAST)),
    PIZZ(3, () -> new SoundOption(SCSounds.VIOLIN_PIZZICATO)),
    HATO_VIOLIN(2, () -> new SoundOption(SCSounds.HEARTOPIA_VIOLIN)),
    SKY_VIOLIN(2, () -> new SoundOption(SCSounds.SKY_VIOLIN)),
    FF14_VIOLIN(3, () -> new SoundOption(SCSounds.FF14_VIOLIN)),
    FF14_FIDDLE(3, () -> new SoundOption(SCSounds.FF14_FIDDLE)),
    FF14_VIOLA(3, () -> new SoundOption(SCSounds.FF14_VIOLA)),
    WWM_ERHU(3, () -> new SoundOption(SCSounds.WWM_5));

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private ViolinSoundType(final int rows, final Supplier<SoundOption> soundType) {
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
