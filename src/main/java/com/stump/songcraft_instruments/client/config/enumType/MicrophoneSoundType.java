package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.item.MicrophoneEasterEggs.Voice;
import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public enum MicrophoneSoundType implements LayoutSoundType {
    IRINA(3, () -> new SoundOption(SCSounds.IRINA_BROCHIN)),
    BASS(3, () -> new SoundOption(SCSounds.BASS_CHOIR)),
    MIKU(3, () -> new SoundOption(SCSounds.NOT_MIKU)),
    TETO(3, () -> new SoundOption(SCSounds.NOT_TETO)),
    TETO_SNEAKY(3, () -> new SoundOption(SCSounds.NOT_TETO_SNEAKY)),
    SKY_AURORA(2, () -> new SoundOption(SCSounds.SKY_AURORA)),

    // Hidden voices, only offered by the handheld microphone when unlocked. See MicrophoneEasterEggs
    CAT(3, () -> new SoundOption(SCSounds.MICROPHONE_CAT), Voice.CAT),
    DOG(3, () -> new SoundOption(SCSounds.MICROPHONE_DOG), Voice.DOG),
    MEATBALL_MAN(3, () -> new SoundOption(SCSounds.MICROPHONE_MEATBALL_MAN), Voice.MEATBALL_MAN);

    private final int rows;
    private final Supplier<SoundOption> soundArr;
    private final @Nullable Voice easterEgg;
    private MicrophoneSoundType(final int rows, final Supplier<SoundOption> soundType) {
        this(rows, soundType, null);
    }
    private MicrophoneSoundType(final int rows, final Supplier<SoundOption> soundType, final @Nullable Voice easterEgg) {
        this.rows = rows;
        this.soundArr = soundType;
        this.easterEgg = easterEgg;
    }

    @Override
    public Supplier<SoundOption> getSoundArr() {
        return soundArr;
    }

    @Override
    public int rows() {
        return rows;
    }

    public boolean isEasterEgg() {
        return easterEgg != null;
    }

    /**
     * @return The sound types always offered by the microphones
     */
    public static MicrophoneSoundType[] regular() {
        return Arrays.stream(values()).filter((soundType) -> !soundType.isEasterEgg()).toArray(MicrophoneSoundType[]::new);
    }

    /**
     * @return The regular sound types, followed by the given unlocked hidden voices
     */
    public static MicrophoneSoundType[] withEasterEggs(final Voice... unlocked) {
        return Arrays.stream(values())
            .filter((soundType) -> !soundType.isEasterEgg() || Arrays.asList(unlocked).contains(soundType.easterEgg))
            .toArray(MicrophoneSoundType[]::new);
    }

    public static MicrophoneSoundType of(final Voice voice) {
        return Arrays.stream(values()).filter((soundType) -> soundType.easterEgg == voice).findFirst().orElseThrow();
    }
}
