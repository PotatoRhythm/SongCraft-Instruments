package com.stump.songcraft_instruments.sound.pedal;

import com.stump.songcraft_instruments.networking.packet.S2CTapeDeckPacket.Emitter;
import com.stump.songcraft_instruments.pedal.LofiParams;
import com.stump.songcraft_instruments.pedal.PedalChain;
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
 * A Lo-fi tape deck's hiss, rolling at one of the places its instrument is heard (see {@code TapeDeckPlayer}).
 * <p>Fades in, or spins up like a tape motor getting to speed; and fades out, or winds down in pitch like a tape
 * stopping. It's played through the deck's Lo-fi effect, so the tape's dropouts dip the hiss along with the notes
 * (see {@link PedalLiveEffects}).</p>
 */
@OnlyIn(Dist.CLIENT)
public class TapeHissSoundInstance extends AbstractTickableSoundInstance implements PedalSoundInstance {
    /**
     * The hiss sound file's own loudness (its RMS), which {@link LofiParams#hissGain} is relative to full scale from
     */
    private static final double FILE_GAIN = Math.pow(10, -18 / 20.0);
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

    private final Emitter emitter;
    private final PedalChain chain;
    private double hissGain;
    private float deckVolume;

    private State state = State.STARTING;
    /**
     * Whether it starts and stops with the motor (spinning up and winding down) rather than by fading
     */
    private boolean motorStart, motorStop;
    private int stateTicks = 0;

    public TapeHissSoundInstance(final SoundEvent hiss, final Emitter emitter, final PedalEffect effect,
                                 final float deckVolume, final boolean motorStart) {
        super(hiss, NoteSound.INSTRUMENT_SOUND_SOURCE, SoundInstance.createUnseededRandom());
        this.emitter = emitter;
        // Its dropouts are applied live; its tone isn't rendered into the hiss, which is the tape itself
        this.chain = new PedalChain(List.of(effect));
        this.motorStart = motorStart;
        setSettings(effect, deckVolume);

        this.looping = true;
        this.delay = 0;
        this.attenuation = Attenuation.LINEAR;
        updatePosition();
        updateLevel();
    }

    public Emitter getEmitter() {
        return emitter;
    }

    /**
     * Changes the deck's settings: its Age, which sets how loud the hiss is, and the volume it plays at
     */
    public void setSettings(final PedalEffect effect, final float deckVolume) {
        this.hissGain = LofiParams.of(effect).hissGain();
        this.deckVolume = deckVolume;
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
    public PedalChain getPedalChain() {
        return chain;
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

        updatePosition();
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

        final Vec3 at = emitterPosition();
        this.volume = (at == null) ? MIN_VOLUME : Math.max(MIN_VOLUME, (float) (hissGain / FILE_GAIN) * deckVolume * level);
        this.pitch = pitch;
    }

    private void updatePosition() {
        final Vec3 at = emitterPosition();
        if (at == null)
            return;
        this.x = at.x;
        this.y = at.y;
        this.z = at.z;
    }

    @Nullable
    private Vec3 emitterPosition() {
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
