package com.stump.songcraft_instruments.client.pedal;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.networking.packet.S2CTapeDeckPacket;
import com.stump.songcraft_instruments.networking.packet.S2CTapeDeckPacket.Emitter;
import com.stump.songcraft_instruments.pedal.PedalEffect;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.pedal.TapeHissSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Plays the Lo-fi tape decks the server tells us about (see {@code TapeDecks}): a looping hiss at each place an
 * instrument (or looper) is heard while it rolls, and its play and stop keys' clunks.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public final class TapeDeckPlayer {
    private static final class Deck {
        PedalEffect effect;
        float volume;
        final Map<Emitter, TapeHissSoundInstance> hiss = new HashMap<>();
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
     *                 as when coming within range of a deck already rolling.
     */
    private static void start(final S2CTapeDeckPacket packet, final boolean withKeys) {
        if (DECKS.containsKey(packet.deckId)) {
            update(packet);
            return;
        }

        final Deck deck = new Deck();
        deck.effect = packet.effect;
        deck.volume = packet.volume;
        DECKS.put(packet.deckId, deck);

        for (final Emitter emitter : packet.emitters) {
            startHiss(deck, emitter, withKeys);
            if (withKeys)
                playKey(SCSounds.TAPE_START.get(), emitter, deck.volume);
        }
    }

    private static void update(final S2CTapeDeckPacket packet) {
        final Deck deck = DECKS.get(packet.deckId);
        if (deck == null) {
            start(packet, false);
            return;
        }

        deck.effect = packet.effect;
        deck.volume = packet.volume;

        // Places it's no longer heard from fade out; new ones fade in
        deck.hiss.entrySet().removeIf((entry) -> {
            if (packet.emitters.contains(entry.getKey()))
                return false;
            entry.getValue().stopTape(false);
            return true;
        });
        for (final Emitter emitter : packet.emitters) {
            final TapeHissSoundInstance hiss = deck.hiss.get(emitter);
            if (hiss != null)
                hiss.setSettings(deck.effect, deck.volume);
            else
                startHiss(deck, emitter, false);
        }
    }

    /**
     * @param withKeys Whether it's heard stopping: its stop key clunks and its motor winds down.
     *                 Otherwise it fades out, as when going out of range.
     */
    private static void stop(final String deckId, final boolean withKeys) {
        final Deck deck = DECKS.remove(deckId);
        if (deck == null)
            return;

        for (final TapeHissSoundInstance hiss : deck.hiss.values()) {
            hiss.stopTape(withKeys);
            if (withKeys)
                playKey(SCSounds.TAPE_STOP.get(), hiss.getEmitter(), deck.volume);
        }
    }

    private static void startHiss(final Deck deck, final Emitter emitter, final boolean withMotor) {
        final TapeHissSoundInstance hiss = new TapeHissSoundInstance(
            SCSounds.TAPE_HISS.get(), emitter, deck.effect, deck.volume, withMotor
        );
        deck.hiss.put(emitter, hiss);
        Minecraft.getInstance().getSoundManager().play(hiss);
    }

    private static void playKey(final SoundEvent sound, final Emitter emitter, final float volume) {
        final Vec3 at = positionOf(emitter);
        if (at == null)
            return;

        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
            sound, NoteSound.INSTRUMENT_SOUND_SOURCE, volume, 1, SoundInstance.createUnseededRandom(), at.x, at.y, at.z
        ));
    }

    @Nullable
    private static Vec3 positionOf(final Emitter emitter) {
        if (emitter.pos() != null)
            return emitter.pos().getCenter();

        final Minecraft minecraft = Minecraft.getInstance();
        final Entity entity = (minecraft.level == null) ? null : minecraft.level.getEntity(emitter.entityId());
        return (entity == null) ? null : entity.position();
    }

    @SubscribeEvent
    public static void onLoggingOut(final ClientPlayerNetworkEvent.LoggingOut event) {
        // The sound engine stops the hiss itself as the world unloads
        DECKS.clear();
    }
}
