package com.stump.songcraft_instruments.sound.pedal;

import com.stump.songcraft_instruments.pedal.DelayParams;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.pedal.PedalEffect;
import com.stump.songcraft_instruments.sound.ChannelTwinSounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Renders a sound through a chain of pedal effects, ahead of time.
 * <p>Works on whole sounds rather than as they play, so it costs nothing while playing: each sound is rendered once
 * per pedal setting and cached (see {@link PedalBufferCache}). Effects are applied in signal order on floating point
 * samples, then a soft limiter keeps stacked effects from clipping harshly once converted back to 16-bit.</p>
 */
@OnlyIn(Dist.CLIENT)
final class PedalDsp {
    /**
     * Where the limiter starts to round off peaks, out of 1 (full scale)
     */
    private static final float LIMIT_THRESHOLD = 0.85f;
    /**
     * Each echo is darker than the last, as on an analog or tape delay: echoes pass through a low-pass at this
     * frequency (as heard, whatever the note's pitch) every time they repeat
     */
    private static final double DELAY_DAMPING_HZ = 4000;
    /**
     * The Bitcrusher's range: the bit depth its crush knob goes from and to, and the sample rate its downsample
     * knob goes from and to
     */
    private static final double CRUSH_MAX_BITS = 12, CRUSH_MIN_BITS = 2,
        DOWNSAMPLE_MAX_HZ = 32000, DOWNSAMPLE_MIN_HZ = 1000;
    /**
     * How much of a held note's sound its echo tail repeats (see {@link #echoSlice}), in seconds as heard
     */
    private static final double ECHO_SLICE_SECONDS = 0.3;
    /**
     * Fades at the edges of slices and cut-off tails, so they don't click
     */
    private static final double EDGE_FADE_SECONDS = 0.005;
    /**
     * Rendered tails end once every channel stays below this (about -80 dB)
     */
    private static final float TAIL_SILENCE = 1e-4f;

    private PedalDsp() {}

    /**
     * @param pitch The pitch the sound will be played at (what OpenAL resamples it by), so effects that play out
     *              over time come out with the right timing; see {@link PedalSounds}
     * @param echoTail Whether to render only the echoes of a short slice of the sound, for a held note's release
     * @return The given sound's samples, rendered through the chain, in the same format.
     * Effects that play out over time may make it longer.
     */
    static ByteBuffer render(final ChannelTwinSounds.Decoded sound, final PedalChain chain,
                             final float pitch, final boolean echoTail) {
        final int channelCount = sound.format().getChannels();
        final float sampleRate = sound.format().getSampleRate();

        float[][] channels = toFloat(sound.samples(), channelCount);
        if (echoTail)
            channels = echoSlice(channels, sampleRate, pitch);

        final int dryFrames = channels[0].length;
        final double dryRms = rms(channels, dryFrames);
        final float dryPeak = peak(channels);

        // A held note's own sound fades out as usual, so its echo tail leaves out the dry sound, but only once:
        // a second delay repeats the first one's echoes along with its own
        boolean dropDry = echoTail;
        for (final PedalEffect effect : chain.effects()) {
            switch (effect.type()) {
                case FUZZ -> fuzz(channels, sampleRate, effect);
                case BITCRUSH -> bitcrush(channels, sampleRate, effect);
                case DELAY -> {
                    channels = delay(channels, sampleRate, pitch, effect, dropDry);
                    dropDry = false;
                }
            }
        }

        channels = trimTail(channels, sampleRate, pitch, dryFrames);
        capLoudness(channels, dryRms, dryPeak, dryFrames);
        for (final float[] channel : channels)
            softLimit(channel);
        return toPcm(channels);
    }

    /**
     * Keeps pedals from ever making a sound louder than it is without them, in loudness or in peaks.
     * <p>The player's instrument volume is applied on top of this (and capped by Minecraft at full), so a note
     * played through pedals is never louder than the same note played at that volume without them.</p>
     * @param dryFrames How long the sound was before effects; loudness is compared over that much of it,
     *                  so an echo tail doesn't count as making it quieter
     */
    private static void capLoudness(final float[][] channels, final double dryRms, final float dryPeak,
                                    final int dryFrames) {
        final double wetRms = rms(channels, dryFrames);
        final float wetPeak = peak(channels);

        double scale = 1;
        if (wetRms > dryRms)
            scale = dryRms / wetRms;
        if (wetPeak * scale > dryPeak)
            scale = dryPeak / wetPeak;
        if (scale >= 1)
            return;

        for (final float[] samples : channels)
            for (int i = 0; i < samples.length; i++)
                samples[i] *= (float) scale;
    }


