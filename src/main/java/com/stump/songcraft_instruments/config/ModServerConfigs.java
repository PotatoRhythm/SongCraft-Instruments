package com.stump.songcraft_instruments.config;

import com.stump.songcraft_instruments.SCInstrumentMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

/**
 * Per-world server configs, in {@code <world>/serverconfig/songcraft_instruments-server.toml}
 */
@EventBusSubscriber(bus = Bus.MOD, modid = SCInstrumentMod.MODID)
public class ModServerConfigs {
    public static final ForgeConfigSpec CONFIGS;

    public static final BooleanValue RECORD_IMPORT_OPERATORS_ONLY;
    public static final IntValue RECORD_IMPORT_DAILY_LIMIT_KB;

    static {
        final ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();

        configBuilder.push("records");

        RECORD_IMPORT_OPERATORS_ONLY = configBuilder
            .comment("Whether only operators may import record files with /screcord import. Exporting is always allowed")
            .define("import_operators_only", false);
        RECORD_IMPORT_DAILY_LIMIT_KB = configBuilder
            .comment("How much new recording data (in KB) a player may import in 24 hours. Records take about 2-3 bytes per note. Files already in the world don't count. Operators are exempt. 0 for no limit")
            .defineInRange("import_daily_limit_kb", 10240, 0, 1048576);

        configBuilder.pop();

        CONFIGS = configBuilder.build();
    }


    @SubscribeEvent
    public static void registerConfigs(final FMLConstructModEvent event) {
        ModLoadingContext.get().registerConfig(Type.SERVER, CONFIGS);
    }
}
