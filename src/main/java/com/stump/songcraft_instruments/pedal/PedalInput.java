package com.stump.songcraft_instruments.pedal;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

/**
 * Something a pedal can be plugged into: another pedal, or a speaker block. Takes at most one pedal.
 * <p>A link is stored on both ends (the pedal's output, and this input), and only counts while both agree,
 * see {@link PedalLinks}.</p>
 */
public interface PedalInput {
    /**
     * @return The position of the pedal plugged into this, or null if there is none
     */
    @Nullable BlockPos getPedalInput();
    void setPedalInput(@Nullable BlockPos pedalPos);
}
