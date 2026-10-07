package com.stump.songcraft_instruments;

import com.stump.songcraft_instruments.item.ModItems;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.buttonidentifier.*;
import com.stump.songcraft_instruments.particle.ModParticles;
import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.util.CommonUtil;
import com.stump.songcraft_instruments.block.ModBlocks;
import com.stump.songcraft_instruments.block.blockentity.ModBlockEntities;
import com.stump.songcraft_instruments.criteria.ModCriteria;
import com.stump.songcraft_instruments.item.crafting.ModRecipeSerializers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.GeckoLib;

/**
 * The main class of the SongCraft Instruments mod
 * 
 * Original mod author: tavWasPlayZ
 * Lunar Melodies fork by: Stump
 */
@Mod(SCInstrumentMod.MODID)
public class SCInstrumentMod
{
    public static final String MODID = "songcraft_instruments";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static CompoundTag modTag(final ItemStack item) {
        return item.getOrCreateTagElement(MODID);
    }
    public static CompoundTag modTag(final BlockEntity be) {
        return CommonUtil.getOrCreateElementTag(be.getPersistentData(), MODID);
    }

    public SCInstrumentMod()
    {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        GeckoLib.initialize();

        SCPacketHandler.registerPackets();
        NoteButtonIdentifiers.register(
            NoteGridButtonIdentifier.class,
            GloriousDrumNoteIdentifier.class,
            DjemDjemDrumNoteIdentifier.class,
            DrumsetNoteIdentifier.class
        );

        ModCriteria.load();

        ModItems.register(bus);
        ModBlocks.register(bus);
        ModBlockEntities.register(bus);
        ModRecipeSerializers.register(bus);
        ModParticles.register(bus);

        SCSounds.register(bus);
        SCCreativeModeTabs.regsiter(bus);

        MinecraftForge.EVENT_BUS.register(this);
    }
}
