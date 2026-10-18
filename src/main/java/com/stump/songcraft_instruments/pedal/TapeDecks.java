package com.stump.songcraft_instruments.pedal;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.speaker.Speaker;
import com.stump.songcraft_instruments.block.blockentity.speaker.SpeakerAddress;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.event.InstrumentOpenStateChangedEvent;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.S2CTapeDeckPacket;
import com.stump.songcraft_instruments.networking.packet.S2CTapeDeckPacket.Action;
import com.stump.songcraft_instruments.networking.packet.S2CTapeDeckPacket.Emitter;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.networking.packet.instrument.util.InstrumentPacketUtil;
import com.stump.songcraft_instruments.util.SpeakerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Lo-fi pedal's tape decks: each Lo-fi pedal is one tape, whose hiss rolls while anyone plays through it.
 * <p>A deck starts rolling (its play key clunks) when a player opens an instrument plugged into its pedal, or plays the
 * first note through it. Each player playing
 * through it is a source: their instrument and its speakers are places the tape is heard from while they play or hold
 * notes. Each listener hears one hiss per tape, as loud as they are near its nearest place (see
 * {@code TapeHissSoundInstance}). Once all of them have gone quiet for {@link #IDLE_TICKS} (or closed their instruments),
 * the stop key clunks and the tape winds down, like a tape deck that stops when the band does.</p>
 * <p>A looper plays back each tape it recorded as a deck of its own, heard from the looper and its speakers, so a recording
 * starts and stops its tapes as they did when it was played.</p>
 * <p>Players within range are told when a deck starts, changes, or stops, and when they come within or go out of its
 * range; their own game plays the hiss and the deck's key clunks (see {@code TapeDeckPlayer}). Server only.</p>
 */
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public final class TapeDecks {
    /**
     * How long a source keeps the tape rolling after its last note (or opening their instrument), with no notes held
     */
    private static final int IDLE_TICKS = 10 * 20;
    /**
     * How far from any of its places a deck's hiss is sent, a little past where notes are heard
     */
    private static final double RANGE = InstrumentPacketUtil.PLAY_DISTANCE + 16;
    /**
     * How often (in ticks) each deck's listeners are checked, for players coming within or going out of its range
     */
    private static final int LISTENER_CHECK_INTERVAL = 10;

    /**
     * Someone playing through a deck: a player, or a looper playing back a recording
     */
    private static final class Source {
        List<Emitter> emitters = List.of();
        float volume;
        long lastNoteTick;
        /**
         * The notes held right now, which keep it going however long they're held
         */
        final Set<Object> held = new HashSet<>();

        boolean isActive(final long now) {
            return !held.isEmpty() || (now - lastNoteTick <= IDLE_TICKS);
        }
    }

    private static final class Deck {
        final String id;
        final Level level;
        PedalEffect effect;
        final Map<String, Source> sources = new LinkedHashMap<>();
        /**
         * What the deck's listeners were last told: its places and volume
         */
        List<Emitter> sentEmitters = List.of();
        PedalEffect sentEffect;
        float sentVolume;
        /**
         * The players told it's rolling
         */
        final Set<UUID> listeners = new HashSet<>();

        Deck(final String id, final Level level) {
            this.id = id;
            this.level = level;
        }

        /**
         * @return Every active source's places, each once (a speaker two players share hisses once)
         */
        List<Emitter> emitters() {
            final List<Emitter> emitters = new ArrayList<>();
            for (final Source source : sources.values())
                for (final Emitter emitter : source.emitters)
                    if (!emitters.contains(emitter))
                        emitters.add(emitter);
            return emitters;
        }
        /**
         * @return The loudest active source's volume
         */
        float volume() {
            float volume = 0;
            for (final Source source : sources.values())
                volume = Math.max(volume, source.volume);
            return volume;
        }
    }

    private static final Map<String, Deck> DECKS = new HashMap<>();
    /**
     * The deck each player is playing through, so they leave it when they switch to another (or none)
     */
    private static final Map<String, String> PLAYER_DECKS = new HashMap<>();
    private static int ticksToListenerCheck = LISTENER_CHECK_INTERVAL;

    private TapeDecks() {}


    //#region Notes

    /**
     * Called for every note a player plays, with the pedals it's played through
     * @param heldPhase The phase, for a held note; null for any other note
     * @param heldKey What identifies the held note between its attack and release
     */
    public static void onPlayerNote(final Player player, final PedalChain chain, final int volume,
                                    @Nullable final HeldSoundPhase heldPhase, @Nullable final Object heldKey) {
        if (player.level().isClientSide)
            return;

        final String sourceId = playerSourceId(player);
        final PedalEffect effect = chain.tapeEffect();
        final UUID tape = (effect == null) ? null : InstrumentPedals.tapeOfOpenInstrument(player);

        // Switched to another tape, or to none (the pedal switched off, or unplugged): leave the old one
        final String deckId = (tape == null) ? null : "pedal:" + tape;
        final String previous = PLAYER_DECKS.get(sourceId);
        if (previous != null && !previous.equals(deckId)) {
            PLAYER_DECKS.remove(sourceId);
            leave(previous, sourceId);
        }
        if (deckId == null)
            return;

        PLAYER_DECKS.put(sourceId, deckId);
        onNote(deckId, player.level(), sourceId, effect, volume, heldPhase, heldKey, () -> playerEmitters(player));
    }

    /**
     * Called for every note a looper plays back, with the pedals and tape it was recorded through
     * @param looperId Identifies the looper, so each looper plays its tapes as decks of its own
     * @param tape The tape the note was recorded through (see {@code Recording#tape}); empty if it isn't known
     * @param heldKey What identifies the held note between its attack and release, including who recorded it
     */
    public static void onLooperNote(final String looperId, final String tape, final Level level, final BlockPos soundPos,
                                    final List<Speaker> speakers, final PedalChain chain, final int volume,
                                    @Nullable final HeldSoundPhase heldPhase, @Nullable final Object heldKey) {
        final PedalEffect effect = chain.tapeEffect();
        if (level.isClientSide || effect == null)
            return;

        onNote("looper:" + looperId + "@" + tape, level, "looper:" + looperId, effect, volume, heldPhase, heldKey, () -> {
            final List<Emitter> emitters = new ArrayList<>();
            emitters.add(Emitter.ofBlock(soundPos));
            addSpeakerEmitters(level, speakers, emitters);
            return emitters;
        });
    }

    private interface EmitterSupplier {
        List<Emitter> get();
    }

    private static void onNote(final String deckId, final Level level, final String sourceId, final PedalEffect effect,
                               final int volume, @Nullable final HeldSoundPhase heldPhase, @Nullable final Object heldKey,
                               final EmitterSupplier emitters) {
        Deck deck = DECKS.get(deckId);
        Source source = (deck == null) ? null : deck.sources.get(sourceId);

        // A release only ends a held note; it doesn't start anything on its own
        if (heldPhase == HeldSoundPhase.RELEASE) {
            if (source != null && heldKey != null) {
                source.held.remove(heldKey);
                source.lastNoteTick = level.getGameTime();
            }
            return;
        }

        final boolean starting = (deck == null);
        if (starting) {
            deck = new Deck(deckId, level);
            DECKS.put(deckId, deck);
        }
        if (source == null) {
            source = new Source();
            deck.sources.put(sourceId, source);
        }

        deck.effect = effect;
        source.emitters = emitters.get();
        source.volume = volume / 100f;
        source.lastNoteTick = level.getGameTime();
        if (heldPhase == HeldSoundPhase.ATTACK && heldKey != null)
            source.held.add(heldKey);

        if (starting) {
            snapshot(deck);
            updateListeners(deck, Action.START);
        } else {
            sendIfChanged(deck);
        }
    }

    /**
     * Called when a player dampens their notes: whatever they held is let go of at once
     */
    public static void onDampen(final Player player) {
        final String sourceId = playerSourceId(player);
        final Deck deck = DECKS.get(PLAYER_DECKS.get(sourceId));
        final Source source = (deck == null) ? null : deck.sources.get(sourceId);
        if (source != null)
            source.held.clear();
    }
    /**
     * Called when a looper stops playing: whatever it held, on any of its tapes, is let go of at once
     */
    public static void onLooperStopped(final String looperId) {
        for (final Deck deck : DECKS.values()) {
            final Source source = deck.sources.get("looper:" + looperId);
            if (source != null)
                source.held.clear();
        }
    }

    //#endregion


    //#region Events

    @SubscribeEvent
    public static void onInstrumentOpenChanged(final InstrumentOpenStateChangedEvent event) {
        if (event.player.level().isClientSide)
            return;

        if (event.isOpen) {
            // Opening an instrument plugged into a Lo-fi pedal starts its tape, as if a note were played.
            // At full volume until their first note says otherwise
            onPlayerNote(event.player, InstrumentPedals.chainOfOpenInstrument(event.player), 100, null, null);
        } else {
            // Closing the instrument stops its player's part at once; the tape stops if they were the last on it
            leavePlayer(event.player);
        }
    }
    @SubscribeEvent
    public static void onLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        leavePlayer(event.getEntity());
        DECKS.values().forEach((deck) -> deck.listeners.remove(event.getEntity().getUUID()));
    }
    @SubscribeEvent
    public static void onChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        leavePlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || DECKS.isEmpty())
            return;

        final boolean checkListeners = --ticksToListenerCheck <= 0;
        if (checkListeners)
            ticksToListenerCheck = LISTENER_CHECK_INTERVAL;

        for (final Iterator<Deck> iterator = DECKS.values().iterator(); iterator.hasNext(); ) {
            final Deck deck = iterator.next();
            final long now = deck.level.getGameTime();

            // Sources gone quiet stop hissing, without stopping the tape for the others
            deck.sources.entrySet().removeIf((entry) -> {
                if (entry.getValue().isActive(now))
                    return false;
                PLAYER_DECKS.remove(entry.getKey(), deck.id);
                return true;
            });

            if (deck.sources.isEmpty()) {
                sendToListeners(deck, Action.STOP);
                iterator.remove();
                continue;
            }

            sendIfChanged(deck);
            if (checkListeners)
                updateListeners(deck, Action.JOIN);
        }
    }

    //#endregion


    private static void leavePlayer(final Player player) {
        final String sourceId = playerSourceId(player);
        final String deckId = PLAYER_DECKS.remove(sourceId);
        if (deckId != null)
            leave(deckId, sourceId);
    }
    /**
     * Takes a source off a deck at once; the tape stops if it was the last one
     */
    private static void leave(final String deckId, final String sourceId) {
        final Deck deck = DECKS.get(deckId);
        if (deck == null || deck.sources.remove(sourceId) == null)
            return;

        if (deck.sources.isEmpty()) {
            sendToListeners(deck, Action.STOP);
            DECKS.remove(deckId);
        } else {
            sendIfChanged(deck);
        }
    }

    private static void snapshot(final Deck deck) {
        deck.sentEmitters = deck.emitters();
        deck.sentEffect = deck.effect;
        deck.sentVolume = deck.volume();
    }
    private static void sendIfChanged(final Deck deck) {
        final List<Emitter> emitters = deck.emitters();
        final float volume = deck.volume();
        if (emitters.equals(deck.sentEmitters) && deck.effect.equals(deck.sentEffect) && Math.abs(volume - deck.sentVolume) <= 0.01f)
            return;

        snapshot(deck);
        sendToListeners(deck, Action.UPDATE);
        // New places may bring new players within range
        updateListeners(deck, Action.JOIN);
    }

    /**
     * Tells players who came within the deck's range that it's rolling, and those who left that it's gone
     * @param joinAction How the players newly in range hear it start: with its key clunk, or fading in
     */
    private static void updateListeners(final Deck deck, final Action joinAction) {
        if (!(deck.level instanceof ServerLevel serverLevel))
            return;

        final List<Vec3> places = new ArrayList<>();
        for (final Emitter emitter : deck.sentEmitters) {
            final Vec3 place = positionOf(serverLevel, emitter);
            if (place != null)
                places.add(place);
        }

        for (final ServerPlayer player : serverLevel.players()) {
            final boolean inRange = places.stream().anyMatch((place) -> place.distanceToSqr(player.position()) < RANGE * RANGE);
            final boolean listening = deck.listeners.contains(player.getUUID());

            if (inRange && !listening) {
                deck.listeners.add(player.getUUID());
                SCPacketHandler.sendToClient(packet(deck, joinAction), player);
            } else if (!inRange && listening) {
                deck.listeners.remove(player.getUUID());
                SCPacketHandler.sendToClient(packet(deck, Action.LEAVE), player);
            }
        }
    }
    private static void sendToListeners(final Deck deck, final Action action) {
        if (!(deck.level instanceof ServerLevel serverLevel))
            return;

        final S2CTapeDeckPacket packet = packet(deck, action);
        for (final UUID listener : deck.listeners) {
            if (serverLevel.getPlayerByUUID(listener) instanceof ServerPlayer player)
                SCPacketHandler.sendToClient(packet, player);
        }
    }
    private static S2CTapeDeckPacket packet(final Deck deck, final Action action) {
        return new S2CTapeDeckPacket(deck.id, action, deck.sentEffect, deck.sentVolume, deck.sentEmitters);
    }

    @Nullable
    private static Vec3 positionOf(final ServerLevel level, final Emitter emitter) {
        if (emitter.pos() != null)
            return emitter.pos().getCenter();
        final Entity entity = level.getEntity(emitter.entityId());
        return (entity == null) ? null : entity.position();
    }


    private static String playerSourceId(final Player player) {
        return "player:" + player.getUUID();
    }

    /**
     * @return Where a player's notes are heard from: their instrument (held, or a block), and each of its speakers
     */
    private static List<Emitter> playerEmitters(final Player player) {
        final List<Emitter> emitters = new ArrayList<>();
        final BlockPos instrumentPos = InstrumentOpenProvider.isItem(player) ? null : InstrumentOpenProvider.getBlockPos(player);
        emitters.add((instrumentPos != null) ? Emitter.ofBlock(instrumentPos) : Emitter.ofEntity(player.getId()));
        addSpeakerEmitters(player.level(), SpeakerUtil.getFromOpenInstrument(player), emitters);
        return emitters;
    }
    private static void addSpeakerEmitters(final Level level, final List<Speaker> speakers, final List<Emitter> emitters) {
        for (final Speaker speaker : speakers) {
            final SpeakerAddress address = speaker.getAddress();
            if (address instanceof SpeakerAddress.AtBlock atBlock) {
                emitters.add(Emitter.ofBlock(atBlock.pos()));
            } else if (address instanceof SpeakerAddress.OnEntity onEntity && level instanceof ServerLevel serverLevel) {
                final Entity vehicle = serverLevel.getEntity(onEntity.entityId());
                if (vehicle != null)
                    emitters.add(Emitter.ofEntity(vehicle.getId()));
            }
        }
    }
}
