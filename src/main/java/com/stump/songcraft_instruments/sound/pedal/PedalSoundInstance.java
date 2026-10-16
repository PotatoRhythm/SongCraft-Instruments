package com.stump.songcraft_instruments.sound.pedal;

import com.stump.songcraft_instruments.pedal.PedalChain;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A sound instance played through effects pedals
 */
@OnlyIn(Dist.CLIENT)
public interface PedalSoundInstance {
    /**
     * @return The pedals the sound is played through, including its live effects (see {@link PedalLiveEffects})
     */
    PedalChain getPedalChain();
}