    //#region Effects

    /**
     * A fuzz box: the signal is boosted into a slightly asymmetric soft clipper, which squares off its waveform
     * (the bias adds the even harmonics of a real transistor fuzz), then darkened by the tone knob.
     * <p>The result is matched to the dry sound's loudness, so the level knob is relative to it rather than
     * to how much the fuzz itself boosted.</p>
     * <p>Knobs: drive (how hard it clips), tone (how bright it is), level (its loudness: fully up is as loud as
     * the dry sound, and it only turns down from there, see {@link #capLoudness}).</p>
     */
    private static void fuzz(final float[][] channels, final float sampleRate, final PedalEffect effect) {
        final float gain = dbToGain(6 + effect.knobFraction(0) * 40);
        final double toneHz = 700 * Math.pow(12000 / 700.0, effect.knobFraction(1));
        final float level = dbToGain(-24 + effect.knobFraction(2) * 24);

        final float bias = 0.12f;
        final float biasOffset = (float) Math.tanh(bias);

        // The input is coupled through a small capacitor, so lows don't turn into flub
        final float inputHp = onePoleCoefficient(100, sampleRate);
        final float dcBlock = 1 - (float) (2 * Math.PI * 20 / sampleRate);
        final float toneLp = onePoleCoefficient(toneHz, sampleRate);
        // A gate keeps the fuzz from turning a note's quiet tail (and its noise) into a hiss
        final float gateRelease = (float) Math.exp(-1 / (0.05 * sampleRate));
        final float gateOpen = dbToGain(-48), gateClosed = dbToGain(-60);

        final double dryRms = rms(channels);

        for (final float[] samples : channels) {
            float hpState = 0;
            float dcIn = 0, dcOut = 0;
            float lpState = 0;
            float envelope = 0;

            for (int i = 0; i < samples.length; i++) {
                final float x = samples[i];

                // High-pass: the input minus its low-passed self
                hpState += inputHp * (x - hpState);
                final float highPassed = x - hpState;

                final float abs = Math.abs(x);
                envelope = (abs > envelope) ? abs : envelope * gateRelease;
                final float gate = smoothstep(gateClosed, gateOpen, envelope);

                final float y = (float) Math.tanh(gain * highPassed + bias) - biasOffset;

                // The bias leaves a DC offset
                final float dc = y - dcIn + dcBlock * dcOut;
                dcIn = y;
                dcOut = dc;

                lpState += toneLp * (dc - lpState);
                samples[i] = lpState * gate;
            }
        }

        final double wetRms = rms(channels);
        final float scale = (wetRms > 1e-9) ? (float) (dryRms / wetRms) * level : 0;
        for (final float[] samples : channels)
            for (int i = 0; i < samples.length; i++)
                samples[i] *= scale;
    }

    /**
     * A bitcrusher: lo-fi, chiptune grit from cutting the sound's bit depth and sample rate.
     * <p>The sample rate is cut by holding each sample for a while (with no filtering, so the aliasing that makes
     * the grit is kept), and the bit depth by rounding samples to fewer levels. Quiet sounds round to silence,
     * so a note's quiet tail breaks up into stepped noise and then cuts out.</p>
     * <p>Knobs: crush (off, then from {@link #CRUSH_MAX_BITS} bits down to {@link #CRUSH_MIN_BITS}), downsample (off,
     * then from {@link #DOWNSAMPLE_MAX_HZ} down to {@link #DOWNSAMPLE_MIN_HZ}, at the sound file's own pitch), and mix
     * (how much of the crushed sound is heard over the original).</p>
     */
    private static void bitcrush(final float[][] channels, final float sampleRate, final PedalEffect effect) {
        // Either knob fully down turns its crushing off, rather than crushing a little
        final boolean crushBits = effect.knob(0) > 0, downsample = effect.knob(1) > 0;

        final double bits = CRUSH_MAX_BITS - (CRUSH_MAX_BITS - CRUSH_MIN_BITS) * effect.knobFraction(0);
        // Levels on each side of 0
        final float levels = (float) Math.pow(2, bits - 1);
        final double rate = DOWNSAMPLE_MAX_HZ * Math.pow(DOWNSAMPLE_MIN_HZ / DOWNSAMPLE_MAX_HZ, effect.knobFraction(1));
        final double holdFrames = downsample ? Math.max(1, sampleRate / rate) : 1;
        final float mix = effect.knobFraction(2);

        for (final float[] samples : channels) {
            // Starts due, so the first sample is taken
            double sinceTaken = holdFrames;
            float held = 0;

            for (int i = 0; i < samples.length; i++) {
                final float x = samples[i];
                if (++sinceTaken >= holdFrames) {
                    sinceTaken -= holdFrames;
                    held = crushBits ? Math.round(x * levels) / levels : x;
                }
                samples[i] = x + mix * (held - x);
            }
        }
    }

