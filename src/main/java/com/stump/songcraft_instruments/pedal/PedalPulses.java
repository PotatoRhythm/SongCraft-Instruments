package com.stump.songcraft_instruments.pedal;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.event.InstrumentOpenStateChangedEvent;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.S2CPedalPulsePacket;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Shows a player's notes going through the pedals their instrument is plugged into: specks in the note's color rise
 * from every pedal in the chain at once (see {@code PedalPulseRenderer}). Held notes keep them rising while held.
 * <p>Only live playing pulses: a looper's notes keep the chain they were recorded with, not the pedals' positions.
 * Server only.</p>
 */
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public final class PedalPulses {
    /**
     * The fewest ticks between two pulses of the same player, so chords and fast runs don't flood the pedals
     * with particles
     */
    private static final int MIN_INTERVAL = 4;
    /**
     * How often (in ticks) the pedals pulse again while a player holds a note, as a speaker's held particles do
     */
    private static final int HELD_INTERVAL = 10;

    private static final Map<Player, Long> lastPulse = new WeakHashMap<>();

    /**
     * The notes each player holds right now, by player ID
     */
    private static final Map<UUID, Held> held = new HashMap<>();

    private static final class Held {
        private final ServerPlayer player;
        /**
         * What identifies each held note between its attack and release
         */
        private final Set<Object> notes = new HashSet<>();
        /**
         * The color of the latest note held, which the pulses take
         */
        private int rgb;
        private int timer = 0;

        private Held(final ServerPlayer player) {
            this.player = player;
        }
    }

    private PedalPulses() {}

    /**
     * Sends a pulse through the pedals of the instrument the player has open, to everyone near any of them
     * @param rgb The note's particle color
     */
    public static void onPlayerNote(final Player player, final int rgb) {
        if (!(player.level() instanceof ServerLevel level))
            return;

        final long now = level.getGameTime();
        final Long last = lastPulse.get(player);
        if ((last != null) && (now - last < MIN_INTERVAL) && (now >= last))
            return;

        final List<BlockPos> path = InstrumentPedals.pedalPathOfOpenInstrument(player);
        if (path.isEmpty())
            return;
        lastPulse.put(player, now);

        final Set<ServerPlayer> watchers = new LinkedHashSet<>();
        for (final BlockPos pedal : path)
            watchers.addAll(level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pedal), false));

        final S2CPedalPulsePacket packet = new S2CPedalPulsePacket(path, rgb);
        for (final ServerPlayer watcher : watchers)
            SCPacketHandler.sendToClient(packet, watcher);
    }

    /**
     * A held note: its attack pulses, and the pedals keep pulsing until its release
     * @param key What identifies the held note between its attack and release
     */
    public static void onPlayerHeldNote(final Player player, final HeldSoundPhase phase, final Object key,
                                        final int rgb) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;

        if (phase == HeldSoundPhase.RELEASE) {
            final Held playerHeld = held.get(player.getUUID());
            if ((playerHeld != null) && playerHeld.notes.remove(key) && playerHeld.notes.isEmpty())
                held.remove(player.getUUID());
            return;
        }

        onPlayerNote(player, rgb);
        final Held playerHeld = held.computeIfAbsent(player.getUUID(), (id) -> new Held(serverPlayer));
        playerHeld.notes.add(key);
        playerHeld.rgb = rgb;
        playerHeld.timer = 0;
    }

    /**
     * Called when a player dampens their notes: whatever they held is let go of at once
     */
    public static void onDampen(final Player player) {
        held.remove(player.getUUID());
    }


    //#region Events

    @SubscribeEvent
    public static void onInstrumentOpenChanged(final InstrumentOpenStateChangedEvent event) {
        if (!event.isOpen && !event.player.level().isClientSide)
            held.remove(event.player.getUUID());
    }
    @SubscribeEvent
    public static void onLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        held.remove(event.getEntity().getUUID());
    }
    @SubscribeEvent
    public static void onChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        held.remove(event.getEntity().getUUID());
    }
    @SubscribeEvent
    public static void onRespawn(final PlayerEvent.PlayerRespawnEvent event) {
        held.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event) {
        if ((event.phase != TickEvent.Phase.END) || held.isEmpty())
            return;

        for (final Held playerHeld : held.values()) {
            if (++playerHeld.timer < HELD_INTERVAL)
                continue;
            playerHeld.timer = 0;
            onPlayerNote(playerHeld.player, playerHeld.rgb);
        }
    }

    //#endregion
}
