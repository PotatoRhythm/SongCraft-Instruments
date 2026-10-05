package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum PipaSoundType implements LayoutSoundType {
    REGULAR(3, () -> new SoundOption(SCSounds.PIPA_REGULAR)),
    TREMOLO(3, () -> new SoundOption(SCSounds.PIPA_TERMOLO)),
    SKY_PIPA(2, () -> new SoundOption(SCSounds.SKY_PIPA)),
    WWM_PIPA(3, () -> new SoundOption(SCSounds.WWM_3));

    private final int columns;
    private final Supplier<SoundOption> soundArr;
    private PipaSoundType(final int columns, final Supplier<SoundOption> soundType) {
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
