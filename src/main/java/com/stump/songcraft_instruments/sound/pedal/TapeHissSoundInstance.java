package com.stump.songcraft_instruments.sound.pedal;

import com.stump.songcraft_instruments.networking.packet.S2CTapeDeckPacket.Emitter;
import com.stump.songcraft_instruments.pedal.LofiParams;
import com.stump.songcraft_instruments.pedal.PedalEffect;
import com.stump.songcraft_instruments.sound.NoteSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A Lo-fi tape's hiss, as heard by the local player (see {@code TapeDeckPlayer}): one sound however many places the
 * tape is heard from, like the room tone of the band. It isn't positioned; instead it's louder the nearer the player is
 * to the nearest of them (an instrument or speaker playing through the tape), fading out at the distance notes do.
 * <p>Fades in, or spins up like a tape motor getting to speed; and fades out, or winds down in pitch like a tape
 * stopping.</p>
 */
@OnlyIn(Dist.CLIENT)
public class TapeHissSoundInstance extends AbstractTickableSoundInstance {
    /**
     * The hiss sound file's own loudness (its RMS), which {@link LofiParams#hissGain} is relative to full scale from
     */
    private static final double FILE_GAIN = Math.pow(10, -18 / 20.0);
    /**
     * How far from the nearest place the hiss fades out to silence, the same as instrument notes
     */
    private static final double FADE_DISTANCE = 32;
    /**
     * How long a spin-up takes, and the pitch the motor starts from
     */
    private static final int SPIN_UP_TICKS = 7;
    private static final float SPIN_UP_PITCH = 0.55f;
    /**
     * How long a wind-down takes, and the pitch the motor slows to before it's silent
     */
    private static final int WIND_DOWN_TICKS = 14;
    private static final float WIND_DOWN_PITCH = 0.12f;
    private static final int FADE_TICKS = 5;
    /**
     * Never fully silent while rolling: vanilla stops sounds whose volume reaches 0 when volume settings change
     */
    private static final float MIN_VOLUME = 1e-4f;

    private enum State { STARTING, ROLLING, STOPPING }

    private double hissGain;
    private float deckVolume;
    private List<Emitter> emitters;

    private State state = State.STARTING;
    /**
     * Whether it starts and stops with the motor (spinning up and winding down) rather than by fading
     */
    private final boolean motorStart;
    private boolean motorStop;
    private int stateTicks = 0;

    public TapeHissSoundInstance(final SoundEvent hiss, final List<Emitter> emitters, final PedalEffect effect,
                                 final float deckVolume, final boolean motorStart) {
        super(hiss, NoteSound.INSTRUMENT_SOUND_SOURCE, SoundInstance.createUnseededRandom());
        this.motorStart = motorStart;
        setSettings(effect, deckVolume, emitters);

        this.looping = true;
        this.delay = 0;
        // Heard as room tone rather than from a spot: its volume follows the distance itself, see updateLevel
        this.relative = true;
        this.attenuation = Attenuation.NONE;
        this.x = this.y = this.z = 0;
        updateLevel();
    }

    /**
     * Changes the tape's settings: its Age, which sets how loud the hiss is, the volume it plays at,
     * and the places it's heard from
     */
    public void setSettings(final PedalEffect effect, final float deckVolume, final List<Emitter> emitters) {
        this.hissGain = LofiParams.of(effect).hissGain();
        this.deckVolume = deckVolume;
        this.emitters = List.copyOf(emitters);
    }

    /**
     * Stops the hiss: winding the motor down, or fading out
     */
    public void stopTape(final boolean withMotor) {
        if (state == State.STOPPING)
            return;
        state = State.STOPPING;
        motorStop = withMotor;
        stateTicks = 0;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        stateTicks++;
        if (state == State.STARTING && stateTicks >= (motorStart ? SPIN_UP_TICKS : FADE_TICKS)) {
            state = State.ROLLING;
            stateTicks = 0;
        } else if (state == State.STOPPING && stateTicks >= (motorStop ? WIND_DOWN_TICKS : FADE_TICKS)) {
            stop();
            return;
        }

        updateLevel();
    }

    private void updateLevel() {
        float level = 1;
        float pitch = 1;
        switch (state) {
            case STARTING -> {
                final float t = (float) stateTicks / (motorStart ? SPIN_UP_TICKS : FADE_TICKS);
                level = t;
                if (motorStart)
                    pitch = SPIN_UP_PITCH + (1 - SPIN_UP_PITCH) * easeOut(t);
            }
            case STOPPING -> {
                final float t = (float) stateTicks / (motorStop ? WIND_DOWN_TICKS : FADE_TICKS);
                level = 1 - t;
                if (motorStop)
                    pitch = 1 - (1 - WIND_DOWN_PITCH) * t * t;
            }
            default -> {}
        }

        final float proximity = (float) proximity();
        this.volume = Math.max(MIN_VOLUME, (float) (hissGain / FILE_GAIN) * deckVolume * level * proximity);
        this.pitch = pitch;
    }

    /**
     * @return From 1 (at the nearest place) to 0 (at {@link #FADE_DISTANCE} or further), falling off linearly like notes
     */
    private double proximity() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
            return 0;

        final Vec3 nearest = nearestOf(emitters, minecraft.player.position());
        return (nearest == null) ? 0
            : Math.max(0, 1 - nearest.distanceTo(minecraft.player.position()) / FADE_DISTANCE);
    }

    /**
     * @return Where the nearest of the places is to the given point, or null if none of them can be found
     */
    @Nullable
    public static Vec3 nearestOf(final List<Emitter> emitters, final Vec3 to) {
        Vec3 nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (final Emitter emitter : emitters) {
            final Vec3 at = positionOf(emitter);
            if (at == null)
                continue;
            final double distance = at.distanceToSqr(to);
            if (distance < nearestDistance) {
                nearest = at;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    /**
     * @return Where a place is: its block, or the entity it follows; null if that entity isn't loaded here
     */
    @Nullable
    public static Vec3 positionOf(final Emitter emitter) {
        if (emitter.pos() != null)
            return emitter.pos().getCenter();

        final Minecraft minecraft = Minecraft.getInstance();
        final Entity entity = (minecraft.level == null) ? null : minecraft.level.getEntity(emitter.entityId());
        return (entity == null) ? null : entity.position();
    }

    private static float easeOut(final float t) {
        return 1 - (1 - t) * (1 - t);
    }
}
