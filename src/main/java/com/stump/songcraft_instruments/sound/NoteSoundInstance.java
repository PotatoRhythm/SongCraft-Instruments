package com.stump.songcraft_instruments.sound;

import com.stump.songcraft_instruments.networking.buttonidentifier.NoteButtonIdentifier;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;

public class NoteSoundInstance extends AbstractTickableSoundInstance
        implements DampenableSoundInstance {

    public static final float FADE_TIME = 0.5f;

    private final NoteSound noteSound;
    private final int notePitch;
    private final float startVolume;

    private float currentVolume;
    private boolean dampened;
    //TODO: probably shouldn't use optionals here
    private final ResourceLocation instrumentId;
    private final Optional<NoteButtonIdentifier> noteIdentifier;
    private final Optional<Integer> initiatorId;
    private final Optional<InitiatorID> oInitiatorId;

    private final Vec3 sourcePos;
    private @Nullable CrossfadeMonoSoundInstance monoCrossfade;

    public NoteSoundInstance(NoteSound noteSound, float pitch, NoteSoundMetadata meta, double playDistSqr,
            Optional<Integer> initiatorId, Optional<InitiatorID> oInitiatorId
    ) {
        super(
                noteSound.getByDistance(playDistSqr),
                SoundSource.RECORDS,
                SoundInstance.createUnseededRandom()
        );

        this.noteSound = noteSound;
        this.notePitch = meta.pitch();
        this.startVolume = meta.volume() / 100f;
        this.currentVolume = this.startVolume;

        this.pitch = pitch;
        this.volume = this.startVolume;

        this.instrumentId = meta.instrumentId();
        this.noteIdentifier = meta.noteIdentifier();

        this.initiatorId = initiatorId;
        this.oInitiatorId = oInitiatorId;

        this.sourcePos = meta.pos().getCenter();

        if (noteSound.usesStereo(playDistSqr)) {
            // Stereo plays in the listener's head, and crossfades into a positioned Mono with distance
            this.attenuation = Attenuation.NONE;
            this.relative = true;

            this.x = 0;
            this.y = 0;
            this.z = 0;

            this.monoCrossfade = new CrossfadeMonoSoundInstance(noteSound.getMono(), this, pitch, true);
            updateCrossfade();
        } else if (playDistSqr <= NoteSound.LOCAL_RANGE * NoteSound.LOCAL_RANGE) {
            this.attenuation = Attenuation.NONE;
            this.relative = true;

            this.x = 0;
            this.y = 0;
            this.z = 0;
        } else {
            this.attenuation = Attenuation.LINEAR;
            this.relative = false;

            this.x = meta.pos().getX() + 0.5;
            this.y = meta.pos().getY() + 0.5;
            this.z = meta.pos().getZ() + 0.5;
        }
    }

    @Override
    public void tick() {
        if (dampened) {
            currentVolume -= startVolume / (FADE_TIME * 20.0f);

            if (currentVolume <= 0) {
                currentVolume = 0;
                volume = 0;
                stopNote();
                return;
            }

            volume = currentVolume;
        }

        updateCrossfade();
    }

    /**
     * Fades the Stereo sound with distance, and hands the rest of the volume to the Mono crossfade
     */
    private void updateCrossfade() {
        if (monoCrossfade == null)
            return;

        final double dist = NoteSound.listenerPos().distanceTo(sourcePos);
        volume = currentVolume * NoteSound.stereoGain(dist);
        monoCrossfade.update(sourcePos, currentVolume * NoteSound.crossfadeMonoGain(dist));
    }

    public Optional<CrossfadeMonoSoundInstance> getMonoCrossfade() {
        return Optional.ofNullable(monoCrossfade);
    }

    @Override
    public void dampen() {
        if (dampened)
            return;

        dampened = true;
    }

    @Override
    public boolean isDampened() {
        return dampened;
    }

    public void stopNote() {
        stop();
        NoteSoundInstances.remove(this);
    }

    public NoteSound getNoteSound() {
        return noteSound;
    }

    public int getNotePitch() {
        return notePitch;
    }

    public ResourceLocation getInstrumentId() {
        return instrumentId;
    }

    public Optional<NoteButtonIdentifier> getNoteIdentifier() {
        return noteIdentifier;
    }

    public Optional<Integer> getInitiatorId() {
        return initiatorId;
    }

    public Optional<InitiatorID> getOInitiatorId() {
        return oInitiatorId;
    }
}