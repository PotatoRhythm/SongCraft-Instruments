package com.stump.songcraft_instruments.gamerule;

import com.stump.songcraft_instruments.SCInstrumentMod;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.Category;

public abstract class ModGameRules {

    public static void load() {}

    public static final GameRules.Key<GameRules.IntegerValue>
        RULE_LOOPER_MAX_NOTES = GameRules.register(SCInstrumentMod.MODID+"_looperMaxNotes", Category.MISC, GameRules.IntegerValue.create(1_000_000))
    ;
    
}
