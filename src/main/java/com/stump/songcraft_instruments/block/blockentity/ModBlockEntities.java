package com.stump.songcraft_instruments.block.blockentity;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SCInstrumentMod.MODID);
    public static void register(final IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }


    public static final RegistryObject<BlockEntityType<LooperBlockEntity>> LOOPER = BLOCK_ENTITIES.register("looper", () ->
        BlockEntityType.Builder.of(
            LooperBlockEntity::new, ModBlocks.LOOPER.get()
        ).build(null)
    );

    public static final RegistryObject<BlockEntityType<SpeakerBlockEntity>> SPEAKER = BLOCK_ENTITIES.register("speaker", () ->
        BlockEntityType.Builder.of(
            SpeakerBlockEntity::new, ModBlocks.SPEAKER.get()
        ).build(null)
    );

    public static final RegistryObject<BlockEntityType<PedalBlockEntity>> PEDAL = BLOCK_ENTITIES.register("pedal", () ->
        BlockEntityType.Builder.of(
            PedalBlockEntity::new, ModBlocks.FUZZ_PEDAL.get(), ModBlocks.DELAY_PEDAL.get(),
            ModBlocks.BITCRUSHER_PEDAL.get(), ModBlocks.TREMOLO_PEDAL.get(), ModBlocks.VIBRATO_PEDAL.get(),
            ModBlocks.OCTAVE_PEDAL.get(), ModBlocks.EQ_PEDAL.get(), ModBlocks.REVERB_PEDAL.get(),
            ModBlocks.LOFI_PEDAL.get()
        ).build(null)
    );
    
    // hmm
    public static final RegistryObject<BlockEntityType<ModInstrumentBlockEntity>> INSTRUMENT =
        BLOCK_ENTITIES.register(SCInstrumentMod.MODID + "_instrument", () ->
                BlockEntityType.Builder.of(ModInstrumentBlockEntity::new,
                        ModBlocks.MICROPHONE_STAND.get() ).build(null)
        );

    public static final RegistryObject<BlockEntityType<KeyboardStandBlockEntity>> KEYBOARD_STAND =
            BLOCK_ENTITIES.register("keyboard_stand", () ->
                    BlockEntityType.Builder.of(KeyboardStandBlockEntity::new,
                            ModBlocks.KEYBOARD_STAND.get()).build(null)
            );

    public static final RegistryObject<BlockEntityType<DrumsetBlockEntity>> DRUMSET =
            BLOCK_ENTITIES.register("drumset",
                    () -> BlockEntityType.Builder.of(DrumsetBlockEntity::new,
                            ModBlocks.DRUMSET.get()).build(null));

    public static final RegistryObject<BlockEntityType<KeyboardBlockEntity>> KEYBOARD =
            BLOCK_ENTITIES.register("keyboard",
                    () -> BlockEntityType.Builder.of(KeyboardBlockEntity::new,
                            ModBlocks.KEYBOARD.get()).build(null));

    public static final RegistryObject<BlockEntityType<KotoBlockEntity>> KOTO =
            BLOCK_ENTITIES.register("koto",
                    () -> BlockEntityType.Builder.of(KotoBlockEntity::new,
                            ModBlocks.KOTO.get()).build(null));

    public static final RegistryObject<BlockEntityType<MicrophoneStandBlockEntity>> MICROPHONE_STAND =
            BLOCK_ENTITIES.register("microphone_stand",
                    () -> BlockEntityType.Builder.of(MicrophoneStandBlockEntity::new,
                            ModBlocks.MICROPHONE_STAND.get()).build(null));

    public static final RegistryObject<BlockEntityType<DummyBlockEntity>> DUMMY =
            BLOCK_ENTITIES.register("dummy",
                    () -> BlockEntityType.Builder.of(
                            DummyBlockEntity::new,
                            ModBlocks.DRUMSET.get(),
                            ModBlocks.KEYBOARD.get(),
                            ModBlocks.KOTO.get()
                    ).build(null));
}