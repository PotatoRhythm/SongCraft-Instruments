package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SoundOption;

import java.util.Locale;
import java.util.function.Supplier;

public interface SoundType {
    Supplier<SoundOption> getSoundArr();

    /**
     * @return The name of this sound type
     * as in the translation files
     */
    default String getName() {
        return toString().toLowerCase(Locale.ENGLISH);
    }
}
