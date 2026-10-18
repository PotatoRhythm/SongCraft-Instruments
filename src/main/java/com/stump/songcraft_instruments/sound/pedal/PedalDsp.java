package com.stump.songcraft_instruments.sound.pedal;

import com.stump.songcraft_instruments.pedal.DelayParams;
import com.stump.songcraft_instruments.pedal.EqParams;
import com.stump.songcraft_instruments.pedal.LofiParams;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.pedal.PedalEffect;
import com.stump.songcraft_instruments.pedal.PedalEffectType;
import com.stump.songcraft_instruments.pedal.ReverbParams;
import com.stump.songcraft_instruments.sound.ChannelTwinSounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;

import javax.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.List;

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
    /**
     * How much a held note's holds must overlap, as heard, for their reverb to be baked in (see {@link #renderHold}).
     * Holds that leave gaps can't be crossfaded, so play without it.
     */
    private static final double MIN_HOLD_OVERLAP_SECONDS = 0.02;
    /**
     * How long, as heard, one hold's baked reverb crossfades into the next one's, at most (and at most their overlap).
     * Holds start on game ticks, so usually a few milliseconds off from a period apart, which puts the note's tone
     * in the two reverbs at a random phase to each other: they partly cancel or add up while they crossfade.
     * Short keeps that brief, while long enough not to be heard as a sudden change.
     */
    private static final double HOLD_CROSSFADE_SECONDS = 0.15;

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

        // A held note's own sound fades out as usual, so its echo tail leaves out the dry sound
        channels = applyEffects(channels, chain, playRate, echoTail);

        channels = trimTail(channels, playRate, dryFrames);
        scale(channels, loudnessScale(channels, dryRms, dryPeak, dryFrames));
        for (final float[] channel : channels)
            softLimit(channel);
        return toPcm(channels);
    }

    /**
     * Renders a held note's hold sound with its reverb baked in.
     * <p>Holds repeat while a note is held, each starting {@code periodTicks} after the last and overlapping its end,
     * so a reverb ringing on past each one would pile up as they repeat. Instead each hold carries the reverb that's
     * ringing while it plays:</p>
     * <ol>
     *     <li>The hold is rendered through the chain up to its last reverb, with the reverb's whole tail.</li>
     *     <li>What the reverbs added (the result without the hold itself) is cut into pieces one period long,
     *     which are added up: the tails of the holds before it that are still ringing, plus its own reverb.
     *     That repeats every period, so it loops seamlessly from one hold to the next.</li>
     *     <li>It's added to the hold, crossfading into the next hold's at the start of where the two overlap
     *     (see {@link #HOLD_CROSSFADE_SECONDS}), and the rest of the chain after the reverb is applied to them
     *     together.</li>
     * </ol>
     * <p>The first hold carries only its own reverb, so the reverb builds up as the note starts. Later holds carry
     * the tails of holds before the first too, so the second is slightly more reverberant than it should be.</p>
     * <p>Holds that don't overlap the next one (their sound being shorter than the time between them) can't be
     * crossfaded, so are rendered without their reverb, as held notes used to be.</p>
     * @param chain The chain the note is played through while held, without its delays
     * @param periodTicks How many ticks apart the note's holds start
     * @param first Whether this is the note's first hold, which no hold before it rings into
     * @return The hold's samples, the same length and format as the given sound's
     */
    static ByteBuffer renderHold(final ChannelTwinSounds.Decoded sound, final PedalChain chain, final float pitch,
                                 final int periodTicks, final boolean first) {
        final float playRate = sound.format().getSampleRate() * pitch;
        final float[][] original = toFloat(sound.samples(), sound.format().getChannels());

        final HeldReverb held = HeldReverb.of(original, chain, playRate, periodTicks);
        if (held == null) {
            final PedalChain withoutReverb = withoutTimeBased(chain);
            // Nothing left to render: played as it is, as it would be without pedals
            return withoutReverb.isEmpty() ? sound.samples() : render(sound, withoutReverb, pitch, false);
        }

        final float[][] result;
        if (first) {
            // Only its own reverb, fading out as the next hold's fades in
            final float[][] firstHold = new float[original.length][held.frames()];
            for (int i = 0; i < held.frames(); i++) {
                final float fadeOut = held.fadeOut(i);
                for (int c = 0; c < original.length; c++)
                    firstHold[c][i] = held.dry()[c][i] + fadeOut * held.reverb()[c][i];
            }
            result = applyEffects(firstHold, held.afterReverb(), playRate, false);
        } else {
            result = held.later();
        }

        // Every hold is turned down alike, by how much the later ones need, so the reverb doesn't jump as it builds up
        scale(result, held.scale());
        for (final float[] channel : result)
            softLimit(channel);
        return toPcm(result);
    }

    /**
     * Renders what a held note's reverb rings on with once it's let go, played as the note fades out (see
     * {@code HeldNoteSoundInstance#setReleased}). Its holds' baked reverb (see {@link #renderHold}) fades out with them,
     * so this takes over from it: the reverb the note had built up dying away, plus that of the note as it fades out.
     * <p>Rendered by playing the note out: its holds repeating as they do while it's held, for as long as the reverb
     * rings, then fading out over {@code fadeTicks} as it does when released, all through the reverb. What the reverb
     * rings with from the release on is kept, at the same level as is baked into the holds. The note is let go
     * halfway through a hold, standing in for wherever it was.</p>
     * <p>Holds that can't have their reverb baked in (see {@link #renderHold}) ring on from a short slice of
     * their sound instead, as held notes used to.</p>
     * @param chain The chain the note is played through while held, without its delays
     * @param periodTicks How many ticks apart the note's holds start
     * @param fadeTicks How many ticks the note takes to fade out once released
     * @return The reverb, starting from the release
     */
    static ByteBuffer renderReleaseTail(final ChannelTwinSounds.Decoded sound, final PedalChain chain,
                                        final float pitch, final int periodTicks, final int fadeTicks) {
        final float playRate = sound.format().getSampleRate() * pitch;
        final float[][] original = toFloat(sound.samples(), sound.format().getChannels());

        final HeldReverb held = HeldReverb.of(original, chain, playRate, periodTicks);
        if (held == null)
            return render(sound, chain, pitch, true);

        // Each hold goes through the chain up to its first reverb on its own, as it does when played,
        // and the reverbs (and what's between them) take all of them together
        final List<PedalEffect> toReverb = held.toReverb().effects();
        int firstReverb = 0;
        while (toReverb.get(firstReverb).type() != PedalEffectType.REVERB)
            firstReverb++;
        final float[][] hold = applyEffects(copy(original), new PedalChain(toReverb.subList(0, firstReverb)), playRate, false);
        final PedalChain reverbs = new PedalChain(toReverb.subList(firstReverb, toReverb.size()));

        final int channelCount = original.length, frames = held.frames(), period = held.periodFrames();
        final int fadeFrames = Math.max(1, (int) Math.round(fadeTicks / 20.0 * playRate));
        final int tailFrames = (int) Math.round(Math.min(chain.tailSeconds(), DelayParams.MAX_TAIL_SECONDS) * playRate);

        // Holds start every period from the start, enough of them before the release for the reverb to have settled,
        // and go on chaining while the note fades out
        final int settledHolds = (tailFrames + frames) / period + 2;
        final int release = settledHolds * period + period / 2;
        final int fadeEnd = release + fadeFrames;
        final int holds = (fadeEnd - 1) / period + 1;
        final float[][] played = new float[channelCount][(holds - 1) * period + frames];
        for (int k = 0; k < holds; k++)
            for (int c = 0; c < channelCount; c++)
                for (int i = 0; i < frames; i++)
                    played[c][k * period + i] += hold[c][i];
        for (final float[] samples : played)
            for (int i = release; i < samples.length; i++)
                samples[i] *= (i < fadeEnd) ? 1 - (float) (i - release) / fadeFrames : 0;

        // Only what the reverbs add, as loud as in the holds (whose reverbs' gains are by their place in the chain)
        final double[] reverbGains = Arrays.copyOfRange(held.reverbGains(), firstReverb, toReverb.size());
        final float[][] rung = applyEffects(played, reverbs, playRate, true, reverbGains);
        float[][] tail = new float[channelCount][];
        for (int c = 0; c < channelCount; c++)
            tail[c] = Arrays.copyOfRange(rung[c], release, rung[c].length);

        tail = applyEffects(tail, held.afterReverb(), playRate, false);
        tail = trimTail(tail, playRate, Math.min(fadeFrames, tail[0].length - 1));
        scale(tail, held.scale());
        for (final float[] channel : tail)
            softLimit(channel);
        return toPcm(tail);
    }

    /**
     * What a held note's holds and their release tail are rendered from: a hold through its chain, with the reverb
     * that's ringing while it plays (see {@link #renderHold})
     * @param frames How long the hold is
     * @param periodFrames How far apart holds start, in the hold's samples
     * @param crossfadeFrames How long each hold's baked reverb crossfades into the next one's
     * @param toReverb The chain up to its last reverb, which plays out over time
     * @param afterReverb The rest of the chain, applied to each hold as a whole
     * @param dry The hold through {@code toReverb}, without what its reverbs add
     * @param reverb What its reverbs add to it, with their whole tail
     * @param later A later hold, with the reverb of the holds before it baked in, through the whole chain
     * @param scale How much every hold (and the tail) is turned down, so the later ones aren't louder than the hold
     * @param reverbGains How loud each of the chain's reverbs is made, for the hold (see {@link #reverb})
     */
    private record HeldReverb(int frames, int periodFrames, int crossfadeFrames,
                              PedalChain toReverb, PedalChain afterReverb,
                              float[][] dry, float[][] reverb, float[][] later, double scale, double[] reverbGains) {
        /**
         * @return Or null if the chain has no reverb, or the holds don't overlap enough to be crossfaded
         */
        @Nullable
        static HeldReverb of(final float[][] original, final PedalChain chain, final float playRate,
                             final int periodTicks) {
            final int channelCount = original.length, frames = original[0].length;
            // Ticks pass as heard, while the sound plays through pitch times as many of its samples
            final int periodFrames = (int) Math.round(periodTicks / 20.0 * playRate);
            final int overlapFrames = frames - periodFrames;

            final List<PedalEffect> effects = chain.effects();
            int lastReverb = -1;
            for (int i = 0; i < effects.size(); i++)
                if (effects.get(i).type() == PedalEffectType.REVERB)
                    lastReverb = i;
            if (lastReverb < 0 || periodFrames <= 0 || overlapFrames < MIN_HOLD_OVERLAP_SECONDS * playRate)
                return null;

            // Up to its last reverb, the chain plays out over time; after it, it's applied to each hold as a whole
            final PedalChain toReverb = new PedalChain(effects.subList(0, lastReverb + 1));
            final PedalChain afterReverb = withoutTimeBased(new PedalChain(effects.subList(lastReverb + 1, effects.size())));

            // The hold as the chain leaves it up to the reverb, with and without what its reverbs add. Both go through
            // its other effects alike, so the reverbs' part is exactly the difference between them.
            final double[] reverbGains = new double[effects.size()];
            Arrays.fill(reverbGains, Double.NaN);
            final float[][] dry = applyEffects(copy(original), withoutTimeBased(toReverb), playRate, false);
            final float[][] reverb = applyEffects(copy(original), toReverb, playRate, false, reverbGains);
            for (int c = 0; c < channelCount; c++)
                for (int i = 0; i < frames; i++)
                    reverb[c][i] -= dry[c][i];

            // Every hold before this one started a whole number of periods earlier, so what's left of their reverbs
            // lines up with this one's period by period
            final float[][] loop = new float[channelCount][periodFrames];
            for (int c = 0; c < channelCount; c++)
                for (int i = 0; i < reverb[c].length; i++)
                    loop[c][i % periodFrames] += reverb[c][i];

            // Each hold's reverb fades in over the start of its overlap with the last hold, as that one's fades out
            final int crossfadeFrames = Math.min(Math.min(overlapFrames, periodFrames),
                (int) Math.round(HOLD_CROSSFADE_SECONDS * playRate));
            final HeldReverb held = new HeldReverb(frames, periodFrames, crossfadeFrames, toReverb, afterReverb,
                dry, reverb, new float[channelCount][frames], 1, reverbGains);
            for (int i = 0; i < frames; i++) {
                final float fade = held.fadeIn(i) * held.fadeOut(i);
                for (int c = 0; c < channelCount; c++)
                    held.later[c][i] = dry[c][i] + fade * loop[c][i % periodFrames];
            }

            final float[][] later = applyEffects(held.later, afterReverb, playRate, false);
            final double scale = loudnessScale(later, rms(original, frames), peak(original), frames);
            return new HeldReverb(frames, periodFrames, crossfadeFrames, toReverb, afterReverb,
                dry, reverb, later, scale, reverbGains);
        }

        /**
         * @return How much of a hold's baked reverb is heard at the given sample, as it fades in from the last hold's
         */
        float fadeIn(final int i) {
            return (i < crossfadeFrames) ? holdCrossfade((double) i / crossfadeFrames) : 1;
        }
        /**
         * @return How much of a hold's baked reverb is heard at the given sample, as it fades out into the next hold's
         */
        float fadeOut(final int i) {
            if (i < periodFrames)
                return 1;
            return (i < periodFrames + crossfadeFrames)
                ? holdCrossfade(1 - (double) (i - periodFrames) / crossfadeFrames) : 0;
        }
    }

    /**
     * An equal-power crossfade, which keeps the level steady when what's crossfaded is at a random phase to each
     * other, as holds' reverbs usually are (see {@link #HOLD_CROSSFADE_SECONDS})
     * @param position From 0 (the start of the crossfade) to 1 (its end)
     * @return How much of a hold's baked reverb is heard as it fades in over the start of its overlap with the last
     * hold. Fades out mirrored: {@code holdCrossfade(1 - position)}.
     */
    private static float holdCrossfade(final double position) {
        return (float) Math.sin(Math.PI / 2 * position);
    }

    /**
     * Runs the samples through the chain's rendered effects, in signal order
     * @param dropDry Whether the first effect that plays out over time leaves the sound itself out, and keeps only
     *                its echoes. Only once: a second delay (or a reverb) repeats the first one's echoes along with its own.
     * @return The channels as the effects leave them, longer than the ones given if they played out over time
     */
    private static float[][] applyEffects(final float[][] channels, final PedalChain chain, final float playRate,
                                          final boolean dropDry) {
        return applyEffects(channels, chain, playRate, dropDry, null);
    }
    /**
     * @param reverbGains How loud each reverb's reverb is made, by its place in the chain (see {@link #reverb}).
     *                    One that's NaN is worked out from the sound reaching it, and stored.
     */
    private static float[][] applyEffects(float[][] channels, final PedalChain chain, final float playRate,
                                          boolean dropDry, @Nullable final double[] reverbGains) {
        final List<PedalEffect> effects = chain.effects();
        for (int i = 0; i < effects.size(); i++) {
            final PedalEffect effect = effects.get(i);
            switch (effect.type()) {
                case FUZZ -> fuzz(channels, playRate, effect);
                case BITCRUSH -> bitcrush(channels, playRate, effect);
                case EQ -> eq(channels, playRate, effect);
                // Its tape tone; its wobble is applied while notes play, see PedalLiveEffects
                case LOFI -> lofi(channels, playRate, effect);
                case DELAY -> {
                    channels = delay(channels, playRate, effect, dropDry);
                    dropDry = false;
                }
                case REVERB -> {
                    channels = reverb(channels, playRate, effect, dropDry, reverbGains, i);
                    dropDry = false;
                }
                // Applied while notes play instead, see PedalLiveEffects
                case TREMOLO, VIBRATO -> {}
            }
        }
        return channels;
    }

    private static PedalChain withoutTimeBased(final PedalChain chain) {
        return new PedalChain(chain.effects().stream().filter((effect) -> !effect.type().timeBased).toList());
    }

    /**
     * Keeps pedals from ever making a sound louder than it is without them, in loudness or in peaks.
     * <p>The player's instrument volume is applied on top of this (and capped by Minecraft at full), so a note
     * played through pedals is never louder than the same note played at that volume without them.</p>
     * @param dryFrames How long the sound was before effects; loudness is compared over that much of it,
     *                  so an echo tail doesn't count as making it quieter
     * @return What to multiply the rendered sound by, at most 1
     */
    private static double loudnessScale(final float[][] channels, final double dryRms, final float dryPeak,
                                        final int dryFrames) {
        final double wetRms = rms(channels, dryFrames);
        final float wetPeak = peak(channels);

        double scale = 1;
        if (wetRms > dryRms)
            scale = dryRms / wetRms;
        if (wetPeak * scale > dryPeak)
            scale = dryPeak / wetPeak;
        return Math.min(scale, 1);
    }
    private static void scale(final float[][] channels, final double scale) {
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
     * @param gains How loud the reverb is made, by the reverb's index: so a sound rendered from another
     *              (like a held note's release tail, from its holds) rings at the same level as it.
     *              If null, or NaN at the index, worked out from this sound as usual, and stored there.
     * @return The channels with the reverb, longer than the ones given
     */
    private static float[][] reverb(final float[][] channels, final float playRate,
                                    final PedalEffect effect, final boolean dropDry,
                                    @Nullable final double[] gains, final int index) {
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
        final float wetGain;
        if (gains != null && !Double.isNaN(gains[index])) {
            wetGain = (float) gains[index];
        } else {
            wetGain = (wetEnergy > 1e-12) ? (float) (params.mix() * Math.sqrt(dryEnergy / wetEnergy)) : 0;
            if (gains != null)
                gains[index] = wetGain;
        }
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
    private static float[][] copy(final float[][] channels) {
        final float[][] copy = new float[channels.length][];
        for (int c = 0; c < channels.length; c++)
            copy[c] = channels[c].clone();
        return copy;
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
