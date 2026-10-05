package com.stump.songcraft_instruments.item;

import com.stump.songcraft_instruments.SCInstrumentMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {

    public static final TagKey<Item> GUILD_WARS_INSTRUMENTS =
            ItemTags.create(
                    new ResourceLocation(
                            SCInstrumentMod.MODID,
                            "guild_wars/instruments"
                    )
            );

    public static final TagKey<Item> GENSHIN_INSTRUMENTS =
            ItemTags.create(
                    new ResourceLocation(
                            SCInstrumentMod.MODID,
                            "genshin/instruments"
                    )
            );

    private ModItemTags() {}
}