package com.stump.songcraft_instruments.sound.held;

import com.stump.songcraft_instruments.client.util.ClientUtil;
import com.stump.songcraft_instruments.particle.ModParticles;
import com.stump.songcraft_instruments.sound.CrossfadeMonoSoundInstance;
import com.stump.songcraft_instruments.sound.DampenableSoundInstance;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound.Phase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class HeldNoteSoundInstance extends AbstractTickableSoundInstance implements DampenableSoundInstance {
    public final HeldNoteSound heldSoundContainer;
    public final HeldNoteSound.Phase phase;
    private int particleTimer = 10;

    public final ResourceLocation instrumentId;

    public final Optional<Entity> initiator;
    public final InitiatorID initiatorId;

    /**
     * The origin of the sound. May be empty
     * for the initiator's position.
     */
    public final Optional<BlockPos> soundOrigin;
    public final int notePitch;
    public final int particleColor;
    private final float startVolume;

    private boolean released;
    private boolean dampened;

    /**
     * A positioned Mono sound this Stereo sound crossfades into with distance.
     * Null when this sound is not Stereo.
     */
    private @Nullable CrossfadeMonoSoundInstance monoCrossfade;
    private Vec3 crossfadePos;
    /**
     * The distance-based volume multiplier of this sound, applied on top of {@link #volume}
     */
    private float distanceGain = 1;

    /**
     * @param initiator The initiator of the sound. Empty for a non-player initiator.
     *                  Value must be present if {@code soundOrigin} is empty.
     * @param soundOrigin The block position of where the sound was originated from.
     *                    Value must be present if {@code initiator} is empty.
     */
    protected HeldNoteSoundInstance(HeldNoteSound heldSoundContainer, HeldNoteSound.Phase phase,
                                    int notePitch, int particleColor, float startVolume, float volume,
                                    @Nullable Entity initiator, @Nullable BlockPos soundOrigin,
                                    InitiatorID initiatorId, ResourceLocation instrumentId,
                                    int timeAlive, boolean released) {
        super(
            heldSoundContainer.getSound(phase).getByDistance(distFromSourceSqr(soundOrigin, initiator)),
            NoteSound.INSTRUMENT_SOUND_SOURCE,
            SoundInstance.createUnseededRandom()
        );

        this.initiatorId = initiatorId;
        this.instrumentId = instrumentId;

        this.heldSoundContainer = heldSoundContainer;
        this.phase = phase;
        this.overallTimeAlive = timeAlive;

        this.initiator = Optional.ofNullable(initiator);
        this.soundOrigin = Optional.ofNullable(soundOrigin);

        this.startVolume = startVolume;
        this.volume = volume;
        this.notePitch = notePitch;
        this.particleColor = particleColor;
        this.pitch = NoteSound.getPitchByNoteOffset(notePitch);

        this.released = released;


        final NoteSound sound = heldSoundContainer.getSound(phase);
        if (sound.usesStereo(distFromSourceSqr())) {
            // Stereo plays in the listener's head, and crossfades into a positioned Mono with distance
            attenuation = Attenuation.NONE;
            relative = true;
            x = y = z = 0;

            monoCrossfade = new CrossfadeMonoSoundInstance(sound.getMono(), this, pitch, false);
            crossfadePos = getSourcePos();
            updateCrossfade();
        } else if (distFromSourceSqr() < Mth.square(NoteSound.LOCAL_RANGE)) {
            // Very close; play relative
            attenuation = Attenuation.NONE;
            relative = true;
            x = y = z = 0;
        } else {
            // Not close; play local
            attenuation = Attenuation.LINEAR;
            relative = false;

            this.soundOrigin.ifPresentOrElse(
                (loc) -> {
                    x = loc.getX();
                    y = loc.getY();
                    z = loc.getZ();
                },
                this::toInitiatorPos
            );
        }
    }

    /**
     * A held note sound instance for 3rd party trigger
     * @param initiator The initiator of the sound. Empty for a non-player initiator.
     *                  Value must be present if {@code soundOrigin} is empty.
     * @param soundOrigin The block position of where the sound was originated from.
     *                    Value must be present if {@code initiator} is empty.
     */
    public HeldNoteSoundInstance(HeldNoteSound heldSoundContainer, HeldNoteSound.Phase phase,
                                 int notePitch, int particleColor, float startVolume, float volume,
                                 @Nullable Entity initiator, @Nullable BlockPos soundOrigin,
                                 InitiatorID initiatorId, ResourceLocation instrumentId) {
        this(
            heldSoundContainer,
            phase, notePitch, particleColor, startVolume, volume,
            initiator, soundOrigin, initiatorId, instrumentId,
            0, false
        );
    }


    public void queueAndAddInstance() {
        Minecraft.getInstance().getSoundManager().queueTickingSound(this);
        if (monoCrossfade != null)
            Minecraft.getInstance().getSoundManager().queueTickingSound(monoCrossfade);
        ClientUtil.stopMusicIfClose(
            soundOrigin.orElseGet(initiator.map(Entity::blockPosition)::get)
        );
        addSoundInstance();
    }

    /**
     * Adds a new held sound to the cached held sounds.
     * Its identifier will either be the initiator's UUID
     * or the block position string.
     */
    public void addSoundInstance() {
        HeldNoteSounds.put(initiatorId, heldSoundContainer, notePitch, this);
    }
    protected void removeSoundInstance() {
        HeldNoteSounds.release(initiatorId, heldSoundContainer, notePitch, this);
    }

    /**
     * Marks this held sound as being released
     */
    public void setReleased() {
        if (released)
            return;

        this.released = true;

        // Play release sound, if applicable.
        // Only a 'hold' sound type may play a release.
        if (phase == Phase.HOLD) {

            if (heldSoundContainer.release() != null) {
                final Vec3 pos = getSourcePos();

                heldSoundContainer.release().playLocally(
                    pitch, volume,
                    new BlockPos((int) pos.x, (int) pos.y, (int) pos.z)
                );
            }

        }
    }

    @Override
    public void dampen() {
        if (dampened || released)
            return;

        dampened = true;
        released = true;
    }

    @Override
    public boolean isDampened() {
        return dampened;
    }

    public boolean isReleased() {
        return released;
    }


    protected static double distFromSourceSqr(@Nullable BlockPos soundOrigin, @Nullable Entity initiator) {
        return Minecraft.getInstance().player.position().distanceToSqr(getSourcePos(soundOrigin, initiator));
    }
    public double distFromSourceSqr() {
        return Minecraft.getInstance().player.position().distanceToSqr(getSourcePos());
    }

    protected static Vec3 getSourcePos(@Nullable BlockPos soundOrigin, @Nullable Entity initiator) {
        return (soundOrigin == null) ? initiator.position() : soundOrigin.getCenter();
    }
    protected Vec3 getSourcePos() {
        return getSourcePos(soundOrigin.orElse(null), initiator.orElse(null));
    }


    protected int timeAlive = 0, overallTimeAlive;
    @Override
    public void tick() {
        toInitiatorPos();

        handleChainHolding();

        if (released) {
            float fadeOutMultiplier = 1;
            float fhft = heldSoundContainer.fullHoldFadeoutTime() * 20;

            // Lesser the significance of hold in the first FULL_HOLD_FADE_OUT_TIME ticks
            // Basically fade in the fade out
            if ((phase == Phase.HOLD) && (fhft != 0)) {
                if (overallTimeAlive < fhft) {
                    fadeOutMultiplier = 1 / ((overallTimeAlive + 1) / fhft);
                }
            }

            volume -= heldSoundContainer.releaseFadeOut() * fadeOutMultiplier;
            if (volume <= 0) {
                stopHeld();
                return;
            }
        } else {
            if (phase == Phase.HOLD) {
                if (++particleTimer >= 10) {
                    particleTimer = 0;
                    spawnNoteParticle();
                }
            }
        }

        updateCrossfade();

        timeAlive++;
        overallTimeAlive++;
    }

    /**
     * Fades the Stereo sound with distance, and hands the rest of the volume to the Mono crossfade
     */
    protected void updateCrossfade() {
        if (monoCrossfade == null)
            return;

        // Same as toInitiatorPos: a released sound stays where it was "blown"
        if (!released)
            crossfadePos = getSourcePos();

        final double dist = NoteSound.listenerPos().distanceTo(crossfadePos);
        distanceGain = NoteSound.stereoGain(dist);
        monoCrossfade.update(crossfadePos, volume * NoteSound.crossfadeMonoGain(dist));
    }

    protected boolean chainedHolding = false;
    protected void handleChainHolding() {
        if (chainedHolding || (pitch == 0)) // if, for some reason, ig
            return;

        switch (phase) {
            case ATTACK: {
                // Attack wants to chain the first hold:
                if (timeAlive == (int)(heldSoundContainer.holdDelay() * 20)) {
                    queueHoldPhase(false);
                    chainedHolding = true;
                }
                break;
            }
            case HOLD: {
                // Hold wants to chain the next hold:
                if ((timeAlive * pitch) >= (int)((heldSoundContainer.holdDuration() + heldSoundContainer.chainedHoldDelay()) * 20)) {
                    queueHoldPhase(heldSoundContainer.decay() > 0);
                    chainedHolding = true;

                    // We now don't need to cache it anymore.
                    removeSoundInstance();
                }
                break;
            }
        }
    }

    protected void queueHoldPhase(final boolean decreaseVol) {
        float decay = decreaseVol ? (heldSoundContainer.decay() * startVolume) : 0;
        float nextVolume = volume - decay;
        if (nextVolume <= 0)
            return;

        new HeldNoteSoundInstance(
            heldSoundContainer, Phase.HOLD, notePitch, particleColor, startVolume, nextVolume,
            initiator.orElse(null), soundOrigin.orElse(null),
            initiatorId, instrumentId,
            overallTimeAlive, released
        ).queueAndAddInstance();
    }

    protected void toInitiatorPos() {
        if (relative)
            return;
        if (soundOrigin.isPresent() || initiator.isEmpty())
            return;
        // "Blown air" at the same location
        if (released)
            return;

        x = initiator.get().getX();
        y = initiator.get().getY();
        z = initiator.get().getZ();
    }

    // We don't want to randomly distort this stuff unlike the parent
    @Override
    public float getVolume() {
        return volume * distanceGain;
    }
    /**
     * @return The volume of this note, without any distance-based fading
     */
    public float getBaseVolume() {
        return volume;
    }
    @Override
    public float getPitch() {
        return pitch;
    }

    // For some reason 'stop' is final...
    public void stopHeld() {
        stop();
        removeSoundInstance();
    }

    private void spawnNoteParticle() {
        if (initiator.isEmpty())
            return;

        Entity entity = initiator.get();
        var level = Minecraft.getInstance().level;
        if (level == null)
            return;

        double xOffset = (level.random.nextDouble() - 0.5) * 0.30;
        double yOffset = (level.random.nextDouble() - 0.5) * 0.30;
        double zOffset = (level.random.nextDouble() - 0.5) * 0.30;

        float bodyYaw = entity.getYRot();
        double radians = Math.toRadians(bodyYaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);

        int rgb = particleColor;

        level.addParticle(
                ModParticles.CUSTOM_NOTE.get(),
                entity.getX() + forwardX * 0.6 + xOffset,
                entity.getY() + 1.3 + yOffset,
                entity.getZ() + forwardZ * 0.6 + zOffset,
                rgb,        // dx = packed color
                0.15,       // dy = size
                0           // dz unused
        );
    }
}
