package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * The Xiao's sounds: the xiaos, panflutes and horns of the games in the team's instrument plan.
 */
@OnlyIn(Dist.CLIENT)
public enum XiaoSoundType implements LayoutSoundType {
    HATO_XIAO(2, () -> new SoundOption(SCSounds.HEARTOPIA_XIAO)),
    WWM_XIAO(3, () -> new SoundOption(SCSounds.WWM_4)),
    SKY_PANFLUTE(2, () -> new SoundOption(SCSounds.SKY_PANFLUTE)),
    FF14_PANPIPES(3, () -> new SoundOption(SCSounds.FF14_PANPIPES)),
    SKY_HORN(2, () -> new SoundOption(SCSounds.SKY_HORN)),
    FF14_HORN(3, () -> new SoundOption(SCSounds.FF14_HORN));

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private XiaoSoundType(final int rows, final Supplier<SoundOption> soundType) {
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
