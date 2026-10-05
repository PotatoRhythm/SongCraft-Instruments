package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * The Concertina's sounds: the concertina, bagpipes and harmonica of the games in the team's instrument plan.
 */
@OnlyIn(Dist.CLIENT)
public enum ConcertinaSoundType implements LayoutSoundType {
    HATO_CONCERTINA(2, () -> new SoundOption(SCSounds.HEARTOPIA_CONCERTINA)),
    SKY_HARMONICA(2, () -> new SoundOption(SCSounds.SKY_HARMONICA)),
    HATO_BAGPIPES(2, () -> new SoundOption(SCSounds.HEARTOPIA_BAGPIPES));

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private ConcertinaSoundType(final int rows, final Supplier<SoundOption> soundType) {
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
