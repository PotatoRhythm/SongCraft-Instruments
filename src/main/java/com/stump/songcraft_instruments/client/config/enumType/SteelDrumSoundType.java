package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * The Steel Drum's sounds: the hang drums, hand pans and other tuned percussion of the games in the team's instrument plan.
 */
@OnlyIn(Dist.CLIENT)
public enum SteelDrumSoundType implements LayoutSoundType {
    HATO_HANG_DRUM(2, () -> new SoundOption(SCSounds.HEARTOPIA_HANG)),
    SKY_HAND_PAN(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.SKY_HANDPAN)),
    SKY_TRIUMPH_HAND_PAN(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.SKY_TRIUMPH_HANDPAN)),
    HATO_BOOMWHACKERS(1, () -> new SoundOption(SCSounds.HEARTOPIA_BOOMWHACKERS)),
    WWM_SOUND_7(2, () -> new SoundOption(SCSounds.WWM_7)),
    FF14_TIMPANI(3, () -> new SoundOption(SCSounds.FF14_TIMPANI));

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private SteelDrumSoundType(final int rows, final Supplier<SoundOption> soundType) {
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
