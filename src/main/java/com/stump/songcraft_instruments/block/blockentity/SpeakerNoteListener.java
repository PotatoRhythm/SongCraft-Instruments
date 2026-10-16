package com.stump.songcraft_instruments.block.blockentity;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.speaker.Speaker;
import com.stump.songcraft_instruments.event.HeldNoteSoundPlayedEvent;
import com.stump.songcraft_instruments.event.InstrumentPlayedEvent;
import com.stump.songcraft_instruments.event.NoteSoundPlayedEvent;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import com.stump.songcraft_instruments.util.SpeakerUtil;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.List;

/**
 * Listens to instrument played events and immediately relays them
 * through every speaker paired to the playing instrument, through the instrument's pedals.
 */
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public class SpeakerNoteListener {

    @SubscribeEvent
    public static void onNoteSoundPlayed(final NoteSoundPlayedEvent event) {
        if (!event.isByPlayer() || event.level().isClientSide)
            return;
        final InitiatorID source = InitiatorID.fromEntity(event.entityInfo().get().entity);
        getMatchingSpeakers(event).forEach((speakerBE) ->
            speakerBE.playNote(event.sound(), event.soundMeta(), source, event.pedalChain())
        );
    }

    @SubscribeEvent
    public static void onHeldNoteSoundPlayed(final HeldNoteSoundPlayedEvent event) {
        getMatchingSpeakers(event).forEach((speakerBE) ->
            speakerBE.playHeldNote(event.sound(), event.soundMeta(), event.phase, event.initiatorID, event.pedalChain())
        );
    }


    /**
     * return The speakers paired to the instrument that produced the provided event.
     * Only matches player-initiated events, this both ties speaker playback to
     * an actual player performance, and prevents a speaker's own relayed sound
     * (which is not player-initiated) from re-triggering itself.
     */
    private static List<Speaker> getMatchingSpeakers(final InstrumentPlayedEvent<?> event) {
        if (!event.isByPlayer() || event.level().isClientSide)
            return List.of();

        return SpeakerUtil.getFromEvent(event);
    }
}