    /**
     * A delay: the note repeats after a set time, each echo quieter (by the feedback) and darker than the last.
     * <p>Rendered for the pitch the note plays at, so the echoes are spaced by the set time once resampled to it.
     * The echoes ring on past the end of the note, for up to {@link DelayParams#MAX_TAIL_SECONDS}.</p>
     * <p>Knobs: time (from {@link DelayParams#MIN_SECONDS} to {@link DelayParams#MAX_SECONDS}), feedback
     * (how many echoes), mix (how loud the first echo is).</p>
     * @param dropDry Whether to leave the note itself out, and keep only its echoes
     * @return The channels with the echoes, longer than the ones given
     */
    private static float[][] delay(final float[][] channels, final float sampleRate, final float pitch,
                                   final PedalEffect effect, final boolean dropDry) {
        final DelayParams params = DelayParams.of(effect);
        // Samples are played back pitch times as fast, so everything in them is pitch times as long here
        final double rate = sampleRate * pitch;
        final int delayFrames = Math.max(1, (int) Math.round(params.seconds() * rate));
        final int tailFrames = (int) Math.round(params.tailSeconds() * rate);
        final float feedback = (float) params.feedback(), mix = (float) params.mix();
        final float damping = onePoleCoefficient(DELAY_DAMPING_HZ / pitch, sampleRate);

        final int frames = channels[0].length;
        final float[][] result = new float[channels.length][frames + tailFrames];
        for (int c = 0; c < channels.length; c++) {
            final float[] in = channels[c], out = result[c];
            // What goes into the delay: the note, plus what's fed back of the echoes
            final float[] line = new float[out.length];
            float echo = 0;

            for (int i = 0; i < out.length; i++) {
                final float x = (i < frames) ? in[i] : 0;
                final float delayed = (i >= delayFrames) ? line[i - delayFrames] : 0;
                echo += damping * (delayed - echo);

                line[i] = x + feedback * echo;
                out[i] = (dropDry ? 0 : x) + mix * echo;
            }
        }
        return result;
    }

    /**
     * @return A short slice from inside the sound, faded in and out, for a held note's echo tail.
     * Held sounds loop, so any part of one stands for how it sounds when released.
     */
    private static float[][] echoSlice(final float[][] channels, final float sampleRate, final float pitch) {
        final int frames = channels[0].length;
        final int sliceFrames = Math.min(frames, (int) Math.round(ECHO_SLICE_SECONDS * sampleRate * pitch));
        final int start = Math.min(frames / 4, frames - sliceFrames);
        final int fadeIn = Math.min(sliceFrames / 2, (int) Math.round(EDGE_FADE_SECONDS * sampleRate * pitch));

        final float[][] slice = new float[channels.length][sliceFrames];
        for (int c = 0; c < channels.length; c++) {
            for (int i = 0; i < sliceFrames; i++) {
                // Fades out across the whole slice, like the note dying away
                final float fadeOut = 1 - (float) i / sliceFrames;
                final float in = (i < fadeIn) ? (float) i / fadeIn : 1;
                slice[c][i] = channels[c][start + i] * fadeOut * in;
            }
        }
        return slice;
    }

