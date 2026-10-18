package com.stump.songcraft_instruments.sound.pedal;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.pedal.PedalChain;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plays instrument sounds through effects pedals, by swapping a sound for a rendered copy of it.
 * <p>A rendered copy is named after its pedals and the original file:
 * {@code songcraft_instruments:pedal_fx/<chain key>/<variant>/<original namespace>/<original path>}. Vanilla loads it
 * like any other sound file, which {@link PedalBufferCache} catches and renders instead. Everything else about the
 * sound (its volume, attenuation, and the sound event it was played as) stays as it was.</p>
 * <p>The variant is the exact pitch the sound plays at, {@code p<pitch in thousandths>}: every chain is rendered for it,
 * so its filters and timing come out as set once OpenAL resamples the sound to that pitch, rather than moving with the
 * note. Most notes play at their sound file's own pitch ({@code p1000}) unless transposed, so they share one copy.
 * {@code t<pitch>} is a held note's echo tail, {@code f<pitch>-<ticks>} and {@code h<pitch>-<ticks>} its first
 * and later holds with their echoes and reverb baked in, for holds that start {@code <ticks>} apart, and
 * {@code r<pitch>-<ticks>-<fade ticks>-<held ticks>-<attack lead ticks>} what those ring on with once it's let go
 * (see {@link PedalDsp}).</p>
 */
@OnlyIn(Dist.CLIENT)
public final class PedalSounds {
    private static final String FX_DIR = "pedal_fx/";
    private static final String SOUNDS_PREFIX = "sounds/", OGG_SUFFIX = ".ogg";
    private static final String PERIOD_SEPARATOR = "-";

    /**
     * Which copy of a sound is rendered, named by its variant's first letter
     */
    enum Kind {
        /**
         * The sound through the whole chain
         */
        NOTE('p'),
        /**
         * Only the echoes of a short slice of the sound, for a held note's release (its own sound fades out as usual).
         * Only for chains with effects that play out over time.
         */
        ECHO_TAIL('t'),
        /**
         * A held note's first hold, with the echoes and reverb it starts building up baked in
         */
        FIRST_HOLD('f'),
        /**
         * A held note's later holds, with the echoes and reverb of the holds before them baked in
         */
        HOLD('h'),
        /**
         * What a held note's baked echoes and reverb ring on with once it's let go, as the note fades out
         */
        RELEASE_TAIL('r');

        final char letter;

        Kind(final char letter) {
            this.letter = letter;
        }

        boolean isHold() {
            return this == FIRST_HOLD || this == HOLD;
        }
        /**
         * @return How many numbers follow its pitch in its variant: how far apart holds start, then how long the
         * note takes to fade out, how long it was held, and how long before its first hold its attack starts
         */
        int timings() {
            return isHold() ? 1 : (this == RELEASE_TAIL) ? 4 : 0;
        }

        @Nullable
        static Kind of(final char letter) {
            for (final Kind kind : values())
                if (kind.letter == letter)
                    return kind;
            return null;
        }
    }

    private PedalSounds() {}

    /**
     * @param pitch The pitch the sound will play at (what OpenAL resamples it by)
     * @return The sound, played through the given pedals. The sound itself if there are none,
     * or it can't be (e.g. it is streamed)
     */
    public static Sound withPedals(final Sound sound, final PedalChain chain, final float pitch) {
        return withPedals(sound, chain, pitch, false);
    }
    /**
     * @param echoTail Whether to play only the echoes of a short slice of the sound, for a held note's release
     *                 (its own sound fades out as usual). Only for chains with effects that play out over time.
     */
    public static Sound withPedals(final Sound sound, final PedalChain chain, final float pitch, final boolean echoTail) {
        return withPedals(sound, chain, pitch, echoTail ? Kind.ECHO_TAIL : Kind.NOTE);
    }
    /**
     * A held note's hold sound, played through the given pedals while it's held. Holds repeat, each starting
     * before the last one ends, so echoes and reverb on them are baked into each hold rather than ringing on past
     * it: each carries what the holds before it are still ringing with (see {@link PedalDsp#renderHold}).
     * @param periodTicks How many ticks apart the note's holds start at this pitch
     *                    (see {@code HeldNoteSound#holdPeriodTicks})
     * @param first Whether this is the note's first hold, which no other hold rings into
     * @return The hold sound, played through the pedals. Without effects that play out over time, the same as
     * {@link #withPedals}.
     */
    public static Sound holdWithPedals(final Sound sound, final PedalChain chain, final float pitch,
                                       final int periodTicks, final boolean first) {
        if (!chain.isTimeBased() || periodTicks <= 0)
            return withPedals(sound, chain, pitch);
        return withPedals(sound, chain, pitch, first ? Kind.FIRST_HOLD : Kind.HOLD, periodTicks);
    }

