package com.stump.songcraft_instruments.sound.held;

import com.stump.songcraft_instruments.client.util.ClientUtil;
import com.stump.songcraft_instruments.particle.ModParticles;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.pedal.PedalEffectType;
import com.stump.songcraft_instruments.sound.CrossfadeMonoSoundInstance;
import com.stump.songcraft_instruments.sound.DampenableSoundInstance;
import com.stump.songcraft_instruments.sound.NearestCopyFilter;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.WorldOriginSoundInstance;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound.Phase;
import com.stump.songcraft_instruments.sound.pedal.PedalBufferCache;
import com.stump.songcraft_instruments.sound.pedal.PedalSoundInstance;
import com.stump.songcraft_instruments.sound.pedal.PedalSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

@OnlyIn(Dist.CLIENT)
public class HeldNoteSoundInstance extends AbstractTickableSoundInstance
        implements DampenableSoundInstance, WorldOriginSoundInstance, PedalSoundInstance {
    public final HeldNoteSound heldSoundContainer;
    public final HeldNoteSound.Phase phase;
    private int particleTimer = 10;

    public final ResourceLocation instrumentId;

    public final Optional<Entity> initiator;
    public final InitiatorID initiatorId;
    /**
     * The initiators of other copies of this note that were dropped in favor of this one, see {@link NearestCopyFilter}.
     * The note is also stored under these, so their releases and dampens reach it.
     */
    private final Set<InitiatorID> mergedInitiators = new HashSet<>();

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
     * The effects pedals this note (and its holds and release) are played through, see {@link PedalSounds}.
     * Its delays' echoes only apply to its release, while its reverb is baked into its holds
     * (see {@link PedalSounds#holdWithPedals}).
     */
    private final PedalChain pedalChain;
    /**
     * Whether this is the note's first hold, which no hold before it rings into (see {@link PedalSounds#holdWithPedals})
     */
    private final boolean firstHold;
    /**
     * How many ticks after it starts this sound chains its next hold, see {@link HeldNoteSound#holdPeriodTicks}
     */
    private final int holdPeriodTicks;

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
     * @param firstHold Whether this is the note's first hold, rather than one chained from another hold
     */
    protected HeldNoteSoundInstance(HeldNoteSound heldSoundContainer, HeldNoteSound.Phase phase,
                                    int notePitch, int particleColor, float startVolume, float volume,
                                    @Nullable Entity initiator, @Nullable BlockPos soundOrigin,
                                    InitiatorID initiatorId, ResourceLocation instrumentId,
                                    int timeAlive, boolean released, PedalChain pedalChain, boolean firstHold) {
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
        this.pedalChain = pedalChain;
        this.firstHold = firstHold;
        this.holdPeriodTicks = heldSoundContainer.holdPeriodTicks(pitch);


        final NoteSound sound = heldSoundContainer.getSound(phase);
        if (sound.usesStereo(distFromSourceSqr())) {
            // Stereo plays in the listener's head, and crossfades into a positioned Mono with distance
            attenuation = Attenuation.NONE;
            relative = true;
            x = y = z = 0;

            monoCrossfade = new CrossfadeMonoSoundInstance(sound.getMono(), this, pitch, pedalChain, this::withPedals);
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
            PedalChain.NONE
        );
    }
    /**
     * A held note sound instance for 3rd party trigger, played through the given pedals
     */
    public HeldNoteSoundInstance(HeldNoteSound heldSoundContainer, HeldNoteSound.Phase phase,
                                 int notePitch, int particleColor, float startVolume, float volume,
                                 @Nullable Entity initiator, @Nullable BlockPos soundOrigin,
                                 InitiatorID initiatorId, ResourceLocation instrumentId,
                                 PedalChain pedalChain) {
        this(
            heldSoundContainer,
            phase, notePitch, particleColor, startVolume, volume,
            initiator, soundOrigin, initiatorId, instrumentId,
            0, false, pedalChain, true
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
        mergedInitiators.forEach((merged) -> HeldNoteSounds.put(merged, heldSoundContainer, notePitch, this));
    }
    protected void removeSoundInstance() {
        HeldNoteSounds.release(initiatorId, heldSoundContainer, notePitch, this);
        mergedInitiators.forEach((merged) -> HeldNoteSounds.release(merged, heldSoundContainer, notePitch, this));
    }

    /**
     * Also stores this note under the given initiators. Must be called before {@link #queueAndAddInstance()}
     */
    public void addMergedInitiators(final Set<InitiatorID> initiators) {
        mergedInitiators.addAll(initiators);
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

                // The release gets every pedal, echoes included
                heldSoundContainer.release().playLocally(
                    pitch, volume,
                    new BlockPos((int) pos.x, (int) pos.y, (int) pos.z),
                    pedalChain
                );
            }

        }

        // A hold's reverb is baked in, and rings on from its own release tail as it fades out
        final boolean reverbTail = (phase == Phase.HOLD) && pedalChain.has(PedalEffectType.REVERB);
        if (reverbTail)
            playReleaseTail();

        // Without a release sound, a delay's echoes come from a slice of the held sound instead (as does the reverb
        // of an attack). An attack that hasn't started its hold yet stands in for it; once it has, the hold plays them
        final boolean releasesHold = (phase == Phase.HOLD) || (phase == Phase.ATTACK && !chainedHolding);
        final boolean sliceTail = pedalChain.has(PedalEffectType.DELAY) || (pedalChain.isTimeBased() && !reverbTail);
        if (releasesHold && heldSoundContainer.release() == null && sliceTail) {
            final Vec3 pos = getSourcePos();

            heldSoundContainer.hold().playLocally(
                pitch, volume,
                new BlockPos((int) pos.x, (int) pos.y, (int) pos.z),
                pedalChain, true
            );
        }
    }

    /**
     * Plays what this note's reverb rings on with as it fades out (see {@link PedalSounds#releaseTailWithPedals}).
     * <p>Its holds' baked reverb fades out with them, so the tail fades in as they do, by just as much as the reverb
     * the holds have lost by then: the reverb, dying away at its own rate, takes over from the holds fading at
     * theirs. The two are usually at a random phase to each other, so it's their power that adds up.</p>
     * <p>A note let go before its reverb has built up rings on with as much of it as it has. The reverb builds up,
     * and dies away, as any reverb does, at the rate its decay sets.</p>
     */
    private void playReleaseTail() {
        final double decay = pedalChain.reverbDecaySeconds();
        if (volume <= 0 || decay <= 0)
            return;
        final float[] fade = releaseFade(volume, overallTimeAlive);

        // The reverb's energy, out of what it holds to while the note's held, as it dies away each tick and is fed
        // by what's left of the note
        final double keep = Math.exp(-6 * Math.log(10) / (decay * 20));
        final double builtUp = Math.sqrt(1 - Math.pow(10, -6 * (overallTimeAlive / 20.0) / decay));
        final float[] ramp = new float[fade.length + 1];
        double energy = 1;
        for (int i = 0; i < fade.length; i++) {
            final double left = fade[i] / volume;
            energy = energy * keep + (1 - keep) * left * left;
            ramp[i] = (float) (builtUp * Math.sqrt(Math.max(0, 1 - left * left / energy)));
        }
        // Once the note's gone, all that's left of the reverb is the tail
        ramp[fade.length] = (float) builtUp;

        final int fadeTicks = fade.length + 1, periodTicks = holdPeriodTicks;
        final PedalChain whileHeld = pedalChain.withoutDelays();
        final float tailPitch = getPitch();
        final Vec3 pos = getSourcePos();
        heldSoundContainer.hold().playLocally(
            pitch, volume,
            new BlockPos((int) pos.x, (int) pos.y, (int) pos.z),
            pedalChain,
            (sound) -> PedalSounds.releaseTailWithPedals(sound, whileHeld, tailPitch, periodTicks, fadeTicks),
            ramp
        );
    }

    /**
     * The longest a fade is followed for, in ticks, in case a note somehow never fades
     */
    private static final int MAX_FADE_TICKS = 20 * 60;
    /**
     * @param fromVolume The note's volume as it's released
     * @param fromTimeAlive How long the note has been held as it's released, see {@link #overallTimeAlive}
     * @return The note's volume after each tick of its fade once released, as {@link #tick} fades it,
     * for as long as it's still heard
     */
    private float[] releaseFade(final float fromVolume, final int fromTimeAlive) {
        final float[] fade = new float[MAX_FADE_TICKS];
        float fading = fromVolume;
        int ticks = 0;
        while (ticks < MAX_FADE_TICKS) {
            fading -= releaseFadeStep(fromTimeAlive + ticks);
            if (fading <= 0)
                break;
            fade[ticks++] = fading;
        }
        return Arrays.copyOf(fade, ticks);
    }
    /**
     * @param timeAlive How long the note has been held, see {@link #overallTimeAlive}
     * @return How much the note's volume drops on a tick, once released
     */
    private float releaseFadeStep(final int timeAlive) {
        float fadeOutMultiplier = 1;
        float fhft = heldSoundContainer.fullHoldFadeoutTime() * 20;

        // Lesser the significance of hold in the first FULL_HOLD_FADE_OUT_TIME ticks
        // Basically fade in the fade out
        if ((phase == Phase.HOLD) && (fhft != 0)) {
            if (timeAlive < fhft) {
                fadeOutMultiplier = 1 / ((timeAlive + 1) / fhft);
            }
        }

        return heldSoundContainer.releaseFadeOut() * fadeOutMultiplier;
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

    @Override
    public @Nullable Entity getOriginEntity() {
        return soundOrigin.isEmpty() ? initiator.orElse(null) : null;
    }

    @Override
    public Vec3 getWorldOrigin() {
        if (!relative)
            return new Vec3(x, y, z);

        // A released Stereo sound stays where it was "blown", same as its crossfade
        return (crossfadePos != null) ? crossfadePos : getSourcePos();
    }


    /**
     * @return The note's whole chain, though only its release gets its delays' echoes.
     * Live effects (like a tremolo) apply to all of it, as it's held.
     */
    @Override
    public PedalChain getPedalChain() {
        return pedalChain;
    }

    @Override
    public WeighedSoundEvents resolve(final SoundManager manager) {
        final WeighedSoundEvents events = super.resolve(manager);
        sound = withPedals(sound);
        prepareNextHold(manager);
        return events;
    }

    /**
     * @return The rendered copy this note plays of one of its sounds (its Stereo sound, or its Mono crossfade's)
     * through its pedals. Its delays' echoes only come with its release, as they'd pile up as its holds repeat,
     * while its reverb is baked into its holds. Its attack plays once, so it rings on with its own reverb.
     */
    private Sound withPedals(final Sound sound) {
        final PedalChain chain = pedalChain.withoutDelays();
        return (phase == Phase.HOLD)
            ? PedalSounds.holdWithPedals(sound, chain, getPitch(), holdPeriodTicks, firstHold)
            : PedalSounds.withPedals(sound, chain, getPitch());
    }

    /**
     * Starts rendering the hold that follows this sound, if it's a different copy than this note has played so far
     * (with a reverb baked into its holds, see {@link PedalSounds#holdWithPedals}), so it's ready when it starts:
     * the first hold starts right after the attack, and the second, the first not to be the first, after it.
     * The first hold also starts rendering the note's release tail.
     */
    private void prepareNextHold(final SoundManager manager) {
        if (!pedalChain.has(PedalEffectType.REVERB) || (phase == Phase.HOLD && !firstHold))
            return;

        final PedalChain whileHeld = pedalChain.withoutDelays();
        final float pitch = getPitch();
        if (phase == Phase.ATTACK) {
            prepareHolds(manager, (sound) -> PedalSounds.holdWithPedals(sound, whileHeld, pitch, holdPeriodTicks, true));
            return;
        }

        prepareHolds(manager, (sound) -> PedalSounds.holdWithPedals(sound, whileHeld, pitch, holdPeriodTicks, false));
        // Its release tail too, for the fade it's most likely to have: let go once past the quicker early fades
        final int fadeTicks = releaseFade(volume, Math.max(overallTimeAlive,
            (int) Math.ceil(heldSoundContainer.fullHoldFadeoutTime() * 20))).length + 1;
        prepareHolds(manager, (sound) -> PedalSounds.releaseTailWithPedals(sound, whileHeld, pitch, holdPeriodTicks, fadeTicks));
    }
    /**
     * Starts rendering the given copy of the note's hold sound, as it'd be played from here: Stereo or Mono,
     * and a Stereo one's Mono crossfade
     */
    private void prepareHolds(final SoundManager manager, final UnaryOperator<Sound> withPedals) {
        final NoteSound hold = heldSoundContainer.hold();
        final double distSqr = distFromSourceSqr();
        prepareHold(manager, hold.getByDistance(distSqr), withPedals);
        if (hold.usesStereo(distSqr))
            prepareHold(manager, hold.getMono(), withPedals);
    }
    private void prepareHold(final SoundManager manager, @Nullable final SoundEvent event,
                             final UnaryOperator<Sound> withPedals) {
        final WeighedSoundEvents events = (event == null) ? null : manager.getSoundEvent(event.getLocation());
        if (events == null)
            return;

        final Sound rendered = withPedals.apply(events.getSound(random));
        if (PedalSounds.isFxFile(rendered.getPath()))
            PedalBufferCache.get(rendered.getPath());
    }


    protected int timeAlive = 0, overallTimeAlive;
    @Override
    public void tick() {
        toInitiatorPos();

        handleChainHolding();

        if (released) {
            volume -= releaseFadeStep(overallTimeAlive);
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
                    queueHoldPhase(false, true);
                    chainedHolding = true;
                }
                break;
            }
            case HOLD: {
                // Hold wants to chain the next hold, once it's played through holdDuration + chainedHoldDelay.
                // Baked reverbs count on holds starting exactly this far apart (see PedalSounds#holdWithPedals)
                if (timeAlive >= holdPeriodTicks) {
                    queueHoldPhase(heldSoundContainer.decay() > 0, false);
                    chainedHolding = true;

                    // We now don't need to cache it anymore.
                    removeSoundInstance();
                }
                break;
            }
        }
    }

    /**
     * @param first Whether it's the note's first hold, chained from its attack
     */
    protected void queueHoldPhase(final boolean decreaseVol, final boolean first) {
        float decay = decreaseVol ? (heldSoundContainer.decay() * startVolume) : 0;
        float nextVolume = volume - decay;
        if (nextVolume <= 0)
            return;

        final HeldNoteSoundInstance next = new HeldNoteSoundInstance(
            heldSoundContainer, Phase.HOLD, notePitch, particleColor, startVolume, nextVolume,
            initiator.orElse(null), soundOrigin.orElse(null),
            initiatorId, instrumentId,
            overallTimeAlive, released, pedalChain, first
        );
        next.addMergedInitiators(mergedInitiators);
        next.queueAndAddInstance();
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

    // The parent applies the volume defined in sounds.json, which balances instruments and notes
    @Override
    public float getVolume() {
        return super.getVolume() * distanceGain;
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
