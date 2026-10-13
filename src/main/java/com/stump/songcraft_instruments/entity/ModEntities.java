package com.stump.songcraft_instruments.entity;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.entity.vehicle.LooperBoat;
import com.stump.songcraft_instruments.entity.vehicle.LooperMinecart;
import com.stump.songcraft_instruments.entity.vehicle.SpeakerBoat;
import com.stump.songcraft_instruments.entity.vehicle.SpeakerMinecart;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SCInstrumentMod.MODID);
    public static void register(final IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }

    // Sized and tracked like the vanilla boats and minecarts
    public static final RegistryObject<EntityType<LooperBoat>> LOOPER_BOAT = ENTITY_TYPES.register("looper_boat", () ->
        EntityType.Builder.<LooperBoat>of(LooperBoat::new, MobCategory.MISC)
            .sized(1.375F, 0.5625F).clientTrackingRange(10)
            .build("looper_boat")
    );
    public static final RegistryObject<EntityType<SpeakerBoat>> SPEAKER_BOAT = ENTITY_TYPES.register("speaker_boat", () ->
        EntityType.Builder.<SpeakerBoat>of(SpeakerBoat::new, MobCategory.MISC)
            .sized(1.375F, 0.5625F).clientTrackingRange(10)
            .build("speaker_boat")
    );
    public static final RegistryObject<EntityType<LooperMinecart>> LOOPER_MINECART = ENTITY_TYPES.register("looper_minecart", () ->
        EntityType.Builder.<LooperMinecart>of(LooperMinecart::new, MobCategory.MISC)
            .sized(0.98F, 0.7F).clientTrackingRange(8)
            .build("looper_minecart")
    );
    public static final RegistryObject<EntityType<SpeakerMinecart>> SPEAKER_MINECART = ENTITY_TYPES.register("speaker_minecart", () ->
        EntityType.Builder.<SpeakerMinecart>of(SpeakerMinecart::new, MobCategory.MISC)
            .sized(0.98F, 0.7F).clientTrackingRange(8)
            .build("speaker_minecart")
    );
}
