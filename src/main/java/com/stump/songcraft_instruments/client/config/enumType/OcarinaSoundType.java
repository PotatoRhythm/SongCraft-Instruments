package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * The Ocarina's sounds: the ocarinas and conch shells of the games in the team's instrument plan.
 */
@OnlyIn(Dist.CLIENT)
public enum OcarinaSoundType implements LayoutSoundType {
    HATO_OCARINA(2, () -> new SoundOption(SCSounds.HEARTOPIA_OCARINA)),
    SKY_OCARINA(2, () -> new SoundOption(SCSounds.SKY_OCARINA)),
    SKY_MANTA_OCARINA(2, () -> new SoundOption(SCSounds.SKY_MANTA_OCARINA)),
    HATO_CONCH_SHELLS(1, () -> new SoundOption(SCSounds.HEARTOPIA_CONCH_SHELLS));

    private final int columns;
    private final Supplier<SoundOption> soundArr;
    private OcarinaSoundType(final int columns, final Supplier<SoundOption> soundType) {
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
