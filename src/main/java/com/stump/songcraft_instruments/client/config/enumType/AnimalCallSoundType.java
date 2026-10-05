package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * The Animal Call's sounds: Sky's animal calls of the games in the team's instrument plan.
 */
@OnlyIn(Dist.CLIENT)
public enum AnimalCallSoundType implements LayoutSoundType {
    SKY_BIRD_CALL(2, () -> new SoundOption(SCSounds.SKY_BIRD_CALL)),
    SKY_CRAB_CALL(2, () -> new SoundOption(SCSounds.SKY_CRAB_CALL)),
    SKY_FISH_CALL(2, () -> new SoundOption(SCSounds.SKY_FISH_CALL)),
    SKY_JELLY_CALL(2, () -> new SoundOption(SCSounds.SKY_JELLY_CALL)),
    SKY_MANTA_CALL(2, () -> new SoundOption(SCSounds.SKY_MANTA_CALL)),
    SKY_SPIRIT_MANTA_CALL(2, () -> new SoundOption(SCSounds.SKY_SPIRIT_MANTA_CALL)),
    SKY_MOTH_CALL(2, () -> new SoundOption(SCSounds.SKY_MOTH_CALL));

    private final int columns;
    private final Supplier<SoundOption> soundArr;
    private AnimalCallSoundType(final int columns, final Supplier<SoundOption> soundType) {
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
