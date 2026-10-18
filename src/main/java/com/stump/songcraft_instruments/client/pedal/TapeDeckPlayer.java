package com.stump.songcraft_instruments.client.pedal;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.networking.packet.S2CTapeDeckPacket;
import com.stump.songcraft_instruments.networking.packet.S2CTapeDeckPacket.Emitter;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.pedal.TapeHissSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plays the Lo-fi tapes the server tells us about (see {@code TapeDecks}): one hiss per tape while it rolls, louder the
 * nearer we are to the nearest place it's heard from (see {@link TapeHissSoundInstance}), and its play and stop keys'
 * clunks from the nearest of those places.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public final class TapeDeckPlayer {
    private static final class Deck {
        TapeHissSoundInstance hiss;
        float volume;
        List<Emitter> emitters;
    }

    private static final Map<String, Deck> DECKS = new HashMap<>();

    private TapeDeckPlayer() {}

    public static void handle(final S2CTapeDeckPacket packet) {
        switch (packet.action) {
            case START -> start(packet, true);
            case JOIN -> start(packet, false);
            case UPDATE -> update(packet);
            case STOP -> stop(packet.deckId, true);
            case LEAVE -> stop(packet.deckId, false);
        }
    }

    /**
     * @param withKeys Whether it's heard starting: its play key clunks and its motor spins up. Otherwise it fades in,
     *                 as when coming within range of a tape already rolling.
     */
    private static void start(final S2CTapeDeckPacket packet, final boolean withKeys) {
        if (DECKS.containsKey(packet.deckId)) {
            update(packet);
            return;
        }

        final Deck deck = new Deck();
        deck.volume = packet.volume;
        deck.emitters = packet.emitters;
        deck.hiss = new TapeHissSoundInstance(SCSounds.TAPE_HISS.get(), packet.emitters, packet.effect, packet.volume, withKeys);
        DECKS.put(packet.deckId, deck);

        Minecraft.getInstance().getSoundManager().play(deck.hiss);
        if (withKeys)
            playKey(SCSounds.TAPE_START.get(), deck);
    }

    private static void update(final S2CTapeDeckPacket packet) {
        final Deck deck = DECKS.get(packet.deckId);
        if (deck == null) {
            start(packet, false);
            return;
        }

        deck.volume = packet.volume;
        deck.emitters = packet.emitters;
        deck.hiss.setSettings(packet.effect, packet.volume, packet.emitters);
    }

    /**
     * @param withKeys Whether it's heard stopping: its stop key clunks and its motor winds down.
     *                 Otherwise it fades out, as when going out of range.
     */
    private static void stop(final String deckId, final boolean withKeys) {
        final Deck deck = DECKS.remove(deckId);
        if (deck == null)
            return;

        deck.hiss.stopTape(withKeys);
        if (withKeys)
            playKey(SCSounds.TAPE_STOP.get(), deck);
    }

    /**
     * Plays a key's clunk from the tape's place nearest us
     */
    private static void playKey(final SoundEvent sound, final Deck deck) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
            return;
        final Vec3 at = TapeHissSoundInstance.nearestOf(deck.emitters, minecraft.player.position());
        if (at == null)
            return;

        minecraft.getSoundManager().play(new SimpleSoundInstance(
            sound, NoteSound.INSTRUMENT_SOUND_SOURCE, deck.volume, 1, SoundInstance.createUnseededRandom(), at.x, at.y, at.z
        ));
    }

    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        // The sound engine stops the hiss itself as the world unloads
        DECKS.clear();
    }
}
