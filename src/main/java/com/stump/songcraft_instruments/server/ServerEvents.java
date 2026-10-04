package com.stump.songcraft_instruments.server;

import com.stump.songcraft_instruments.block.blockentity.looper.LooperConnections;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.util.LooperRecordStateUtil;
import com.stump.songcraft_instruments.event.InstrumentOpenStateChangedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public class ServerEvents {

    @SubscribeEvent
    public static void onInstrumentClosedStateClosed(final InstrumentOpenStateChangedEvent event) {
        if (event.player.level().isClientSide)
            return;

        if (!event.isOpen) {
            LooperRecordStateUtil.handle((ServerPlayer) event.player, event.hand, false, true);

            // A group recording ends once every participant has had their instrument closed for a few seconds
            LooperConnections.getConnectedLooper(event.player)
                .ifPresent((lbe) -> lbe.session().onParticipantLeft(event.player.getUUID()));
        }
    }

}
