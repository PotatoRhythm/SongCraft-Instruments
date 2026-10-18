package com.stump.songcraft_instruments.sound;

import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.sound.pedal.PedalSoundInstance;
import com.stump.songcraft_instruments.sound.pedal.PedalSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.function.UnaryOperator;

/**
 * A positioned Mono sound that plays alongside a Stereo note, so the note can
 * crossfade from Stereo to Mono as the listener moves away from it.
 * <p>Its volume and position are driven entirely by its Stereo counterpart,
 * via {@link #update}, on top of the volume defined in sounds.json. It stops itself once the Stereo sound is stopped.</p>
 */
@OnlyIn(Dist.CLIENT)
public class CrossfadeMonoSoundInstance extends AbstractTickableSoundInstance
        implements WorldOriginSoundInstance, PedalSoundInstance {
    private final AbstractTickableSoundInstance stereo;
    /**
     * The effects pedals the note is played through, same as its Stereo sound's
     */
    private final PedalChain pedalChain;
    /**
     * Swaps the sound for its rendered copy through the pedals, the same kind of copy as its Stereo sound plays
     */
    private final UnaryOperator<Sound> withPedals;

    public CrossfadeMonoSoundInstance(SoundEvent mono, AbstractTickableSoundInstance stereo, float pitch) {
        this(mono, stereo, pitch, PedalChain.NONE, false);
    }
    /**
     * @param echoTail Whether to play only the echoes of the sound, same as its Stereo sound
     */
    public CrossfadeMonoSoundInstance(SoundEvent mono, AbstractTickableSoundInstance stereo, float pitch,
                                      PedalChain pedalChain, boolean echoTail) {
        // The pitch it plays at, same as SoundEngineMixin gives it
        this(mono, stereo, pitch, pedalChain, (sound) -> PedalSounds.withPedals(sound, pedalChain, pitch, echoTail));
    }
    /**
     * @param pedalChain The note's whole chain, whose live effects apply to this sound too
     * @param withPedals Swaps the sound for its rendered copy through the pedals, as its Stereo sound does
     */
    public CrossfadeMonoSoundInstance(SoundEvent mono, AbstractTickableSoundInstance stereo, float pitch,
                                      PedalChain pedalChain, UnaryOperator<Sound> withPedals) {
        super(mono, NoteSound.INSTRUMENT_SOUND_SOURCE, SoundInstance.createUnseededRandom());

        this.stereo = stereo;
        this.pedalChain = pedalChain;
        this.withPedals = withPedals;

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
    public PedalChain getPedalChain() {
        return pedalChain;
    }

    @Override
    public WeighedSoundEvents resolve(final SoundManager manager) {
        final WeighedSoundEvents events = super.resolve(manager);
        sound = withPedals.apply(sound);
        return events;
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
