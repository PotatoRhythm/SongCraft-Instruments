package com.stump.songcraft_instruments.pedal;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.speaker.Speaker;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.event.InstrumentOpenStateChangedEvent;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.S2CPedalMonitorPacket;
import com.stump.songcraft_instruments.util.SpeakerUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets players hear their own notes through the pedals of the speakers their instrument plays through.
 * <p>Speakers don't relay notes back to whoever played them (they already hear their own copy, without delay),
 * so the pedal chains of the open instrument's speakers are sent to its player instead, who applies them
 * to their own copy (see {@code PedalMonitor} on the client).</p>
 * <p>Sent when an instrument is opened, and again when something those chains depend on changes:
 * a speaker's chain, or which speakers are paired. Only sent when it actually differs from what the player has.
 * Server only.</p>
 */
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public final class PedalMonitorSync {
    /**
     * What each player was last sent, so unchanged lists aren't sent again
     */
    private static final Map<UUID, List<S2CPedalMonitorPacket.Entry>> SENT = new ConcurrentHashMap<>();

    private PedalMonitorSync() {}

    @SubscribeEvent
    public static void onInstrumentOpenChanged(final InstrumentOpenStateChangedEvent event) {
        if (event.isOpen && (event.player instanceof ServerPlayer player))
            sync(player);
    }

    @SubscribeEvent
    public static void onLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }
    @SubscribeEvent
    public static void onChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            sync(player);
    }

    /**
     * Called when a speaker's pedal chain changed
     */
    public static void onSpeakerChainChanged(final Level level) {
        syncPlayersWithOpenInstruments(level);
    }
    /**
     * Called when an instrument was paired to or unpaired from a speaker
     */
    public static void onPairingsChanged(final Level level) {
        syncPlayersWithOpenInstruments(level);
    }

    /**
     * Pairings and chains change rarely (and chains at most a few times a second per speaker),
     * and few players have an instrument open at once, so simply check them all
     */
    private static void syncPlayersWithOpenInstruments(final Level level) {
        if (level.isClientSide)
            return;

        for (final Player player : level.players()) {
            if ((player instanceof ServerPlayer serverPlayer) && InstrumentOpenProvider.isOpen(player))
                sync(serverPlayer);
        }
    }

    private static void sync(final ServerPlayer player) {
        final List<S2CPedalMonitorPacket.Entry> entries = new ArrayList<>();
        for (final Speaker speaker : SpeakerUtil.getFromOpenInstrument(player)) {
            if (!speaker.getPedalChain().isEmpty())
                entries.add(new S2CPedalMonitorPacket.Entry(speaker.getSoundPos(), speaker.getPedalChain()));
        }

        final List<S2CPedalMonitorPacket.Entry> previous = SENT.put(player.getUUID(), entries);
        // A player who never had any pedals needn't be told they have none
        if (entries.equals(previous) || (previous == null && entries.isEmpty()))
            return;

        SCPacketHandler.sendToClient(new S2CPedalMonitorPacket(entries), player);
    }
}
