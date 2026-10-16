package com.stump.songcraft_instruments.sound.pedal;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.pedal.PedalChain;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * Plays instrument sounds through effects pedals, by swapping a sound for a rendered copy of it.
 * <p>A rendered copy is named after its pedals and the original file:
 * {@code songcraft_instruments:pedal_fx/<chain key>/<variant>/<original namespace>/<original path>}. Vanilla loads it
 * like any other sound file, which {@link PedalBufferCache} catches and renders instead. Everything else about the
 * sound (its volume, attenuation, and the sound event it was played as) stays as it was.</p>
 * <p>The variant is the exact pitch the sound plays at, {@code p<pitch in thousandths>}: every chain is rendered for it,
 * so its filters and timing come out as set once OpenAL resamples the sound to that pitch, rather than moving with the
 * note. Most notes play at their sound file's own pitch ({@code p1000}) unless transposed, so they share one copy.
 * {@code t<pitch>} is a held note's echo tail (see {@link PedalDsp}).</p>
 */
@OnlyIn(Dist.CLIENT)
public final class PedalSounds {
    private static final String FX_DIR = "pedal_fx/";
    private static final String SOUNDS_PREFIX = "sounds/", OGG_SUFFIX = ".ogg";
    private static final String AT_PITCH = "p", ECHO_TAIL = "t";

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
    public static Sound withPedals(final Sound sound, final PedalChain fullChain, final float pitch, final boolean echoTail) {
        // Live effects aren't rendered, so turning their knobs never renders anything (see PedalLiveEffects)
        final PedalChain chain = fullChain.rendered();
        if (chain.isEmpty() || sound == SoundManager.EMPTY_SOUND || sound == SoundManager.INTENTIONALLY_EMPTY_SOUND
            || sound.shouldStream() || sound.getType() != Sound.Type.FILE)
            return sound;

        final String variant = (echoTail ? ECHO_TAIL : AT_PITCH) + Math.max(1, Math.round(pitch * 1000));

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
     * @param echoTail See {@link #withPedals(Sound, PedalChain, float, boolean)}
     * @param original The original sound file's location (sounds/...ogg)
     */
    record FxFile(PedalChain chain, float pitch, boolean echoTail, ResourceLocation original) {}

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
        final boolean echoTail = variant.startsWith(ECHO_TAIL);
        if (!echoTail && !variant.startsWith(AT_PITCH))
            return null;
        try {
            final int milli = Integer.parseInt(variant.substring(1));
            return (milli <= 0) ? null : new FxFile(chain, milli / 1000f, echoTail, original);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    public static boolean isFxFile(final ResourceLocation file) {
        return file.getNamespace().equals(SCInstrumentMod.MODID) && file.getPath().startsWith(SOUNDS_PREFIX + FX_DIR);
    }
}
