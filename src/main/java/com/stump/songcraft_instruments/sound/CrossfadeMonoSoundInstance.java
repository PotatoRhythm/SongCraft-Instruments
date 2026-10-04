package com.stump.songcraft_instruments.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A positioned Mono sound that plays alongside a Stereo note, so the note can
 * crossfade from Stereo to Mono as the listener moves away from it.
 * <p>Its volume and position are driven entirely by its Stereo counterpart,
 * via {@link #update}. It stops itself once the Stereo sound is stopped.</p>
 */
@OnlyIn(Dist.CLIENT)
public class CrossfadeMonoSoundInstance extends AbstractTickableSoundInstance {
    private final AbstractTickableSoundInstance stereo;
    /**
     * Whether the volume & pitch defined in sounds.json should be applied,
     * to match the behaviour of the Stereo counterpart.
     */
    private final boolean applySoundDefinition;

    public CrossfadeMonoSoundInstance(SoundEvent mono, AbstractTickableSoundInstance stereo,
                                      float pitch, boolean applySoundDefinition) {
        super(mono, NoteSound.INSTRUMENT_SOUND_SOURCE, SoundInstance.createUnseededRandom());

        this.stereo = stereo;
        this.applySoundDefinition = applySoundDefinition;

        this.pitch = pitch;
        this.volume = 0;
        this.attenuation = Attenuation.LINEAR;
        this.relative = false;
    }

    public void update(final Vec3 pos, final float volume) {
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.volume = volume;
    }

    @Override
    public void tick() {
        if (stereo.isStopped())
            stop();
    }

    // Up close, this sound is fully faded out. It must still start so it can fade in later.
    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public float getVolume() {
        return applySoundDefinition ? super.getVolume() : volume;
    }
    @Override
    public float getPitch() {
        return applySoundDefinition ? super.getPitch() : pitch;
    }
}
