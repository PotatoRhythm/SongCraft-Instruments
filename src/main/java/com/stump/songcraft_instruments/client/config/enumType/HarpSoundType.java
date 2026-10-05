package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * The Harp's sounds: the harps and lyres of the games in the team's instrument plan.
 */
@OnlyIn(Dist.CLIENT)
public enum HarpSoundType implements LayoutSoundType {
    HATO_HARP(3, () -> new SoundOption(SCSounds.HEARTOPIA_HARP)),
    SKY_HARP(2, () -> new SoundOption(SCSounds.SKY_HARP)),
    FF14_HARP(3, () -> new SoundOption(SCSounds.FF14_HARP)),
    WWM_KONGHOU(3, () -> new SoundOption(SCSounds.WWM_1)),
    HATO_LYRE(2, () -> new SoundOption(SCSounds.HEARTOPIA_LYRE));

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private HarpSoundType(final int rows, final Supplier<SoundOption> soundType) {
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
