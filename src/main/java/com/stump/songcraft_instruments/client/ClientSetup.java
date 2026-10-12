package com.stump.songcraft_instruments.client;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.ModBlockEntities;
import com.stump.songcraft_instruments.render.DrumsetRenderer;
import com.stump.songcraft_instruments.render.KeyboardRenderer;
import com.stump.songcraft_instruments.render.KeyboardStandRenderer;
import com.stump.songcraft_instruments.render.KotoRenderer;
import com.stump.songcraft_instruments.render.MicrophoneStandRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SCInstrumentMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        BlockEntityRenderers.register(ModBlockEntities.DRUMSET.get(), DrumsetRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.KEYBOARD.get(), KeyboardRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.KEYBOARD_STAND.get(), KeyboardStandRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.MICROPHONE_STAND.get(), MicrophoneStandRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.KOTO.get(), KotoRenderer::new);
    }
}