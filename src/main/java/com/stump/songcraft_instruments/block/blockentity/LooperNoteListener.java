package com.stump.songcraft_instruments.block.blockentity;

import com.stump.songcraft_instruments.block.blockentity.looper.LooperConnections;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.event.HeldNoteSoundPlayedEvent;
import com.stump.songcraft_instruments.event.InstrumentPlayedEvent;
import com.stump.songcraft_instruments.event.NoteSoundPlayedEvent;
import com.stump.songcraft_instruments.pedal.InstrumentPedals;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Listens to instrument played events
 * and writes it to a matching looper.
 */
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public class LooperNoteListener {

    @SubscribeEvent
    public static void onNoteSoundPlayed(final NoteSoundPlayedEvent event) {
        getMatchingLooper(event).ifPresent(looperBE -> {
            int rgb = event.soundMeta().particleColor();
            looperBE.writer().writeNote(
                    event.sound(),
                    event.soundMeta(),
                    looperBE.getTicks(),
                    rgb,
                    getPlayer(event).getUUID(),
                    event.pedalChain(),
                    tapeOf(event)
            );
        });
    }

    @SubscribeEvent
    public static void onHeldNoteSoundPlayed(final HeldNoteSoundPlayedEvent event) {
        getMatchingLooper(event).ifPresent(looperBE -> {
            int rgb = event.soundMeta().particleColor();
            looperBE.writer().writeHeldNote(
                    event.sound(),
                    event.phase,
                    event.soundMeta(),
                    looperBE.getTicks(),
                    rgb,
                    getPlayer(event).getUUID(),
                    event.pedalChain(),
                    tapeOf(event)
            );
        });
    }

    /**
     * @return The Lo-fi pedal (the tape) the note went through, or null
     */
    @Nullable
    private static UUID tapeOf(final InstrumentPlayedEvent<?> event) {
        return (event.pedalChain().tapeEffect() == null) ? null : InstrumentPedals.tapeOfOpenInstrument(getPlayer(event));
    }

    private static Player getPlayer(final InstrumentPlayedEvent<?> event) {
        return (Player) event.entityInfo().get().entity;
    }


    /**
     * @return The looper matching the provided event
     */
    private static Optional<LooperBlockEntity> getMatchingLooper(final InstrumentPlayedEvent<?> event) {
        // Only get player events
        if (!event.isByPlayer())
            return Optional.empty();

        final Player player = (Player) event.entityInfo().get().entity;

        if (event.level().isClientSide)
            return Optional.empty();


        final Level level = player.level();

        // Participants are recorded on whichever instrument they play
        final Optional<LooperBlockEntity> groupLooper = LooperConnections.getGroupSessionLooper(player);
        if (groupLooper.isEmpty())
            return Optional.empty();

        final LooperBlockEntity looperBE = groupLooper.get();
        // Out of range participants remain in the session, but are not recorded
        if (!looperBE.isWritable() || looperBE.writer().isCapped(level) || !looperBE.session().isInRecordRange(player))
            return Optional.empty();

        return looperBE.session().acceptGroupNote() ? groupLooper : Optional.empty();
    }
}
