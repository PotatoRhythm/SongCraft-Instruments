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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Credits the artists of item models and sprites in the item's hover text, right under its name
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public class ArtCredits {
    private static final int SOLFIST_RED = 0xFF5555;
    private static final int APOLLO_PINK = 0xFFB3D9;
    /** For artists whose colour preference isn't known */
    private static final int WHITE = 0xFFFFFF;

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

    private static Map<Item, List<Credit>> credits;

    private static Map<Item, List<Credit>> credits() {
        // Items only exist after registration, so the map is built on first use
        if (credits == null) {
            credits = new HashMap<>();
            credit(ModItems.GW2_BASS.get(), ArtType.MODEL, "Solfist", SOLFIST_RED);
            credit(ModItems.MICROPHONE.get(), ArtType.MODEL, "Apollo", APOLLO_PINK);
            credit(ModItems.MICROPHONE_STAND.get(), ArtType.MODEL, "Apollo", APOLLO_PINK);
            credit(ModItems.DRUMSET.get(), ArtType.MODEL, "Apollo", APOLLO_PINK);

            // From Genshin Instruments and Even More Instruments!
            for (Item item : List.of(ModItems.WINDSONG_LYRE.get(), ModItems.VINTAGE_LYRE.get(),
                    ModItems.FLORAL_ZITHER.get(), ModItems.GLORIOUS_DRUM.get(), ModItems.NIGHTWIND_HORN.get(),
                    ModItems.UKULELE.get(), ModItems.DJEM_DJEM_DRUM.get(), ModItems.VIOLIN.get(),
                    ModItems.VIOLIN_BOW.get(), ModItems.GUITAR.get(), ModItems.PIPA.get(), ModItems.BACHI.get(),
                    ModItems.SHAMISEN.get(), ModItems.NOTE_BLOCK_INSTRUMENT.get()))
                credit(item, ArtType.SPRITE, "Gen", WHITE);
            for (Item item : List.of(ModItems.NIGHTWIND_HORN.get(), ModItems.KEYBOARD.get(),
                    ModItems.KEYBOARD_STAND.get(), ModItems.KOTO.get(), ModItems.TROMBONE.get(),
                    ModItems.SAXOPHONE.get()))
                credit(item, ArtType.MODEL, "StavWasPlayZ", WHITE);
            for (Item item : List.of(ModItems.LOOPER_ADAPTER.get(), ModItems.RECORD_WRITABLE.get()))
                credit(item, ArtType.SPRITE, "StavWasPlayZ", WHITE);
        }
        return credits;
    }

    private static void credit(Item item, ArtType type, String artist, int color) {
        credits.computeIfAbsent(item, (key) -> new ArrayList<>()).add(new Credit(type, artist, color));
    }

    @SubscribeEvent
    public static void onItemTooltip(final ItemTooltipEvent event) {
        final List<Credit> itemCredits = credits().get(event.getItemStack().getItem());
        if (itemCredits == null)
            return;

        // Index 0 is the item's name
        final int index = Math.min(1, event.getToolTip().size());
        for (int i = 0; i < itemCredits.size(); i++)
            event.getToolTip().add(index + i, itemCredits.get(i).toComponent());
    }
}
