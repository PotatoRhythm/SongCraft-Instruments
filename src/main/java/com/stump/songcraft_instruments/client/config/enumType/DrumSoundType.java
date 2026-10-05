package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * The Drum's sounds: the drums and cymbals of the games in the team's instrument plan.
 */
@OnlyIn(Dist.CLIENT)
public enum DrumSoundType implements LayoutSoundType {
    HATO_CAJON(LayoutSoundType.DRUM_PADS, () -> new SoundOption(SCSounds.HEARTOPIA_CAJON)),
    HATO_CONGA_BONGOS(LayoutSoundType.DRUM_PADS, () -> new SoundOption(SCSounds.HEARTOPIA_CONGA_BONGOS)),
    FF14_BONGO(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.FF14_BONGO)),
    SKY_DRUM(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.SKY_DRUM)),
    SKY_DUNDUN(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.SKY_DUNDUN)),
    SKY_FORTUNE_DRUM(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.SKY_FORTUNE_DRUM)),
    FF14_BASS_DRUM(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.FF14_BASS_DRUM)),
    FF14_SNARE_DRUM(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.FF14_SNARE_DRUM)),
    FF14_DRUM_KIT(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.FF14_DRUM_KIT)),
    SKY_TR_909(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.SKY_TR_909)),
    SKY_DANCE(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.SKY_DANCE)),
    SKY_CYMBALS(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.SKY_CYMBALS)),
    FF14_CYMBALS(LayoutSoundType.PADS, () -> new SoundOption(SCSounds.FF14_CYMBAL));

    private final int columns;
    private final Supplier<SoundOption> soundArr;
    private DrumSoundType(final int columns, final Supplier<SoundOption> soundType) {
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
