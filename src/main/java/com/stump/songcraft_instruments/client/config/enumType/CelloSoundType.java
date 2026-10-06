package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * The Cello's sounds: the cellos of the games in the team's instrument plan.
 */
@OnlyIn(Dist.CLIENT)
public enum CelloSoundType implements LayoutSoundType {
    HATO_CELLO(2, () -> new SoundOption(SCSounds.HEARTOPIA_CELLO)),
    SKY_CELLO(2, () -> new SoundOption(SCSounds.SKY_CELLO)),
    FF14_CELLO(3, () -> new SoundOption(SCSounds.FF14_CELLO));

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private CelloSoundType(final int rows, final Supplier<SoundOption> soundType) {
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