    /**
     * What a held note's hold sound rings on with once it's let go, as it fades out: its echoes and reverb baked
     * into its holds (see {@link #holdWithPedals}) dying away, and those of the note as it fades
     * (see {@link PedalDsp#renderReleaseTail}). Takes over from what's baked into the holds as they fade out.
     * @param periodTicks How many ticks apart the note's holds start at this pitch
     * @param fadeTicks How many ticks the note takes to fade out once released. Longer ones share a copy with
     *                  ones slightly longer or shorter, so a note doesn't render a new one at every volume.
     * @param heldTicks How many ticks after its attack started it was let go. Played out as it was, so its echoes
     *                  come when they would. A note held long enough for what rings to have settled may give the
     *                  shortest time it would sound the same at (as long after a hold started), so notes let go
     *                  at the same point of a hold share a copy.
     * @param attack The note's attack sound, played {@code attackLeadTicks} before its first hold
     *               (see {@link #attackOf})
     * @return The release tail. Without effects that play out over time, the echoes of a short slice of the sound,
     * as {@link #withPedals} gives with {@code echoTail}.
     */
    public static Sound releaseTailWithPedals(final Sound sound, final PedalChain chain, final float pitch,
                                              final int periodTicks, final int fadeTicks, final int heldTicks,
                                              @Nullable final Sound attack, final int attackLeadTicks) {
        if (!chain.isTimeBased() || periodTicks <= 0)
            return withPedals(sound, chain, pitch, true);

        final Sound tail = withPedals(sound, chain, pitch, Kind.RELEASE_TAIL, periodTicks, roundFadeTicks(fadeTicks),
            Math.max(1, heldTicks), Math.max(0, attackLeadTicks));
        if (attack != null && attack.getType() == Sound.Type.FILE && !attack.shouldStream())
            ATTACKS.put(sound.getPath(), attack.getPath());
        return tail;
    }

    /**
     * The attack sound file of each held note's hold sound file whose release tail has been played (see
     * {@link #releaseTailWithPedals}), so the tail can be rendered from both: a rendered copy is named after one file
     */
    private static final Map<ResourceLocation, ResourceLocation> ATTACKS = new ConcurrentHashMap<>();
    /**
     * @param hold A held note's hold sound file (sounds/...ogg)
     * @return Its attack sound file, if a release tail was played from it (see {@link #releaseTailWithPedals})
     */
    @Nullable
    static ResourceLocation attackOf(final ResourceLocation hold) {
        return ATTACKS.get(hold);
    }
    /**
     * @return The fade's length rounded to within about 10%, as it changes the tail by less than its own rounding
     * to whole ticks does on shorter fades
     */
    private static int roundFadeTicks(final int fadeTicks) {
        if (fadeTicks <= 10)
            return Math.max(1, fadeTicks);
        final int step = (fadeTicks <= 20) ? 2 : (fadeTicks <= 40) ? 4 : 8;
        return Math.round((float) fadeTicks / step) * step;
    }

