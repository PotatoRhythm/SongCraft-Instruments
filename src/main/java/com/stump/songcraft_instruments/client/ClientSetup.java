package com.stump.songcraft_instruments.client;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.ModBlockEntities;
import com.stump.songcraft_instruments.client.gui.LooperVehicleScreen;
import com.stump.songcraft_instruments.client.gui.PedalScreen;
import com.stump.songcraft_instruments.entity.ModEntities;
import com.stump.songcraft_instruments.menu.ModMenus;
import com.stump.songcraft_instruments.render.CarriedBlockBoatRenderer;
import com.stump.songcraft_instruments.render.DrumsetRenderer;
import com.stump.songcraft_instruments.render.KeyboardRenderer;
import com.stump.songcraft_instruments.render.KeyboardStandRenderer;
import com.stump.songcraft_instruments.render.KotoRenderer;
import com.stump.songcraft_instruments.render.LooperMinecartRenderer;
import com.stump.songcraft_instruments.render.LooperRenderer;
import com.stump.songcraft_instruments.render.MicrophoneStandRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
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
        BlockEntityRenderers.register(ModBlockEntities.LOOPER.get(), LooperRenderer::new);

        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.LOOPER_VEHICLE.get(), LooperVehicleScreen::new);
            MenuScreens.register(ModMenus.PEDAL.get(), PedalScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LOOPER_BOAT.get(), CarriedBlockBoatRenderer::new);
        event.registerEntityRenderer(ModEntities.SPEAKER_BOAT.get(), CarriedBlockBoatRenderer::new);
        event.registerEntityRenderer(ModEntities.LOOPER_MINECART.get(), LooperMinecartRenderer::new);
        event.registerEntityRenderer(ModEntities.SPEAKER_MINECART.get(), (context) -> new MinecartRenderer<>(context, ModelLayers.MINECART));
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        // The looper's moving parts, rendered by LooperRenderer
        LooperRenderer.MODELS.forEach(event::register);
    }
}