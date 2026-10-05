package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum MicrophoneSoundType implements LayoutSoundType {
    IRINA(3, () -> new SoundOption(SCSounds.IRINA_BROCHIN)),
    BASS(3, () -> new SoundOption(SCSounds.BASS_CHOIR)),
    MIKU(3, () -> new SoundOption(SCSounds.NOT_MIKU)),
    TETO(3, () -> new SoundOption(SCSounds.NOT_TETO)),
    TETO_SNEAKY(3, () -> new SoundOption(SCSounds.NOT_TETO_SNEAKY)),
    SKY_AURORA(2, () -> new SoundOption(SCSounds.SKY_AURORA));

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private MicrophoneSoundType(final int rows, final Supplier<SoundOption> soundType) {
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
