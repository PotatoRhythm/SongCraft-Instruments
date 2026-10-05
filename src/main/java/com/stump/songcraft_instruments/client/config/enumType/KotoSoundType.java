package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum KotoSoundType implements LayoutSoundType {
    EMI(3, () -> new SoundOption(SCSounds.KOTO)),
    WWM_GUQIN(3, () -> new SoundOption(SCSounds.WWM_6));

    private final int columns;
    private final Supplier<SoundOption> soundArr;
    private KotoSoundType(final int columns, final Supplier<SoundOption> soundType) {
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
