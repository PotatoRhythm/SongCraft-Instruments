package com.stump.songcraft_instruments.sound.pedal;

import com.mojang.blaze3d.audio.SoundBuffer;
import com.mojang.logging.LogUtils;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.sound.ChannelTwinSounds;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * The rendered copies of sounds played through pedals (see {@link PedalSounds}), in place of vanilla's sound buffer
 * cache, which never lets go of anything until resources reload.
 * <p>Each copy is rendered once in the background, and kept until the cache grows past its budget
 * ({@link ModClientConfigs#PEDAL_CACHE_MB}). The least recently played copies are then let go,
 * but never while a sound is playing them.</p>
 * <p>A copy is played the moment it is rendered (a few milliseconds the first time), so notes are never dropped.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class PedalBufferCache {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * A copy played this recently may be about to start playing, so is never let go
     */
    private static final long MIN_IDLE_NANOS = 5_000_000_000L;
    /**
     * A prewarm stops once it has rendered this share of the budget, so it never pushes everything else out
     */
    private static final double PREWARM_BUDGET_SHARE = 0.5;

    private static final class Entry {
        final CompletableFuture<SoundBuffer> buffer;
        final int generation;
        long bytes;
        long lastUsedNanos = System.nanoTime();
        /**
         * How many sources are playing this copy right now
         */
        int users;

        Entry(final CompletableFuture<SoundBuffer> buffer, final int generation) {
            this.buffer = buffer;
            this.generation = generation;
        }
    }

    private static final Object LOCK = new Object();
    /**
     * By sound file location, least recently used first
     */
    private static final LinkedHashMap<ResourceLocation, Entry> ENTRIES = new LinkedHashMap<>(16, 0.75f, true);
    private static final Map<SoundBuffer, Entry> BY_BUFFER = new IdentityHashMap<>();
    private static final Map<Integer, Entry> BY_SOURCE = new HashMap<>();
    private static long totalBytes;
    /**
     * Bumped whenever the cache is cleared, so renders that finish afterwards aren't counted
     */
    private static int generation;
    /**
     * Bumped whenever a prewarm starts (or the cache is cleared), so the one before it stops
     */
    private static int prewarmRun;

    /**
     * The sound files vanilla's sound buffers are loaded from, including the mod's converted twins
     */
    private static volatile ResourceProvider resources;

    private PedalBufferCache() {}

    public static void setResources(final ResourceProvider soundResources) {
        resources = soundResources;
    }


    /**
     * @param file A rendered copy's sound file location, see {@link PedalSounds#isFxFile}
     * @return The rendered copy, rendering it if it isn't yet
     */
    public static CompletableFuture<SoundBuffer> get(final ResourceLocation file) {
        final ResourceProvider resources = PedalBufferCache.resources;
        if (resources == null)
            return CompletableFuture.failedFuture(new IllegalStateException("Sounds are not loaded yet"));

        synchronized (LOCK) {
            final Entry existing = ENTRIES.get(file);
            if (existing != null) {
                existing.lastUsedNanos = System.nanoTime();
                return existing.buffer;
            }

            final int entryGeneration = generation;
            final CompletableFuture<SoundBuffer> future = new CompletableFuture<>();
            final Entry entry = new Entry(future, entryGeneration);
            ENTRIES.put(file, entry);

            Util.backgroundExecutor().execute(() -> {
                try {
                    future.complete(render(resources, file, entry));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        }
    }

    /**
     * Renders copies ahead of time, so they are ready the first time they are played. Replaces any prewarm
     * still going.
     * <p>Renders one copy at a time, so it doesn't crowd out notes being played (or the rest of the game's
     * background work), and stops after {@link #PREWARM_BUDGET_SHARE} of the budget. Copies it didn't get to
     * render the first time they're played, as usual.</p>
     * @param sounds The rendered copies, see {@link PedalSounds#withPedals}
     */
    public static void prewarm(final List<Sound> sounds) {
        final Deque<ResourceLocation> files = new ArrayDeque<>();
        for (final Sound sound : sounds)
            if (PedalSounds.isFxFile(sound.getPath()))
                files.add(sound.getPath());

        final int run;
        synchronized (LOCK) {
            run = ++prewarmRun;
        }
        prewarmNext(files, run, 0);
    }
    private static void prewarmNext(final Deque<ResourceLocation> files, final int run, final long renderedBytes) {
        if (resources == null || renderedBytes >= budgetBytes() * PREWARM_BUDGET_SHARE)
            return;

        ResourceLocation file;
        synchronized (LOCK) {
            if (run != prewarmRun)
                return;
            // Skips copies already rendered (or rendering), without counting it as them being played
            do {
                file = files.poll();
            } while (file != null && ENTRIES.containsKey(file));
        }
        if (file == null)
            return;

        get(file).whenComplete((buffer, error) -> {
            long bytes = 0;
            synchronized (LOCK) {
                final Entry entry = (buffer != null) ? BY_BUFFER.get(buffer) : null;
                if (entry != null)
                    bytes = entry.bytes;
            }
            prewarmNext(files, run, renderedBytes + bytes);
        });
    }

    private static SoundBuffer render(final ResourceProvider resources, final ResourceLocation file, final Entry entry) {
        final PedalSounds.FxFile fx = PedalSounds.parse(file);
        if (fx == null)
            throw new CompletionException(new IllegalArgumentException("Not a pedal sound: " + file));

        final ChannelTwinSounds.Decoded dry = ChannelTwinSounds.decode(resources, fx.original());
        ByteBuffer samples;
        try {
            samples = PedalDsp.render(dry, fx.chain(), fx.pitch(), fx.echoTail());
        } catch (RuntimeException e) {
            // Better heard dry than not at all
            LOGGER.error("Failed to render {} through pedals {}; playing it without them", fx.original(), fx.chain().key(), e);
            samples = dry.samples();
        }

        final SoundBuffer buffer = new SoundBuffer(samples, dry.format());
        synchronized (LOCK) {
            if (entry.generation == generation) {
                entry.bytes = samples.remaining();
                totalBytes += entry.bytes;
                BY_BUFFER.put(buffer, entry);
            }
        }
        return buffer;
    }


    // Tracking what is playing, on the sound thread

    /**
     * Called when a channel's source is given a buffer to play
     */
    public static void onAttach(final int source, final SoundBuffer buffer) {
        synchronized (LOCK) {
            final Entry entry = BY_BUFFER.get(buffer);
            if (entry == null)
                return;

            entry.users++;
            entry.lastUsedNanos = System.nanoTime();
            BY_SOURCE.put(source, entry);
            evictOverBudget();
        }
    }
    /**
     * Called when a channel's source is freed
     */
    public static void onDetach(final int source) {
        synchronized (LOCK) {
            final Entry entry = BY_SOURCE.remove(source);
            if (entry != null)
                entry.users--;
        }
    }

    /**
     * Lets go of the least recently played copies nothing is playing, until the cache is within its budget
     */
    /**
     * Called on the main thread every sound engine tick. Lets go of copies past the budget on the sound thread,
     * as copies that are rendered but never played (like prewarms) don't go through {@link #onAttach}, and copies
     * still too recently played to let go there only become idle later.
     */
    public static void tick(final Executor soundExecutor) {
        synchronized (LOCK) {
            if (totalBytes <= budgetBytes())
                return;
        }
        soundExecutor.execute(() -> {
            synchronized (LOCK) {
                evictOverBudget();
            }
        });
    }

    private static long budgetBytes() {
        return ModClientConfigs.PEDAL_CACHE_MB.get() * 1024L * 1024L;
    }

    private static void evictOverBudget() {
        final long budget = budgetBytes();
        if (totalBytes <= budget)
            return;

        final long now = System.nanoTime();
        final Iterator<Entry> iterator = ENTRIES.values().iterator();
        while (iterator.hasNext() && totalBytes > budget) {
            final Entry entry = iterator.next();
            if (entry.users > 0 || !entry.buffer.isDone() || (now - entry.lastUsedNanos < MIN_IDLE_NANOS))
                continue;

            iterator.remove();
            discard(entry);
        }
    }

    private static void discard(final Entry entry) {
        // A render that failed has no buffer, and getNow would throw its exception
        final SoundBuffer buffer = entry.buffer.isCompletedExceptionally() ? null : entry.buffer.getNow(null);
        if (buffer != null) {
            BY_BUFFER.remove(buffer);
            buffer.discardAlBuffer();
        }
        totalBytes -= entry.bytes;
        entry.bytes = 0;
    }

    /**
     * Lets go of every copy. Called before the sound engine destroys its OpenAL context, and our buffers with it.
     */
    public static void clear() {
        synchronized (LOCK) {
            for (final Entry entry : ENTRIES.values())
                discard(entry);
            ENTRIES.clear();
            BY_BUFFER.clear();
            BY_SOURCE.clear();
            totalBytes = 0;
            generation++;
            prewarmRun++;
        }
    }
}
