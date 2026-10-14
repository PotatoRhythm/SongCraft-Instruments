package com.stump.songcraft_instruments.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * A positioned Mono sound that plays alongside a Stereo note, so the note can
 * crossfade from Stereo to Mono as the listener moves away from it.
 * <p>Its volume and position are driven entirely by its Stereo counterpart,
 * via {@link #update}, on top of the volume defined in sounds.json. It stops itself once the Stereo sound is stopped.</p>
 */
@OnlyIn(Dist.CLIENT)
public class CrossfadeMonoSoundInstance extends AbstractTickableSoundInstance implements WorldOriginSoundInstance {
    private final AbstractTickableSoundInstance stereo;

    public CrossfadeMonoSoundInstance(SoundEvent mono, AbstractTickableSoundInstance stereo, float pitch) {
        super(mono, NoteSound.INSTRUMENT_SOUND_SOURCE, SoundInstance.createUnseededRandom());

        this.stereo = stereo;

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
    public Vec3 getWorldOrigin() {
        return new Vec3(x, y, z);
    }

    @Override
    public @Nullable Entity getOriginEntity() {
        return (stereo instanceof WorldOriginSoundInstance origin) ? origin.getOriginEntity() : null;
    }
}
