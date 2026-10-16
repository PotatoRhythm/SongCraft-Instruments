package com.stump.songcraft_instruments.pedal;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.event.InstrumentOpenStateChangedEvent;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.S2CPedalMonitorPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets players hear their own notes through the pedals their instrument is plugged into.
 * <p>A player hears their own copy of their notes without waiting on the server, so the chain of the instrument they
 * have open is sent to them, to play that copy through (see {@code PedalMonitor} on the client). Everyone else gets
 * the chain along with each note.</p>
 * <p>Sent when an instrument is opened, and again whenever pedals change. Pedals only apply while they're loaded,
 * so the chain is also checked every {@link #CHECK_INTERVAL} ticks, in case its pedals loaded or unloaded.
 * Only sent when it actually differs from what the player has. Server only.</p>
 */
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public final class PedalMonitorSync {
    private static final int CHECK_INTERVAL = 20;

    /**
     * What each player was last sent, so an unchanged chain isn't sent again
     */
    private static final Map<UUID, PedalChain> SENT = new ConcurrentHashMap<>();
    private static int ticksToCheck = CHECK_INTERVAL;

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

    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || --ticksToCheck > 0)
            return;

        ticksToCheck = CHECK_INTERVAL;
        for (final ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (InstrumentOpenProvider.isOpen(player))
                sync(player);
        }
    }

    /**
     * Called when a pedal's settings or cables changed, or an instrument was plugged in or unplugged.
     * Changes come in at most as fast as players click, and few players have an instrument open at once,
     * so simply check them all.
     */
    public static void onPedalsChanged(final Level level) {
        if (level.isClientSide)
            return;

        for (final Player player : level.players()) {
            if ((player instanceof ServerPlayer serverPlayer) && InstrumentOpenProvider.isOpen(player))
                sync(serverPlayer);
        }
    }

    private static void sync(final ServerPlayer player) {
        final PedalChain chain = InstrumentPedals.chainOfOpenInstrument(player);

        final PedalChain previous = SENT.put(player.getUUID(), chain);
        // A player who never had any pedals needn't be told they have none
        if (chain.equals(previous) || (previous == null && chain.isEmpty()))
            return;

        SCPacketHandler.sendToClient(new S2CPedalMonitorPacket(chain), player);
    }
}
