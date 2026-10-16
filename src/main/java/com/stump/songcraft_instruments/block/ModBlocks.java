package com.stump.songcraft_instruments.block;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.pedal.PedalEffectType;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SCInstrumentMod.MODID);

    public static void register(final IEventBus bus) {
        BLOCKS.register(bus);
    }

    // //NOTE: For testing purposes
    // public static final RegistryObject<Block> LYRE_BLOCK = BLOCKS.register("lyre_block", () ->
    //     new LyreInstrumentBlock(Properties.copy(Blocks.OAK_WOOD))
    // );

    public static final RegistryObject<Block>
        KEYBOARD = BLOCKS.register("keyboard", () -> new KeyboardBlock(
        BlockBehaviour.Properties.of().noOcclusion().strength(.3f)
        )),
        KEYBOARD_STAND = BLOCKS.register("keyboard_stand", () -> new KeyboardStandBlock(
                BlockBehaviour.Properties.of().noOcclusion().strength(.3f)
        )),

        KOTO = BLOCKS.register("koto", () -> new KotoBlock(
                BlockBehaviour.Properties.of().noOcclusion().strength(.3f).sound(SoundType.WOOD)
        )),

        MICROPHONE_STAND = BLOCKS.register("microphone_stand", () -> new MicrophoneStandBlock(
                BlockBehaviour.Properties.of().noOcclusion().strength(.3f).sound(SoundType.WOOD)
        )),

        DRUMSET = BLOCKS.register("drumset", () -> new DrumsetBlock(
                    BlockBehaviour.Properties.of().noOcclusion().strength(0.3f).sound(SoundType.WOOD)
        )),

        LOOPER = BLOCKS.register("looper", () -> new LooperBlock(BlockBehaviour.Properties.copy(Blocks.NOTE_BLOCK))),

        SPEAKER = BLOCKS.register("speaker", () -> new SpeakerBlock(BlockBehaviour.Properties.copy(Blocks.NOTE_BLOCK))),

        FUZZ_PEDAL = BLOCKS.register("fuzz_pedal", () -> new PedalBlock(PedalEffectType.FUZZ,
            BlockBehaviour.Properties.of().noOcclusion().strength(0.8f).sound(SoundType.METAL)
        )),
        DELAY_PEDAL = BLOCKS.register("delay_pedal", () -> new PedalBlock(PedalEffectType.DELAY,
            BlockBehaviour.Properties.of().noOcclusion().strength(0.8f).sound(SoundType.METAL)
        )),
        BITCRUSHER_PEDAL = BLOCKS.register("bitcrusher_pedal", () -> new PedalBlock(PedalEffectType.BITCRUSH,
            BlockBehaviour.Properties.of().noOcclusion().strength(0.8f).sound(SoundType.METAL)
        )),
        TREMOLO_PEDAL = BLOCKS.register("tremolo_pedal", () -> new PedalBlock(PedalEffectType.TREMOLO,
            BlockBehaviour.Properties.of().noOcclusion().strength(0.8f).sound(SoundType.METAL)
        )),
        VIBRATO_PEDAL = BLOCKS.register("vibrato_pedal", () -> new PedalBlock(PedalEffectType.VIBRATO,
            BlockBehaviour.Properties.of().noOcclusion().strength(0.8f).sound(SoundType.METAL)
        )),
        OCTAVE_PEDAL = BLOCKS.register("octave_pedal", () -> new PedalBlock(PedalEffectType.OCTAVE,
            BlockBehaviour.Properties.of().noOcclusion().strength(0.8f).sound(SoundType.METAL)
        )),
        EQ_PEDAL = BLOCKS.register("eq_pedal", () -> new PedalBlock(PedalEffectType.EQ,
            BlockBehaviour.Properties.of().noOcclusion().strength(0.8f).sound(SoundType.METAL)
        )),
        REVERB_PEDAL = BLOCKS.register("reverb_pedal", () -> new PedalBlock(PedalEffectType.REVERB,
            BlockBehaviour.Properties.of().noOcclusion().strength(0.8f).sound(SoundType.METAL)
        )),
        LOFI_PEDAL = BLOCKS.register("lofi_pedal", () -> new PedalBlock(PedalEffectType.LOFI,
            BlockBehaviour.Properties.of().noOcclusion().strength(0.8f).sound(SoundType.METAL)
        ))
        ;
}