    /**
     * @return The channels, cut to at most {@link DelayParams#MAX_TAIL_SECONDS} past the original sound,
     * and without the silence their echoes leave at the end
     */
    private static float[][] trimTail(final float[][] channels, final float sampleRate, final float pitch,
                                      final int dryFrames) {
        final int frames = channels[0].length;
        if (frames <= dryFrames)
            return channels;

        final int maxFrames = dryFrames + (int) Math.round(DelayParams.MAX_TAIL_SECONDS * sampleRate * pitch);
        int end = Math.min(frames, maxFrames);
        int lastAudible = dryFrames - 1;
        for (final float[] samples : channels)
            for (int i = end - 1; i > lastAudible; i--)
                if (Math.abs(samples[i]) > TAIL_SILENCE) {
                    lastAudible = i;
                    break;
                }
        final boolean cutOff = (end < frames) && (lastAudible == end - 1);
        end = lastAudible + 1;
        if (end == frames)
            return channels;

        final int fade = Math.min(end - dryFrames, (int) Math.round(EDGE_FADE_SECONDS * sampleRate * pitch));
        final float[][] trimmed = new float[channels.length][];
        for (int c = 0; c < channels.length; c++) {
            trimmed[c] = Arrays.copyOf(channels[c], end);
            // A tail cut off while still ringing fades out, rather than clicking
            if (cutOff)
                for (int i = 0; i < fade; i++)
                    trimmed[c][end - 1 - i] *= (float) i / fade;
        }
        return trimmed;
    }

    //#endregion


    /**
     * Leaves the signal untouched below {@link #LIMIT_THRESHOLD}, and rounds off anything above it so it never
     * reaches full scale
     */
    private static void softLimit(final float[] samples) {
        final float headroom = 1 - LIMIT_THRESHOLD;
        for (int i = 0; i < samples.length; i++) {
            final float x = samples[i];
            final float abs = Math.abs(x);
            if (abs <= LIMIT_THRESHOLD)
                continue;

            final float over = (float) Math.tanh((abs - LIMIT_THRESHOLD) / headroom) * headroom;
            samples[i] = Math.copySign(LIMIT_THRESHOLD + over, x);
        }
    }


    //#region Helpers

    private static float dbToGain(final double db) {
        return (float) Math.pow(10, db / 20);
    }
    /**
     * @return The coefficient of a one-pole low-pass filter at the given frequency
     */
    private static float onePoleCoefficient(final double hz, final float sampleRate) {
        return (float) (1 - Math.exp(-2 * Math.PI * Math.min(hz, sampleRate * 0.45) / sampleRate));
    }
    private static float smoothstep(final float edge0, final float edge1, final float x) {
        final float t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }
    private static float peak(final float[][] channels) {
        float peak = 0;
        for (final float[] samples : channels)
            for (final float sample : samples)
                peak = Math.max(peak, Math.abs(sample));
        return peak;
    }
    private static double rms(final float[][] channels) {
        return rms(channels, Integer.MAX_VALUE);
    }
    /**
     * @return The RMS of the channels' first {@code frames} samples
     */
    private static double rms(final float[][] channels, final int frames) {
        double sum = 0;
        long count = 0;
        for (final float[] samples : channels) {
            final int length = Math.min(frames, samples.length);
            for (int i = 0; i < length; i++)
                sum += samples[i] * samples[i];
            count += length;
        }
        return (count == 0) ? 0 : Math.sqrt(sum / count);
    }

    /**
     * @return Each channel's samples from -1 to 1, de-interleaved
     */
    private static float[][] toFloat(final ByteBuffer pcm, final int channelCount) {
        final ShortBuffer in = pcm.duplicate().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        final int frames = in.remaining() / channelCount;

        final float[][] channels = new float[channelCount][frames];
        for (int frame = 0; frame < frames; frame++)
            for (int c = 0; c < channelCount; c++)
                channels[c][frame] = in.get() / 32768f;
        return channels;
    }
    /**
     * @return The channels as interleaved 16-bit little-endian PCM
     */
    private static ByteBuffer toPcm(final float[][] channels) {
        final int frames = channels[0].length;
        final ByteBuffer result = BufferUtils.createByteBuffer(frames * channels.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        final ShortBuffer out = result.asShortBuffer();

        for (int frame = 0; frame < frames; frame++)
            for (final float[] channel : channels)
                out.put((short) Math.round(Math.max(-1, Math.min(1, channel[frame])) * 32767));
        return result;
    }

    //#endregion
}
