package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * The Bell's sounds: the bells and chimes of the games in the team's instrument plan.
 */
@OnlyIn(Dist.CLIENT)
public enum BellSoundType implements LayoutSoundType {
    SKY_BELLS(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.SKY_BELLS)),
    SKY_SMALL_BELL(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.SKY_SMALL_BELL)),
    WWM_FANGXIANG_CHIMES(3, () -> new SoundOption(SCSounds.WWM_2));

    private final int columns;
    private final Supplier<SoundOption> soundArr;
    private BellSoundType(final int columns, final Supplier<SoundOption> soundType) {
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
