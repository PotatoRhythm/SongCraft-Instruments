package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * The Kalimba's sounds: the kalimbas and the xylophone of the games in the team's instrument plan.
 */
@OnlyIn(Dist.CLIENT)
public enum KalimbaSoundType implements LayoutSoundType {
    HATO_KALIMBA(2, () -> new SoundOption(SCSounds.HEARTOPIA_KALIMBA)),
    SKY_KALIMBA(2, () -> new SoundOption(SCSounds.SKY_KALIMBA)),
    SKY_XYLOPHONE(2, () -> new SoundOption(SCSounds.SKY_XYLOPHONE));

    private final int columns;
    private final Supplier<SoundOption> soundArr;
    private KalimbaSoundType(final int columns, final Supplier<SoundOption> soundType) {
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
