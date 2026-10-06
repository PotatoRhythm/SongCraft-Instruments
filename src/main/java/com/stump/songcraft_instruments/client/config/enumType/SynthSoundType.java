package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * The Synth's sounds: Sky's synths and the classic sine, square, saw and triangle waves.
 */
@OnlyIn(Dist.CLIENT)
public enum SynthSoundType implements LayoutSoundType {
    SKY_BASS_SYNTH(2, () -> new SoundOption(SCSounds.SKY_BASS_SYNTH)),
    SKY_CHIME_SYNTH(2, () -> new SoundOption(SCSounds.SKY_CHIME_SYNTH)),
    SINE(3, () -> new SoundOption(SCSounds.SYNTH_SINE)),
    SQUARE(3, () -> new SoundOption(SCSounds.SYNTH_SQUARE)),
    SAW(3, () -> new SoundOption(SCSounds.SYNTH_SAW)),
    TRIANGLE(3, () -> new SoundOption(SCSounds.SYNTH_TRIANGLE));

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private SynthSoundType(final int rows, final Supplier<SoundOption> soundType) {
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
