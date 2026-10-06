package com.stump.songcraft_instruments.server.command;

import com.stump.songcraft_instruments.SCInstrumentMod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void onCommandsRegister(final RegisterCommandsEvent event) {
        ScRecordCommand.register(event.getDispatcher());
    }

}
