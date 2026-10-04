package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum GuitarSoundType implements SoundType {
    EMI(() -> new SoundOption(SCSounds.GUITAR)),
    NYLON(() -> new SoundOption(SCSounds.GUITAR_NYLON)),
    STEEL(() -> new SoundOption(SCSounds.GUITAR_STEEL));

    private final Supplier<SoundOption> soundArr;
    private GuitarSoundType(final Supplier<SoundOption> soundType) {
        this.soundArr = soundType;
    }

    @Override
    public Supplier<SoundOption> getSoundArr() {
        return soundArr;
    }
}