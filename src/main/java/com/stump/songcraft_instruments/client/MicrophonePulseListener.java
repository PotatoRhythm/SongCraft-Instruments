package com.stump.songcraft_instruments.client;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.MicrophoneStandBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.microphone.MicrophonePulse;
import com.stump.songcraft_instruments.client.gui.instrument.microphone.MicrophoneStandScreen;
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
 * Pulses the mic of the microphone stand that notes are sung into, for everyone that hears them
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public class MicrophonePulseListener {

    @SubscribeEvent
    public static void onNoteSoundPlayed(final NoteSoundPlayedEvent event) {
        getPulse(event).ifPresent(MicrophonePulse::tap);
    }

    @SubscribeEvent
    public static void onHeldNoteSoundPlayed(final HeldNoteSoundPlayedEvent event) {
        getPulse(event).ifPresent((pulse) -> {
            if (event.phase == HeldSoundPhase.ATTACK)
                pulse.hold();
            else
                pulse.release();
        });
    }


    /**
     * @return The pulse of the microphone stand the event's note was sung into, if it was sung into one.
     * Notes relayed through speakers or loopers come from those blocks instead, so they pulse no mic.
     */
    private static Optional<MicrophonePulse> getPulse(final InstrumentPlayedEvent<?> event) {
        if (!event.level().isClientSide || !MicrophoneStandScreen.INSTRUMENT_ID.equals(event.soundMeta().instrumentId()))
            return Optional.empty();

        return (event.level().getBlockEntity(event.soundMeta().pos()) instanceof MicrophoneStandBlockEntity stand)
            ? Optional.of(stand.getMicPulse())
            : Optional.empty();
    }
}
