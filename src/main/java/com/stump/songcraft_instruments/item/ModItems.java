package com.stump.songcraft_instruments.item;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.SCCreativeModeTabs;
import com.stump.songcraft_instruments.block.ModBlocks;
import com.stump.songcraft_instruments.item.emirecord.BurnedRecordItem;
import com.stump.songcraft_instruments.item.emirecord.WritableRecordItem;
import com.stump.songcraft_instruments.item.partial.instrument.CreditableAccessoryInstrumentItem;
import com.stump.songcraft_instruments.item.partial.instrument.CreditableBlockInstrumentItem;
import com.stump.songcraft_instruments.item.partial.instrument.CreditableInstrumentItem;
import com.stump.songcraft_instruments.item.partial.instrument.CreditableWindInstrumentItem;
import com.stump.songcraft_instruments.networking.packet.instrument.util.InstrumentPacketUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.stump.songcraft_instruments.networking.packet.instrument.util.InstrumentPacketUtil.sendOpenPacket;

@SuppressWarnings("unused")
@EventBusSubscriber(modid = SCInstrumentMod.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SCInstrumentMod.MODID);
    public static void register(final IEventBus bus) {
        ITEMS.register(bus);
    }


    private static final LinkedHashMap<ResourceKey<CreativeModeTab>, ArrayList<RegistryObject<Item>>> CREATIVE_TABS_MAP = new LinkedHashMap<>();
    private static ArrayList<RegistryObject<Item>> getCreativeItems(final ResourceKey<CreativeModeTab> tabKey) {
        if (!CREATIVE_TABS_MAP.containsKey(tabKey))
            CREATIVE_TABS_MAP.put(tabKey, new ArrayList<>());
        return CREATIVE_TABS_MAP.get(tabKey);
    }

    @SuppressWarnings("unchecked")
    private static final ResourceKey<CreativeModeTab>[] DEFAULT_INSTRUMENTS_TABS = new ResourceKey[] {
        SCCreativeModeTabs.INSTRUMENTS_TAB.getKey()
    };
    @SuppressWarnings("unchecked")
    private static final ResourceKey<CreativeModeTab>[] DEFAULT_INSTRUMENT_BLOCK_TABS = new ResourceKey[] {
        SCCreativeModeTabs.INSTRUMENTS_TAB.getKey(),
        CreativeModeTabs.FUNCTIONAL_BLOCKS
    };


    public static final RegistryObject<Item>
        WINDSONG_LYRE = register("windsong_lyre", () ->
            new InstrumentItem(
                    (player) -> sendOpenPacket(player, loc("windsong_lyre"))
            )
        ),
        VINTAGE_LYRE = register("vintage_lyre", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("vintage_lyre"))
                )
        ),

        FLORAL_ZITHER = register("floral_zither", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("floral_zither"))
                )
        ),

        GLORIOUS_DRUM = register("glorious_drum", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("glorious_drum"))
                )
        ),

        NIGHTWIND_HORN = register("nightwind_horn", () ->
                new NightwindHornItem(
                        (player) -> sendOpenPacket(player, loc("nightwind_horn"))
                )
        ),

        UKULELE = register("ukulele", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("ukulele"))
                )
        ),

        DJEM_DJEM_DRUM = register("djem_djem_drum", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("djem_djem_drum"))
                )
        ),

        VIOLIN_BOW = register("violin_bow",
            () -> new InstrumentAccessoryItem(
                new Properties().stacksTo(1).durability(InstrumentAccessoryItem.MAX_DURABILITY)
            )
        ),
        VIOLIN = register("violin", ViolinItem::new, DEFAULT_INSTRUMENTS_TABS, VIOLIN_BOW),

        GUITAR = register("guitar", () -> new CreditableInstrumentItem(
            (player) -> InstrumentPacketUtil.sendOpenPacket(
                player, loc("guitar")
            ),
            "Philharmonia"
        )),
        PIPA = register("pipa", () -> new CreditableInstrumentItem(
            (player) -> InstrumentPacketUtil.sendOpenPacket(
                player, loc("pipa")
            ),
            "DSK Asian DreamZ"
        )),

        BACHI = register("bachi",
            () -> new InstrumentAccessoryItem(
                new Properties().stacksTo(1).durability(InstrumentAccessoryItem.MAX_DURABILITY)
            )
        ),
        SHAMISEN = register("shamisen",
            () -> new CreditableAccessoryInstrumentItem(
                (player) -> InstrumentPacketUtil.sendOpenPacket(
                    player, loc("shamisen")
                ),
                (InstrumentAccessoryItem) BACHI.get(),
                "Roland SC-88"
            ),
            DEFAULT_INSTRUMENTS_TABS,
            BACHI
        ),

        KOTO = register("koto", () ->
            new CreditableBlockInstrumentItem(
                ModBlocks.KOTO.get(), new Properties().stacksTo(1),
                "DSK Asian DreamZ"
            ),
            DEFAULT_INSTRUMENT_BLOCK_TABS
        ),

        MICROPHONE_STAND = register("microphone_stand",
                () ->new MicrophoneStandBlockItem(ModBlocks.MICROPHONE_STAND.get(), new Properties().stacksTo(1)),
                SCCreativeModeTabs.INSTRUMENTS_TAB.getKey()
        ),

        DRUMSET = register(
                "drumset",
                () -> new DrumsetBlockItem(ModBlocks.DRUMSET.get(), new Properties().stacksTo(1)),
                SCCreativeModeTabs.INSTRUMENTS_TAB.getKey()
        ),

        KEYBOARD = register(
                "keyboard",
                () -> new KeyboardBlockItem(ModBlocks.KEYBOARD.get(), new Properties().stacksTo(1)),
                SCCreativeModeTabs.INSTRUMENTS_TAB.getKey()
        ),

        MICROPHONE = register("microphone", () -> new MicrophoneInstrumentItem(
                (player) -> InstrumentPacketUtil.sendOpenPacket(
                        player, loc("microphone")
                )
        )),

        ELECTRIC_GUITAR = register("electric_guitar", () -> new MicrophoneInstrumentItem(
                (player) -> InstrumentPacketUtil.sendOpenPacket(
                        player, loc("electric_guitar")
                )
        )),

        BASS_GUITAR = register("bass_guitar", () -> new MicrophoneInstrumentItem(
                (player) -> InstrumentPacketUtil.sendOpenPacket(
                        player, loc("bass_guitar")
                )
        )),

        TROMBONE = register("trombone", () -> new CreditableWindInstrumentItem(
            (player) -> InstrumentPacketUtil.sendOpenPacket(
                player, loc("trombone")
            ),
            "Philharmonia"
        )),
        SAXOPHONE = register("saxophone", () -> new CreditableWindInstrumentItem(
            (player) -> InstrumentPacketUtil.sendOpenPacket(
                player, loc("saxophone")
            ),
            "Philharmonia"
        )),

        KEYBOARD_STAND = registerBlockItem(
                ModBlocks.KEYBOARD_STAND,
                "keyboard_stand",
                SCCreativeModeTabs.INSTRUMENTS_TAB.getKey()
        ),

        GW2_BASS = register("gw2_bass", () ->
                new InstrumentItem(
                            (player) -> sendOpenPacket(player, loc("gw2_bass"))
                )
        ),
        GW2_BELL = register("gw2_bell", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("gw2_bell"))
                )
        ),
        GW2_FLUTE = register("gw2_flute", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("gw2_flute"))
                )
        ),
        GW2_HARP = register("gw2_harp", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("gw2_harp"))
                )
        ),
        GW2_HORN = register("gw2_horn", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("gw2_horn"))
                )
        ),
        GW2_LUTE = register("gw2_lute", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("gw2_lute"))
                )
        ),
        GW2_MINSTREL = register("gw2_minstrel", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("gw2_minstrel"))
                )
        ),
        GW2_ORGAN = register("gw2_organ", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("gw2_organ"))
                )
        ),
        GW2_PELL = register("gw2_pell", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("gw2_pell"))
                )
        ),
        GW2_PIANO = register("gw2_piano", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("gw2_piano"))
                )
        ),
        GW2_QUAGGAN_ORGAN = register("gw2_quaggan_organ", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("gw2_quaggan_organ"))
                )
        ),
        GW2_VERDARACH = register("gw2_verdarach", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("gw2_verdarach"))
                )
        ),

        GW2_FRAME_DRUM = register("gw2_frame_drum", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("gw2_frame_drum"))
                )
        ),

        GW2_DRUMSET = register("gw2_drumset", () ->
                new InstrumentItem(
                        (player) -> sendOpenPacket(player, loc("gw2_drumset"))
                )
        ),

        LOOPER = registerBlockItem(
                ModBlocks.LOOPER,
                "looper",
            SCCreativeModeTabs.MUSIC_PRODUCTION_TAB.getKey(),
            CreativeModeTabs.FUNCTIONAL_BLOCKS,CreativeModeTabs.REDSTONE_BLOCKS
        ),
        
        SPEAKER = registerBlockItem(ModBlocks.SPEAKER, "speaker",
            SCCreativeModeTabs.MUSIC_PRODUCTION_TAB.getKey(), CreativeModeTabs.FUNCTIONAL_BLOCKS
        ),

        LOOPER_ADAPTER = register("looper_adapter",
            () -> new LooperAdapterItem(new Properties().stacksTo(1)),
            CreativeModeTabs.REDSTONE_BLOCKS, SCCreativeModeTabs.MUSIC_PRODUCTION_TAB.getKey()
        ),

        SPEAKER_CABLE = register("speaker_cable",
            () -> new SpeakerCableItem(new Properties().stacksTo(1)),
            CreativeModeTabs.REDSTONE_BLOCKS, SCCreativeModeTabs.MUSIC_PRODUCTION_TAB.getKey()
        ),

        RECORD_WRITABLE = register("record_writable", () -> new WritableRecordItem(new Properties()),
            CreativeModeTabs.TOOLS_AND_UTILITIES, SCCreativeModeTabs.MUSIC_PRODUCTION_TAB.getKey()
        ),
        RECORD_JOHNNY = register("record_johnny", () ->
            new BurnedRecordItem(
                new Properties().stacksTo(1).rarity(Rarity.RARE),
                new ResourceLocation(SCInstrumentMod.MODID, "johnny"),
                "Hänschen klein - Franz Wiedemann",
                null
            ),
            CreativeModeTabs.TOOLS_AND_UTILITIES, SCCreativeModeTabs.MUSIC_PRODUCTION_TAB.getKey()
        ),
        RECORD_SUPER_IDOL = register("record_super_idol", () ->
            new BurnedRecordItem(
                new Properties().stacksTo(1).rarity(Rarity.RARE),
                new ResourceLocation(SCInstrumentMod.MODID, "super_idol"),
                "Super Idol - De Xian Rong",
                "Saxophy"
            ),
            CreativeModeTabs.TOOLS_AND_UTILITIES, SCCreativeModeTabs.MUSIC_PRODUCTION_TAB.getKey()
        ),
        RECORD_OVEN_KID = register("record_oven_kid", () ->
            new BurnedRecordItem(
                new Properties().stacksTo(1).rarity(Rarity.RARE),
                new ResourceLocation(SCInstrumentMod.MODID, "oven_kid"),
                "Timmy Trumpet & Savage - Freaks",
                "StavWasPlayZ"
            ),
            CreativeModeTabs.TOOLS_AND_UTILITIES, SCCreativeModeTabs.MUSIC_PRODUCTION_TAB.getKey()
        ),
        RECORD_RICKROLL = register("record_rickroll", () ->
            new BurnedRecordItem(
                new Properties().stacksTo(1).rarity(Rarity.EPIC),
                new ResourceLocation(SCInstrumentMod.MODID, "rickroll"),
                null,
                "StavWasPlayZ",
                Component.translatable("item.songcraft_instruments.interesting_record")
            ),
            CreativeModeTabs.TOOLS_AND_UTILITIES, SCCreativeModeTabs.MUSIC_PRODUCTION_TAB.getKey()
        )
    ;

    public static final Map<NoteBlockInstrument, RegistryObject<Item>> NOTEBLOCK_INSTRUMENTS = initNoteBlockInstruments();

    public static HashMap<NoteBlockInstrument, RegistryObject<Item>> initNoteBlockInstruments() {
        final NoteBlockInstrument[] instruments = NoteBlockInstrument.values();
        final HashMap<NoteBlockInstrument, RegistryObject<Item>> result = new HashMap<>(instruments.length);

        for (final NoteBlockInstrument instrument : instruments) {
            if (!instrument.isTunable())
                continue;

            result.put(instrument,
                register(NoteBlockInstrumentItem.getId(instrument),
                    () -> new NoteBlockInstrumentItem(instrument)
                )
            );
        }
        
        return result;
    }


    private static ResourceLocation loc(final String path) {
        return new ResourceLocation(SCInstrumentMod.MODID, path);
    }


    // private static RegistryObject<Item> registerBlockItem(final RegistryObject<Block> block) {
    //     return registerBlockItem(block, DEFAULT_INSTRUMENT_BLOCK_TABS);
    // }
    @SafeVarargs
    private static RegistryObject<Item> registerBlockItem(
            RegistryObject<Block> block,
            String name,
            ResourceKey<CreativeModeTab>... tabs
    ) {
        return register(
                name,
                () -> new BlockItem(block.get(), new Properties()),
                tabs
        );
    }

    private static RegistryObject<Item> register(String name, Supplier<Item> supplier, ResourceKey<CreativeModeTab>[] tabs,
                                                 RegistryObject<Item> appearsBefore) {
        final RegistryObject<Item> item = ITEMS.register(name, supplier);

        for (final ResourceKey<CreativeModeTab> tabKey : tabs) {
            final ArrayList<RegistryObject<Item>> items = getCreativeItems(tabKey);
            if (items.contains(appearsBefore)) {
                items.add(items.indexOf(appearsBefore), item);
            } else {
                items.add(item);
            }
        }

        return item;
    }
    @SafeVarargs
    private static RegistryObject<Item> register(String name, Supplier<Item> supplier, ResourceKey<CreativeModeTab>... tabs) {
        final RegistryObject<Item> item = ITEMS.register(name, supplier);

        for (final ResourceKey<CreativeModeTab> tabKey: tabs) {
            getCreativeItems(tabKey).add(item);
        }

        return item;
    }
    private static RegistryObject<Item> register(String name, Supplier<Item> supplier) {
        return register(name, supplier, DEFAULT_INSTRUMENTS_TABS);
    }


    @SubscribeEvent
    public static void addCreative(final BuildCreativeModeTabContentsEvent event) {
        CREATIVE_TABS_MAP.keySet().forEach((tabKey) -> {
            if (!event.getTabKey().equals(tabKey))
                return;

            event.acceptAll(
                getCreativeItems(tabKey).stream()
                    .map((item) -> new ItemStack(item.get()))
                    .toList()
            );
        });
    }

}
