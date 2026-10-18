package com.stump.songcraft_instruments.sound;

import com.stump.songcraft_instruments.networking.buttonidentifier.NoteButtonIdentifier;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import com.stump.songcraft_instruments.sound.pedal.PedalSoundInstance;
import com.stump.songcraft_instruments.sound.pedal.PedalSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class NoteSoundInstance extends AbstractTickableSoundInstance
        implements DampenableSoundInstance, WorldOriginSoundInstance, PedalSoundInstance {

    public static final float FADE_TIME = 0.5f;
    /**
     * The longest a dampened note takes to fade, for notes whose pedals ring on after them (like a delay's echoes).
     * Dampening those fades them out gently instead of cutting their echoes, as a middle ground.
     */
    public static final float MAX_PEDAL_FADE_TIME = 2f;

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
    /**
     * The effects pedals this note is played through, see {@link PedalSounds}
     */
    private final PedalChain pedalChain;
    /**
     * Swaps the sound for the rendered copy of it this note plays through its pedals (see {@link PedalSounds}),
     * such as only the echoes of its sound, for a held note's release
     */
    private final UnaryOperator<Sound> withPedals;
    /**
     * What to multiply this note's volume by on each of its ticks, from its first, as it fades in.
     * The last applies from then on. Null if it plays at its volume throughout.
     */
    private final @Nullable float[] volumeRamp;
    private int ticks;
    /**
     * How long this note takes to fade out when dampened, in seconds
     */
    private final float fadeTime;

    private final Vec3 sourcePos;
    /**
     * The player playing this note on a held instrument, or null if it comes from a block
     */
    private final @Nullable Entity originEntity;
    private @Nullable CrossfadeMonoSoundInstance monoCrossfade;

    public NoteSoundInstance(NoteSound noteSound, float pitch, NoteSoundMetadata meta, double playDistSqr,
            Optional<Integer> initiatorId, Optional<InitiatorID> oInitiatorId
    ) {
        this(noteSound, pitch, meta, playDistSqr, initiatorId, oInitiatorId, PedalChain.NONE);
    }
    public NoteSoundInstance(NoteSound noteSound, float pitch, NoteSoundMetadata meta, double playDistSqr,
            Optional<Integer> initiatorId, Optional<InitiatorID> oInitiatorId, PedalChain pedalChain
    ) {
        this(noteSound, pitch, meta, playDistSqr, initiatorId, oInitiatorId, pedalChain, false);
    }
    /**
     * @param echoTail Whether to play only the echoes of the sound, for a held note's release
     */
    public NoteSoundInstance(NoteSound noteSound, float pitch, NoteSoundMetadata meta, double playDistSqr,
            Optional<Integer> initiatorId, Optional<InitiatorID> oInitiatorId, PedalChain pedalChain,
            boolean echoTail
    ) {
        // The pitch it plays at, same as SoundEngineMixin gives it
        this(noteSound, pitch, meta, playDistSqr, initiatorId, oInitiatorId, pedalChain,
            (sound) -> PedalSounds.withPedals(sound, pedalChain, pitch, echoTail), null);
    }
    /**
     * @param pedalChain The pedals the note is played through, whose live effects apply to it
     * @param withPedals Swaps the sound for the rendered copy of it to play through the pedals
     * @param volumeRamp What to multiply the note's volume by on each of its ticks, from its first; the last applies
     *                   from then on. Null to play at its volume throughout.
     */
    public NoteSoundInstance(NoteSound noteSound, float pitch, NoteSoundMetadata meta, double playDistSqr,
            Optional<Integer> initiatorId, Optional<InitiatorID> oInitiatorId, PedalChain pedalChain,
            UnaryOperator<Sound> withPedals, @Nullable float[] volumeRamp
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
        this.pedalChain = pedalChain;
        this.withPedals = withPedals;
        this.volumeRamp = (volumeRamp == null || volumeRamp.length == 0) ? null : volumeRamp;
        if (this.volumeRamp != null)
            this.volume = rampedVolume();
        this.fadeTime = Mth.clamp((float) pedalChain.tailSeconds(), FADE_TIME, MAX_PEDAL_FADE_TIME);

        this.sourcePos = meta.pos().getCenter();
        // A held instrument's notes come from where its player is; other notes come from their block
        final ClientLevel level = Minecraft.getInstance().level;
        this.originEntity = initiatorId
                .map((id) -> (level == null) ? null : level.getEntity(id))
                .filter((entity) -> entity.blockPosition().equals(meta.pos()))
                .orElse(null);

        if (noteSound.usesStereo(playDistSqr)) {
            // Stereo plays in the listener's head, and crossfades into a positioned Mono with distance
            this.attenuation = Attenuation.NONE;
            this.relative = true;

            this.x = 0;
            this.y = 0;
            this.z = 0;

            this.monoCrossfade = new CrossfadeMonoSoundInstance(noteSound.getMono(), this, pitch, pedalChain, withPedals);
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
        if (dampened) {
            currentVolume -= startVolume / (fadeTime * 20.0f);

            if (currentVolume <= 0) {
                currentVolume = 0;
                volume = 0;
                stopNote();
                return;
            }

            volume = currentVolume;
        }

        if (volumeRamp != null)
            volume = rampedVolume();
        updateCrossfade();
        ticks++;
    }

    /**
     * @return The note's volume, as far as it has faded in by its volume ramp
     */
    private float rampedVolume() {
        return (volumeRamp == null) ? currentVolume
            : currentVolume * volumeRamp[Math.min(ticks, volumeRamp.length - 1)];
    }

    /**
     * Fades the Stereo sound with distance, and hands the rest of the volume to the Mono crossfade
     */
    private void updateCrossfade() {
        if (monoCrossfade == null)
            return;

        final double dist = NoteSound.listenerPos().distanceTo(sourcePos);
        final float rampedVolume = rampedVolume();
        volume = rampedVolume * NoteSound.stereoGain(dist);
        monoCrossfade.update(sourcePos, rampedVolume * NoteSound.crossfadeMonoGain(dist));
    }

    // A note that fades in may start silent, and must still start so it can
    @Override
    public boolean canStartSilent() {
        return volumeRamp != null;
    }

    public Optional<CrossfadeMonoSoundInstance> getMonoCrossfade() {
        return Optional.ofNullable(monoCrossfade);
    }

    @Override
    public Vec3 getWorldOrigin() {
        return sourcePos;
    }

    @Override
    public @Nullable Entity getOriginEntity() {
        return originEntity;
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