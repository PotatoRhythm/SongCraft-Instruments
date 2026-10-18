package com.stump.songcraft_instruments.sound.pedal;

import com.stump.songcraft_instruments.pedal.DelayParams;
import com.stump.songcraft_instruments.pedal.EqParams;
import com.stump.songcraft_instruments.pedal.LofiParams;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.pedal.PedalEffect;
import com.stump.songcraft_instruments.pedal.ReverbParams;
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
     * The Octave's filters: the low-pass its cycles are detected through, and the ones rounding off its octave
     * below and above
     */
    private static final double OCTAVE_DETECT_HZ = 1000, OCTAVE_SUB_TONE_HZ = 1200, OCTAVE_UP_TONE_HZ = 5000;
    /**
     * How far past 0 the note must go for a crossing to count, out of its recent peak level,
     * so small wiggles near 0 don't flip the octave below
     */
    private static final float OCTAVE_THRESHOLD = 0.1f, OCTAVE_MIN_THRESHOLD = 1e-4f;
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
     * @param pitch The pitch the sound will be played at (what OpenAL resamples it by). Every effect is worked out
     *              for it, so its filters, timing and sample rates come out as set once the sound is resampled,
     *              whatever the note; see {@link PedalSounds}
     * @param echoTail Whether to render only the echoes of a short slice of the sound, for a held note's release
     * @return The given sound's samples, rendered through the chain, in the same format.
     * Effects that play out over time may make it longer.
     */
    static ByteBuffer render(final ChannelTwinSounds.Decoded sound, final PedalChain chain,
                             final float pitch, final boolean echoTail) {
        final int channelCount = sound.format().getChannels();
        // Samples are played back pitch times as fast, so everything in them is pitch times as long (and every
        // frequency 1/pitch as high) here as it will be heard: effects work at the rate the samples play at
        final float playRate = sound.format().getSampleRate() * pitch;

        float[][] channels = toFloat(sound.samples(), channelCount);
        if (echoTail)
            channels = echoSlice(channels, playRate);

        final int dryFrames = channels[0].length;
        final double dryRms = rms(channels, dryFrames);
        final float dryPeak = peak(channels);

        // A held note's own sound fades out as usual, so its echo tail leaves out the dry sound, but only once:
        // a second delay (or a reverb) repeats the first one's echoes along with its own
        boolean dropDry = echoTail;
        for (final PedalEffect effect : chain.effects()) {
            switch (effect.type()) {
                case FUZZ -> fuzz(channels, playRate, effect);
                case BITCRUSH -> bitcrush(channels, playRate, effect);
                case OCTAVE -> octave(channels, playRate, effect);
                case EQ -> eq(channels, playRate, effect);
                // Its tape tone; its wobble is applied while notes play, see PedalLiveEffects
                case LOFI -> lofi(channels, playRate, effect);
                case DELAY -> {
                    channels = delay(channels, playRate, effect, dropDry);
                    dropDry = false;
                }
                case REVERB -> {
                    channels = reverb(channels, playRate, effect, dropDry);
                    dropDry = false;
                }
                // Applied while notes play instead, see PedalLiveEffects
                case TREMOLO, VIBRATO -> {}
            }
        }

        channels = trimTail(channels, playRate, dryFrames);
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
    private static void fuzz(final float[][] channels, final float playRate, final PedalEffect effect) {
        final float gain = dbToGain(6 + effect.knobFraction(0) * 40);
        final double toneHz = 700 * Math.pow(12000 / 700.0, effect.knobFraction(1));
        final float level = dbToGain(-24 + effect.knobFraction(2) * 24);

        final float bias = 0.12f;
        final float biasOffset = (float) Math.tanh(bias);

        // The input is coupled through a small capacitor, so lows don't turn into flub
        final float inputHp = onePoleCoefficient(100, playRate);
        final float dcBlock = 1 - (float) (2 * Math.PI * 20 / playRate);
        final float toneLp = onePoleCoefficient(toneHz, playRate);
        // A gate keeps the fuzz from turning a note's quiet tail (and its noise) into a hiss
        final float gateRelease = (float) Math.exp(-1 / (0.05 * playRate));
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
     * then from {@link #DOWNSAMPLE_MAX_HZ} down to {@link #DOWNSAMPLE_MIN_HZ}, as heard), and mix
     * (how much of the crushed sound is heard over the original).</p>
     */
    private static void bitcrush(final float[][] channels, final float playRate, final PedalEffect effect) {
        // Either knob fully down turns its crushing off, rather than crushing a little
        final boolean crushBits = effect.knob(0) > 0, downsample = effect.knob(1) > 0;

        final double bits = CRUSH_MAX_BITS - (CRUSH_MAX_BITS - CRUSH_MIN_BITS) * effect.knobFraction(0);
        // Levels on each side of 0
        final float levels = (float) Math.pow(2, bits - 1);
        final double crushedRate = DOWNSAMPLE_MAX_HZ * Math.pow(DOWNSAMPLE_MIN_HZ / DOWNSAMPLE_MAX_HZ, effect.knobFraction(1));
        final double holdFrames = downsample ? Math.max(1, playRate / crushedRate) : 1;
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
     * An octaver, built like the classic analog ones, so it works sample by sample and never changes a sound's length:
     * <ul>
     *     <li>The octave below comes from a flip-flop: the note's polarity flips on every other cycle (detected by
     *     its zero crossings), which halves its pitch. Smoothed by a low-pass, for a round sub tone.</li>
     *     <li>The octave above comes from folding the note's negative half up (its absolute value),
     *     which doubles its pitch.</li>
     * </ul>
     * <p>Cycles are detected on all channels mixed together, so a Stereo sound's octave below stays in phase
     * between them. Like a real octaver, it tracks single notes best: chords and samples with several notes
     * confuse it into a glitchy, ring-modulated sound.</p>
     * <p>Knobs: sub (the octave below's level), up (the octave above's level), dry (the note's own level).</p>
     */
    private static void octave(final float[][] channels, final float playRate, final PedalEffect effect) {
        final float sub = effect.knobFraction(0), up = effect.knobFraction(1), dry = effect.knobFraction(2);

        // Cycles are detected on the note with its upper harmonics smoothed off, so they don't add crossings
        final float detectLp = onePoleCoefficient(OCTAVE_DETECT_HZ, playRate);
        final float subLp = onePoleCoefficient(OCTAVE_SUB_TONE_HZ, playRate);
        final float upLp = onePoleCoefficient(OCTAVE_UP_TONE_HZ, playRate);
        final float dcBlock = 1 - (float) (2 * Math.PI * 20 / playRate);
        final float envelopeRelease = (float) Math.exp(-1 / (0.02 * playRate));

        final int channelCount = channels.length, frames = channels[0].length;
        final float[] subState = new float[channelCount], upState = new float[channelCount];
        final float[] dcIn = new float[channelCount], dcOut = new float[channelCount];

        float detect = 0, envelope = 0;
        // A cycle counts once the note has gone clearly below 0, then clearly back above it
        boolean wentBelow = false;
        float flip = 1;

        for (int i = 0; i < frames; i++) {
            float mono = 0;
            for (final float[] samples : channels)
                mono += samples[i];
            mono /= channelCount;

            detect += detectLp * (mono - detect);
            final float level = Math.abs(detect);
            envelope = (level > envelope) ? level : envelope * envelopeRelease;
            final float threshold = Math.max(envelope * OCTAVE_THRESHOLD, OCTAVE_MIN_THRESHOLD);
            if (detect < -threshold) {
                wentBelow = true;
            } else if (wentBelow && detect > threshold) {
                wentBelow = false;
                flip = -flip;
            }

            for (int c = 0; c < channelCount; c++) {
                final float x = channels[c][i];

                subState[c] += subLp * (x * flip - subState[c]);

                // Folding leaves the note's average level behind as a DC offset, and twice the signal makes up
                // for the octave above being quieter than the note it comes from
                final float folded = Math.abs(x);
                final float dc = folded - dcIn[c] + dcBlock * dcOut[c];
                dcIn[c] = folded;
                dcOut[c] = dc;
                upState[c] += upLp * (2 * dc - upState[c]);

                channels[c][i] = dry * x + sub * subState[c] + up * upState[c];
            }
        }
    }

    /**
     * A three-band EQ, like an amp's tone stack: a low shelf for the bass, a wide peak for the mids,
     * and a high shelf for the treble, each boosted or cut (see {@link EqParams}). Bands left flat aren't run at all.
     * <p>Boosts are still bound by {@link #capLoudness}: boosting a band turns the whole sound down to make room,
     * so the band comes out louder than the rest without the note getting louder.</p>
     * <p>Knobs: bass, mid, treble.</p>
     */
    private static void eq(final float[][] channels, final float playRate, final PedalEffect effect) {
        final EqParams params = EqParams.of(effect);
        final double[][] bands = {
            shelf(EqParams.BASS_HZ, params.bassDb(), playRate, false),
            peak(EqParams.MID_HZ, EqParams.MID_Q, params.midDb(), playRate),
            shelf(EqParams.TREBLE_HZ, params.trebleDb(), playRate, true)
        };

        for (final double[] band : bands) {
            if (band == null)
                continue;
            for (final float[] samples : channels)
                biquad(samples, band);
        }
    }

    /**
     * A worn cassette tape's tone (see {@link LofiParams}):
     * <ul>
     *     <li>Its narrow bandwidth: lows and highs rolled off, more as it ages.</li>
     *     <li>Its warmth: soft saturation that rounds off the peaks, slightly lopsided for the even harmonics of tape,
     *     then a little more body in the low mids and softer highs. Left out entirely at no warmth.</li>
     * </ul>
     * <p>Its wobble is applied while notes play instead, and its hiss rolls on its own, so neither is rendered here.</p>
     * <p>Knobs: age, wobble (not used here), warmth.</p>
     */
    private static void lofi(final float[][] channels, final float playRate, final PedalEffect effect) {
        final LofiParams params = LofiParams.of(effect);
        // Two poles each way, for a tape's gentle but definite roll-off
        final float highpass = onePoleCoefficient(params.highpassHz(), playRate);
        final float lowpass = onePoleCoefficient(params.lowpassHz(), playRate);

        final boolean saturate = params.warmth() > 0;
        final double drive = params.saturationDrive(), bias = params.saturationBias();
        final double biasOffset = Math.tanh(bias);
        // The clipper's slope through 0, so quiet sounds pass at their own level, however hard it's driven
        final double unity = drive * (1 - biasOffset * biasOffset);
        final float dcBlock = 1 - (float) (2 * Math.PI * 20 / playRate);

        for (final float[] samples : channels) {
            float hp1 = 0, hp2 = 0, lp1 = 0, lp2 = 0;
            float dcIn = 0, dcOut = 0;
            for (int i = 0; i < samples.length; i++) {
                float x = samples[i];

                hp1 += highpass * (x - hp1);
                x -= hp1;
                hp2 += highpass * (x - hp2);
                x -= hp2;

                lp1 += lowpass * (x - lp1);
                lp2 += lowpass * (lp1 - lp2);
                x = lp2;

                if (saturate) {
                    final float y = (float) ((Math.tanh(drive * x + bias) - biasOffset) / unity);
                    // The lopsided clipping leaves a DC offset
                    final float dc = y - dcIn + dcBlock * dcOut;
                    dcIn = y;
                    dcOut = dc;
                    x = dc;
                }
                samples[i] = x;
            }
        }

        final double[] body = peak(LofiParams.WARMTH_BODY_HZ, LofiParams.WARMTH_BODY_Q, params.bodyDb(), playRate);
        final double[] soften = shelf(LofiParams.WARMTH_SOFTEN_HZ, params.softenDb(), playRate, true);
        for (final float[] samples : channels) {
            if (body != null)
                biquad(samples, body);
            if (soften != null)
                biquad(samples, soften);
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
    private static float[][] delay(final float[][] channels, final float playRate,
                                   final PedalEffect effect, final boolean dropDry) {
        final DelayParams params = DelayParams.of(effect);
        final int delayFrames = Math.max(1, (int) Math.round(params.seconds() * playRate));
        final int tailFrames = (int) Math.round(params.tailSeconds() * playRate);
        final float feedback = (float) params.feedback(), mix = (float) params.mix();
        final float damping = onePoleCoefficient(DELAY_DAMPING_HZ, playRate);

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
     * The comb filters' delays in Freeverb, in samples at 44.1 kHz: spread out and coprime, so their echoes
     * don't line up into a pitch
     */
    private static final int[] REVERB_COMBS = {1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
    /**
     * The allpass filters' delays in Freeverb, in samples at 44.1 kHz, which diffuse the combs' echoes into a wash
     */
    private static final int[] REVERB_ALLPASSES = {556, 441, 341, 225};
    /**
     * How many samples (at 44.1 kHz) longer each delay is on the right channel, so the two channels' reverbs differ
     * and stereo sounds come out wide
     */
    private static final int REVERB_STEREO_SPREAD = 23;
    private static final double REVERB_REFERENCE_RATE = 44100;
    private static final float REVERB_ALLPASS_FEEDBACK = 0.5f;

    /**
     * A reverb: the note rings on in a room, dying away over the set decay, each pass through the room darker
     * than the last. Built like Freeverb: eight comb filters in parallel (each a feedback loop with a low-pass in it),
     * then four allpass filters in series that smear their echoes into a dense wash.
     * <p>Rendered for the pitch the note plays at, like the {@link #delay}, so its timing and tone come out as set.
     * The reverb carries {@code mix} times as much energy as the note itself, however long it rings.
     * It rings on past the end of the note, for up to {@link DelayParams#MAX_TAIL_SECONDS}.</p>
     * <p>Knobs: decay (from {@link ReverbParams#MIN_DECAY_SECONDS} to {@link ReverbParams#MAX_DECAY_SECONDS}),
     * tone (how bright it is), mix (how loud it is).</p>
     * @param dropDry Whether to leave the note itself out, and keep only its reverb
     * @return The channels with the reverb, longer than the ones given
     */
    private static float[][] reverb(final float[][] channels, final float playRate,
                                    final PedalEffect effect, final boolean dropDry) {
        final ReverbParams params = ReverbParams.of(effect);
        final int frames = channels[0].length;
        final int tailFrames = (int) Math.round(params.tailSeconds() * playRate);
        final float[][] result = new float[channels.length][frames + tailFrames];

        final double scale = playRate / REVERB_REFERENCE_RATE;
        final float damp = 1 - onePoleCoefficient(params.toneHz(), playRate);

        double dryEnergy = 0, wetEnergy = 0;
        final float[][] wet = new float[channels.length][frames + tailFrames];
        for (int c = 0; c < channels.length; c++) {
            final int spread = (c == 1) ? REVERB_STEREO_SPREAD : 0;
            final float[] in = channels[c], out = wet[c];

            final float[][] combs = new float[REVERB_COMBS.length][];
            final float[] combFeedback = new float[REVERB_COMBS.length], combStore = new float[REVERB_COMBS.length];
            final int[] combIndex = new int[REVERB_COMBS.length];
            for (int k = 0; k < combs.length; k++) {
                combs[k] = new float[Math.max(1, (int) Math.round((REVERB_COMBS[k] + spread) * scale))];
                // Each pass through this comb takes its length, so it must lose 60 dB over the decay's worth of them
                combFeedback[k] = (float) Math.pow(10, -3 * combs[k].length / (params.decaySeconds() * playRate));
            }
            final float[][] allpasses = new float[REVERB_ALLPASSES.length][];
            final int[] allpassIndex = new int[REVERB_ALLPASSES.length];
            for (int k = 0; k < allpasses.length; k++)
                allpasses[k] = new float[Math.max(1, (int) Math.round((REVERB_ALLPASSES[k] + spread) * scale))];

            for (int i = 0; i < out.length; i++) {
                final float x = (i < frames) ? in[i] : 0;

                float sum = 0;
                for (int k = 0; k < combs.length; k++) {
                    final float[] buffer = combs[k];
                    final float delayed = buffer[combIndex[k]];
                    combStore[k] = delayed * (1 - damp) + combStore[k] * damp;
                    buffer[combIndex[k]] = x + combStore[k] * combFeedback[k];
                    if (++combIndex[k] >= buffer.length)
                        combIndex[k] = 0;
                    sum += delayed;
                }

                for (int k = 0; k < allpasses.length; k++) {
                    final float[] buffer = allpasses[k];
                    final float delayed = buffer[allpassIndex[k]];
                    buffer[allpassIndex[k]] = sum + delayed * REVERB_ALLPASS_FEEDBACK;
                    sum = delayed - sum;
                    if (++allpassIndex[k] >= buffer.length)
                        allpassIndex[k] = 0;
                }

                out[i] = sum;
                wetEnergy += (double) sum * sum;
                dryEnergy += (double) x * x;
            }
        }

        // The reverb's loudness follows the mix knob, not how many echoes its loops happen to build up
        final float wetGain = (wetEnergy > 1e-12) ? (float) (params.mix() * Math.sqrt(dryEnergy / wetEnergy)) : 0;
        for (int c = 0; c < channels.length; c++) {
            final float[] in = channels[c], out = result[c];
            for (int i = 0; i < out.length; i++)
                out[i] = ((dropDry || i >= frames) ? 0 : in[i]) + wetGain * wet[c][i];
        }
        return result;
    }

    /**
     * @return A short slice from inside the sound, faded in and out, for a held note's echo tail.
     * Held sounds loop, so any part of one stands for how it sounds when released.
     */
    private static float[][] echoSlice(final float[][] channels, final float playRate) {
        final int frames = channels[0].length;
        final int sliceFrames = Math.min(frames, (int) Math.round(ECHO_SLICE_SECONDS * playRate));
        final int start = Math.min(frames / 4, frames - sliceFrames);
        final int fadeIn = Math.min(sliceFrames / 2, (int) Math.round(EDGE_FADE_SECONDS * playRate));

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
    private static float[][] trimTail(final float[][] channels, final float playRate, final int dryFrames) {
        final int frames = channels[0].length;
        if (frames <= dryFrames)
            return channels;

        final int maxFrames = dryFrames + (int) Math.round(DelayParams.MAX_TAIL_SECONDS * playRate);
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

        final int fade = Math.min(end - dryFrames, (int) Math.round(EDGE_FADE_SECONDS * playRate));
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
    private static float onePoleCoefficient(final double hz, final float playRate) {
        return (float) (1 - Math.exp(-2 * Math.PI * Math.min(hz, playRate * 0.45) / playRate));
    }
    /**
     * The coefficients of a shelving filter, from the Audio EQ Cookbook (with a slope of 1)
     * @param high Whether it shelves the frequencies above {@code hz} (treble), rather than below (bass)
     * @return {b0, b1, b2, a1, a2}, normalized by a0; or null if it's flat
     */
    private static double[] shelf(final double hz, final double db, final float playRate, final boolean high) {
        if (db == 0)
            return null;

        final double a = Math.pow(10, db / 40);
        final double w0 = 2 * Math.PI * Math.min(hz, playRate * 0.45) / playRate;
        final double cos = Math.cos(w0), alpha = Math.sin(w0) / 2 * Math.sqrt(2);
        final double twoSqrtAAlpha = 2 * Math.sqrt(a) * alpha;
        // The high shelf is the low shelf with the cosine's sign flipped
        final double c = high ? -cos : cos;

        final double b0 = a * ((a + 1) - (a - 1) * c + twoSqrtAAlpha);
        final double b1 = 2 * a * ((a - 1) - (a + 1) * c);
        final double b2 = a * ((a + 1) - (a - 1) * c - twoSqrtAAlpha);
        final double a0 = (a + 1) + (a - 1) * c + twoSqrtAAlpha;
        final double a1 = -2 * ((a - 1) + (a + 1) * c);
        final double a2 = (a + 1) + (a - 1) * c - twoSqrtAAlpha;
        return high
            ? new double[] {b0 / a0, -b1 / a0, b2 / a0, -a1 / a0, a2 / a0}
            : new double[] {b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0};
    }
    /**
     * The coefficients of a peaking filter, from the Audio EQ Cookbook
     * @return {b0, b1, b2, a1, a2}, normalized by a0; or null if it's flat
     */
    private static double[] peak(final double hz, final double q, final double db, final float playRate) {
        if (db == 0)
            return null;

        final double a = Math.pow(10, db / 40);
        final double w0 = 2 * Math.PI * Math.min(hz, playRate * 0.45) / playRate;
        final double cos = Math.cos(w0), alpha = Math.sin(w0) / (2 * q);

        final double a0 = 1 + alpha / a;
        return new double[] {(1 + alpha * a) / a0, -2 * cos / a0, (1 - alpha * a) / a0, -2 * cos / a0, (1 - alpha / a) / a0};
    }
    /**
     * Runs a biquad filter over the samples
     * @param c {b0, b1, b2, a1, a2}, normalized by a0
     */
    private static void biquad(final float[] samples, final double[] c) {
        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int i = 0; i < samples.length; i++) {
            final double x = samples[i];
            final double y = c[0] * x + c[1] * x1 + c[2] * x2 - c[3] * y1 - c[4] * y2;
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            samples[i] = (float) y;
        }
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
