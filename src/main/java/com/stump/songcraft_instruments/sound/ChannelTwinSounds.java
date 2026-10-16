package com.stump.songcraft_instruments.sound;

import com.mojang.blaze3d.audio.OggAudioStream;
import com.mojang.blaze3d.audio.SoundBuffer;
import com.stump.songcraft_instruments.SCInstrumentMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;

/**
 * Lets the mod ship only one of a note's Mono ({@code x.ogg}) and Stereo ({@code x.stereo.ogg}) files.
 * <p>The missing twin is listed as a sound file backed by the one that exists, and is converted when loaded:
 * a Mono twin is the Stereo file mixed down as (L+R)/2, and a Stereo twin is the Mono file copied into both channels.
 * Only files that are already exact mixdowns, or Stereo files whose channels are identical, have been removed,
 * so the converted buffers sound the same as the files they replace.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ChannelTwinSounds {
    private static final String STEREO_SUFFIX = ".stereo.ogg";
    private static final String OGG_SUFFIX = ".ogg";

    /**
     * Sound file locations (sounds/...ogg) that do not exist and are converted from their twin
     */
    private static volatile Set<ResourceLocation> twinLocations = Set.of();

    private ChannelTwinSounds() {}

    /**
     * @param soundFiles The sound files found in the loaded resource packs
     * @return The given files, plus a missing twin for every one of this mod's sound files that lacks one
     */
    public static Map<ResourceLocation, Resource> withMissingTwins(final Map<ResourceLocation, Resource> soundFiles) {
        final Map<ResourceLocation, Resource> result = new HashMap<>(soundFiles);
        final Set<ResourceLocation> twins = new HashSet<>();

        soundFiles.forEach((location, resource) -> {
            if (!location.getNamespace().equals(SCInstrumentMod.MODID))
                return;

            final ResourceLocation twin = twinOf(location);
            if (twin != null && !soundFiles.containsKey(twin)) {
                result.put(twin, resource);
                twins.add(twin);
            }
        });

        twinLocations = Set.copyOf(twins);
        return result;
    }

    public static boolean isTwin(final ResourceLocation location) {
        return twinLocations.contains(location);
    }

    /**
     * Loads a missing twin by converting the file it is backed by.
     * @param resources Provides the twin's backing file under the twin's own location
     */
    public static SoundBuffer loadTwin(final ResourceProvider resources, final ResourceLocation location) {
        final Decoded decoded = decode(resources, location);
        return new SoundBuffer(decoded.samples(), decoded.format());
    }

    /**
     * A sound file's samples, as 16-bit little-endian PCM (interleaved if Stereo)
     */
    public record Decoded(ByteBuffer samples, AudioFormat format) {}

    /**
     * Decodes one of the mod's sound files, or converts it if it is a missing twin.
     * @param location The sound file's location (sounds/...ogg)
     */
    public static Decoded decode(final ResourceProvider resources, final ResourceLocation location) {
        try (
            InputStream input = resources.open(location);
            OggAudioStream stream = new OggAudioStream(input)
        ) {
            final ByteBuffer samples = stream.readAll();
            final AudioFormat format = stream.getFormat();
            if (!isTwin(location))
                return new Decoded(samples, format);

            final int channels = location.getPath().endsWith(STEREO_SUFFIX) ? 2 : 1;
            if (format.getChannels() == channels)
                return new Decoded(samples, format);

            final ByteBuffer converted = (channels == 1) ? mixDown(samples) : duplicateChannel(samples);
            return new Decoded(converted, new AudioFormat(format.getSampleRate(), 16, channels, true, false));
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }


    private static ResourceLocation twinOf(final ResourceLocation location) {
        final String path = location.getPath();

        if (path.endsWith(STEREO_SUFFIX))
            return location.withPath(path.substring(0, path.length() - STEREO_SUFFIX.length()) + OGG_SUFFIX);
        if (path.endsWith(OGG_SUFFIX))
            return location.withPath(path.substring(0, path.length() - OGG_SUFFIX.length()) + STEREO_SUFFIX);

        return null;
    }

    /**
     * Interleaved 16-bit Stereo samples to Mono, as (L+R)/2
     */
    private static ByteBuffer mixDown(final ByteBuffer stereo) {
        final ShortBuffer in = stereo.asShortBuffer();
        final int frames = in.remaining() / 2;

        final ByteBuffer result = BufferUtils.createByteBuffer(frames * 2);
        final ShortBuffer out = result.asShortBuffer();

        for (int i = 0; i < frames; i++)
            out.put((short) ((in.get() + in.get()) >> 1));

        return result;
    }

    /**
     * 16-bit Mono samples to interleaved Stereo, with the same signal in both channels
     */
    private static ByteBuffer duplicateChannel(final ByteBuffer mono) {
        final ShortBuffer in = mono.asShortBuffer();

        final ByteBuffer result = BufferUtils.createByteBuffer(in.remaining() * 4);
        final ShortBuffer out = result.asShortBuffer();

        while (in.hasRemaining()) {
            final short sample = in.get();
            out.put(sample).put(sample);
        }

        return result;
    }
}
