package com.stump.songcraft_instruments.sound;

import net.minecraft.world.phys.Vec3;

/**
 * A sound instance that knows where in the world it is being played from,
 * even when it is played relative to the listener (as Stereo and local notes are).
 * <p>Used by mod integrations that need the real position of a sound, such as Sound Physics Remastered.</p>
 */
public interface WorldOriginSoundInstance {
    /**
     * @return The position in the world this sound originates from
     */
    Vec3 getWorldOrigin();
}