    private static Sound withPedals(final Sound sound, final PedalChain fullChain, final float pitch,
                                    final Kind kind, final int... timings) {
        // Live effects aren't rendered, so turning their knobs never renders anything (see PedalLiveEffects)
        final PedalChain chain = fullChain.rendered();
        if (chain.isEmpty() || sound == SoundManager.EMPTY_SOUND || sound == SoundManager.INTENTIONALLY_EMPTY_SOUND
            || sound.shouldStream() || sound.getType() != Sound.Type.FILE)
            return sound;

        String variant = kind.letter + String.valueOf(Math.max(1, Math.round(pitch * 1000)));
        for (int i = 0; i < kind.timings(); i++)
            variant += PERIOD_SEPARATOR + ((i < timings.length) ? timings[i] : 0);

        final ResourceLocation original = sound.getLocation();
        return new Sound(
            SCInstrumentMod.MODID + ":" + FX_DIR + chain.key() + "/" + variant + "/"
                + original.getNamespace() + "/" + original.getPath(),
            sound.getVolume(), sound.getPitch(), sound.getWeight(), Sound.Type.FILE,
            false, false, sound.getAttenuationDistance()
        );
    }

    /**
     * A rendered copy of a sound file
     * @param pitch The pitch it will be played at
     * @param periodTicks For holds and release tails, how many ticks apart holds start (see {@link #holdWithPedals}),
     *                    otherwise 0
     * @param fadeTicks For release tails, how many ticks the note takes to fade out, otherwise 0
     * @param heldTicks For release tails, how long after its attack started the note was let go
     *                  (see {@link #releaseTailWithPedals})
     * @param attackLeadTicks For release tails, how long before its first hold its attack starts
     * @param original The original sound file's location (sounds/...ogg)
     */
    record FxFile(PedalChain chain, float pitch, Kind kind, int periodTicks, int fadeTicks, int heldTicks,
                  int attackLeadTicks, ResourceLocation original) {}

    /**
     * @param file A sound file's location (sounds/...ogg)
     * @return What the file is a rendered copy of, or null if it isn't one
     */
    @Nullable
    static FxFile parse(final ResourceLocation file) {
        final String prefix = SOUNDS_PREFIX + FX_DIR;
        if (!file.getNamespace().equals(SCInstrumentMod.MODID) || !file.getPath().startsWith(prefix))
            return null;

        // <chain key>/<variant>/<original namespace>/<original path>.ogg
        final String[] parts = file.getPath().substring(prefix.length()).split("/", 4);
        if (parts.length < 4)
            return null;

        final PedalChain chain = PedalChain.fromKey(parts[0]);
        final ResourceLocation original = ResourceLocation.tryBuild(parts[2], SOUNDS_PREFIX + parts[3]);
        if (chain == null || chain.isEmpty() || original == null || !original.getPath().endsWith(OGG_SUFFIX))
            return null;

        final String variant = parts[1];
        final Kind kind = variant.isEmpty() ? null : Kind.of(variant.charAt(0));
        if (kind == null)
            return null;
        // <pitch>[-<period ticks>[-<fade ticks>-<held ticks>-<attack lead ticks>]]
        final String[] numbers = variant.substring(1).split(PERIOD_SEPARATOR, -1);
        if (numbers.length != 1 + kind.timings())
            return null;
        try {
            final int[] values = new int[5];
            for (int i = 0; i < numbers.length; i++)
                values[i] = Integer.parseInt(numbers[i]);
            final int milli = values[0], periodTicks = values[1], fadeTicks = values[2];
            if (milli <= 0 || (kind.timings() >= 1 && periodTicks <= 0) || (kind.timings() >= 2 && fadeTicks <= 0)
                || (kind.timings() >= 3 && values[3] <= 0) || values[4] < 0)
                return null;
            return new FxFile(chain, milli / 1000f, kind, periodTicks, fadeTicks, values[3], values[4], original);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    public static boolean isFxFile(final ResourceLocation file) {
        return file.getNamespace().equals(SCInstrumentMod.MODID) && file.getPath().startsWith(SOUNDS_PREFIX + FX_DIR);
    }
}
