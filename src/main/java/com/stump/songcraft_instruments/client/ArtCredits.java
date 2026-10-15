package com.stump.songcraft_instruments.client;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.HashMap;
import java.util.Map;

/**
 * Credits the artists of item models and sprites in the item's hover text, right under its name
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public class ArtCredits {
    private static final int SOLFIST_RED = 0xFF5555;
    private static final int APOLLO_PINK = 0xFFB3D9;

    private enum ArtType {
        MODEL, SPRITE;

        String translationKey() {
            return "tooltip." + SCInstrumentMod.MODID + ".credit." + name().toLowerCase();
        }
    }

    private record Credit(ArtType type, String artist, int color) {
        Component toComponent() {
            final MutableComponent artistName = Component.literal(artist).withStyle((style) -> style.withColor(color));
            return Component.translatable(type.translationKey(), artistName).withStyle(ChatFormatting.GRAY);
        }
    }

    private static Map<Item, Credit> credits;

    private static Map<Item, Credit> credits() {
        // Items only exist after registration, so the map is built on first use
        if (credits == null) {
            credits = new HashMap<>();
            credits.put(ModItems.GW2_BASS.get(), new Credit(ArtType.MODEL, "Solfist", SOLFIST_RED));
            credits.put(ModItems.MICROPHONE.get(), new Credit(ArtType.MODEL, "Apollo", APOLLO_PINK));
            credits.put(ModItems.MICROPHONE_STAND.get(), new Credit(ArtType.MODEL, "Apollo", APOLLO_PINK));
            credits.put(ModItems.DRUMSET.get(), new Credit(ArtType.MODEL, "Apollo", APOLLO_PINK));
        }
        return credits;
    }

    @SubscribeEvent
    public static void onItemTooltip(final ItemTooltipEvent event) {
        final Credit credit = credits().get(event.getItemStack().getItem());
        if (credit == null)
            return;

        // Index 0 is the item's name
        event.getToolTip().add(Math.min(1, event.getToolTip().size()), credit.toComponent());
    }
}
