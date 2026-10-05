package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * The Flute's sounds: the flutes, recorders and reeds of the games in the team's instrument plan.
 */
@OnlyIn(Dist.CLIENT)
public enum FluteSoundType implements LayoutSoundType {
    SKY_FLUTE(2, () -> new SoundOption(SCSounds.SKY_FLUTE)),
    SKY_TRANSVERSE_FLUTE(2, () -> new SoundOption(SCSounds.SKY_TRANSVERSE_FLUTE)),
    FF14_FLUTE(3, () -> new SoundOption(SCSounds.FF14_FLUTE)),
    FF14_FIFE(3, () -> new SoundOption(SCSounds.FF14_FIFE)),
    HATO_SOPRANO_RECORDER(2, () -> new SoundOption(SCSounds.HEARTOPIA_SOPRANO_RECORDER)),
    FF14_OBOE(3, () -> new SoundOption(SCSounds.FF14_OBOE)),
    FF14_CLARINET(3, () -> new SoundOption(SCSounds.FF14_CLARINET));

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private FluteSoundType(final int rows, final Supplier<SoundOption> soundType) {
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
