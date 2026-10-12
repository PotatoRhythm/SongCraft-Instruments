package com.stump.songcraft_instruments.client;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.KotoBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.koto.KotoStrings;
import com.stump.songcraft_instruments.client.gui.instrument.koto.KotoScreen;
import com.stump.songcraft_instruments.event.HeldNoteSoundPlayedEvent;
import com.stump.songcraft_instruments.event.InstrumentPlayedEvent;
import com.stump.songcraft_instruments.event.NoteSoundPlayedEvent;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.Optional;

/**
 * Plucks the strings of the koto that notes are played on, for everyone that hears them
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public class KotoStringListener {

    @SubscribeEvent
    public static void onNoteSoundPlayed(final NoteSoundPlayedEvent event) {
        getStrings(event).ifPresent((strings) ->
            strings.pluck(KotoStrings.stringOf(event.sound().index, event.soundMeta().pitch()))
        );
    }

    @SubscribeEvent
    public static void onHeldNoteSoundPlayed(final HeldNoteSoundPlayedEvent event) {
        if (event.phase != HeldSoundPhase.ATTACK)
            return;

        getStrings(event).ifPresent((strings) ->
            strings.pluck(KotoStrings.stringOf(event.sound().index(), event.soundMeta().pitch()))
        );
    }


    /**
     * @return The strings of the koto the event's note was played on, if it was played on one.
     * Notes relayed through speakers or loopers come from those blocks instead, so they pluck no strings.
     */
    private static Optional<KotoStrings> getStrings(final InstrumentPlayedEvent<?> event) {
        if (!event.level().isClientSide || !KotoScreen.INSTRUMENT_ID.equals(event.soundMeta().instrumentId()))
            return Optional.empty();

        return (event.level().getBlockEntity(event.soundMeta().pos()) instanceof KotoBlockEntity koto)
            ? Optional.of(koto.getKotoStrings())
            : Optional.empty();
    }
}
