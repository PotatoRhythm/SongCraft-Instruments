package com.stump.songcraft_instruments.client;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.gui.instrument.InstrumentScreenRegistry;
import com.stump.songcraft_instruments.client.gui.instrument.bassguitar.BassGuitarScreen;
import com.stump.songcraft_instruments.client.gui.instrument.djemdjemdrum.DjemDjemDrumScreen;
import com.stump.songcraft_instruments.client.gui.instrument.drumset.DrumsetScreen;
import com.stump.songcraft_instruments.client.gui.instrument.electricguitar.ElectricGuitarScreen;
import com.stump.songcraft_instruments.client.gui.instrument.floralzither.FloralZitherScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gloriousdrum.AratakisGreatAndGloriousDrumScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_bass.Gw2BassScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_bell.Gw2BellScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_drumset.Gw2DrumsetScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_frame_drum.Gw2FrameDrumScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_flute.Gw2FluteScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_harp.Gw2HarpScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_horn.Gw2HornScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_lute.Gw2LuteScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_minstrel.Gw2MinstrelScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_organ.Gw2OrganScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_pell.Gw2PellScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_piano.Gw2PianoScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_quaggan_organ.Gw2QuagganOrganScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_verdarach.Gw2VerdarachScreen;
import com.stump.songcraft_instruments.client.gui.instrument.microphone.MicrophoneScreen;
import com.stump.songcraft_instruments.client.gui.instrument.microphone.MicrophoneStandScreen;
import com.stump.songcraft_instruments.client.gui.instrument.nightwind_horn.NightwindHornScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.saxophone.SaxophoneScreen;
import com.stump.songcraft_instruments.client.gui.instrument.trombone.TromboneScreen;
import com.stump.songcraft_instruments.client.gui.instrument.ukelele.UkuleleScreen;
import com.stump.songcraft_instruments.client.gui.instrument.vintagelyre.VintageLyreScreen;
import com.stump.songcraft_instruments.client.gui.instrument.windsonglyre.WindsongLyreScreen;
import com.stump.songcraft_instruments.item.clientExtensions.ModItemPredicates;
import com.stump.songcraft_instruments.client.gui.instrument.guitar.GuitarScreen;
import com.stump.songcraft_instruments.client.gui.instrument.keyboard.KeyboardScreen;
import com.stump.songcraft_instruments.client.gui.instrument.koto.KotoScreen;
import com.stump.songcraft_instruments.client.gui.instrument.harp.HarpScreen;
import com.stump.songcraft_instruments.client.gui.instrument.doublebass.DoubleBassScreen;
import com.stump.songcraft_instruments.client.gui.instrument.cello.CelloScreen;
import com.stump.songcraft_instruments.client.gui.instrument.steeldrum.SteelDrumScreen;
import com.stump.songcraft_instruments.client.gui.instrument.concertina.ConcertinaScreen;
import com.stump.songcraft_instruments.client.gui.instrument.flute.FluteScreen;
import com.stump.songcraft_instruments.client.gui.instrument.xiao.XiaoScreen;
import com.stump.songcraft_instruments.client.gui.instrument.drum.DrumScreen;
import com.stump.songcraft_instruments.client.gui.instrument.kalimba.KalimbaScreen;
import com.stump.songcraft_instruments.client.gui.instrument.ocarina.OcarinaScreen;
import com.stump.songcraft_instruments.client.gui.instrument.animalcall.AnimalCallScreen;
import com.stump.songcraft_instruments.client.gui.instrument.synth.SynthScreen;
import com.stump.songcraft_instruments.client.gui.instrument.bell.BellScreen;
import com.stump.songcraft_instruments.client.gui.instrument.noteblockinstrument.NoteBlockInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.pipa.PipaScreen;
import com.stump.songcraft_instruments.client.gui.instrument.shamisen.ShamisenScreen;
import com.stump.songcraft_instruments.client.gui.instrument.violin.ViolinScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.SeparateTransformsModel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Map;
import java.util.function.Supplier;

@EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD, modid = SCInstrumentMod.MODID)
public class ClientInitiator {

    private static final Map<ResourceLocation, Supplier<? extends InstrumentScreen>> INSTRUMENTS =
        Map.ofEntries(
                Map.entry(WindsongLyreScreen.INSTRUMENT_ID, WindsongLyreScreen::new),
                Map.entry(VintageLyreScreen.INSTRUMENT_ID, VintageLyreScreen::new),
                Map.entry(FloralZitherScreen.INSTRUMENT_ID, FloralZitherScreen::new),
                Map.entry(AratakisGreatAndGloriousDrumScreen.INSTRUMENT_ID, AratakisGreatAndGloriousDrumScreen::new),
                Map.entry(NightwindHornScreen.INSTRUMENT_ID, NightwindHornScreen::new),

                Map.entry(UkuleleScreen.INSTRUMENT_ID, UkuleleScreen::new),
                Map.entry(DjemDjemDrumScreen.INSTRUMENT_ID, DjemDjemDrumScreen::new),

                Map.entry(KeyboardScreen.INSTRUMENT_ID, KeyboardScreen::new),
                Map.entry(DrumsetScreen.INSTRUMENT_ID, DrumsetScreen::new),
                Map.entry(ViolinScreen.INSTRUMENT_ID, ViolinScreen::new),
                Map.entry(TromboneScreen.INSTRUMENT_ID, TromboneScreen::new),
                Map.entry(GuitarScreen.INSTRUMENT_ID, GuitarScreen::new),
                Map.entry(BassGuitarScreen.INSTRUMENT_ID, BassGuitarScreen::new),
                Map.entry(ElectricGuitarScreen.INSTRUMENT_ID, ElectricGuitarScreen::new),
                Map.entry(PipaScreen.INSTRUMENT_ID, PipaScreen::new),
                Map.entry(ShamisenScreen.INSTRUMENT_ID, ShamisenScreen::new),
                Map.entry(KotoScreen.INSTRUMENT_ID, KotoScreen::new),
                Map.entry(SaxophoneScreen.INSTRUMENT_ID, SaxophoneScreen::new),
                Map.entry(MicrophoneScreen.INSTRUMENT_ID, MicrophoneScreen::new),
                Map.entry(MicrophoneStandScreen.INSTRUMENT_ID, MicrophoneStandScreen::new),

                Map.entry(HarpScreen.INSTRUMENT_ID, HarpScreen::new),
                Map.entry(CelloScreen.INSTRUMENT_ID, CelloScreen::new),
                Map.entry(DoubleBassScreen.INSTRUMENT_ID, DoubleBassScreen::new),
                Map.entry(SteelDrumScreen.INSTRUMENT_ID, SteelDrumScreen::new),
                Map.entry(ConcertinaScreen.INSTRUMENT_ID, ConcertinaScreen::new),
                Map.entry(FluteScreen.INSTRUMENT_ID, FluteScreen::new),
                Map.entry(XiaoScreen.INSTRUMENT_ID, XiaoScreen::new),
                Map.entry(DrumScreen.INSTRUMENT_ID, DrumScreen::create),
                Map.entry(KalimbaScreen.INSTRUMENT_ID, KalimbaScreen::new),
                Map.entry(OcarinaScreen.INSTRUMENT_ID, OcarinaScreen::new),
                Map.entry(AnimalCallScreen.INSTRUMENT_ID, AnimalCallScreen::create),
                Map.entry(SynthScreen.INSTRUMENT_ID, SynthScreen::new),
                Map.entry(BellScreen.INSTRUMENT_ID, BellScreen::create),
                Map.entry(NoteBlockInstrumentScreen.INSTRUMENT_ID, NoteBlockInstrumentScreen::new),

                Map.entry(Gw2BassScreen.INSTRUMENT_ID, Gw2BassScreen::new),
                Map.entry(Gw2BellScreen.INSTRUMENT_ID, Gw2BellScreen::new),
                Map.entry(Gw2FluteScreen.INSTRUMENT_ID, Gw2FluteScreen::new),
                Map.entry(Gw2HarpScreen.INSTRUMENT_ID, Gw2HarpScreen::new),
                Map.entry(Gw2HornScreen.INSTRUMENT_ID, Gw2HornScreen::new),
                Map.entry(Gw2LuteScreen.INSTRUMENT_ID, Gw2LuteScreen::new),
                Map.entry(Gw2MinstrelScreen.INSTRUMENT_ID, Gw2MinstrelScreen::new),
                Map.entry(Gw2OrganScreen.INSTRUMENT_ID, Gw2OrganScreen::new),
                Map.entry(Gw2PellScreen.INSTRUMENT_ID, Gw2PellScreen::new),
                Map.entry(Gw2PianoScreen.INSTRUMENT_ID, Gw2PianoScreen::new),
                Map.entry(Gw2QuagganOrganScreen.INSTRUMENT_ID, Gw2QuagganOrganScreen::new),
                Map.entry(Gw2VerdarachScreen.INSTRUMENT_ID, Gw2VerdarachScreen::new),
                Map.entry(Gw2FrameDrumScreen.INSTRUMENT_ID, Gw2FrameDrumScreen::new),
                Map.entry(Gw2DrumsetScreen.INSTRUMENT_ID, Gw2DrumsetScreen::new)
        );

    @SubscribeEvent
    public static void initClient(final FMLClientSetupEvent event) {
        ModArmPose.load();
        ModItemPredicates.register();

        InstrumentScreenRegistry.register(INSTRUMENTS);
    }

    @SubscribeEvent
    public static void modelLoadEvent(final ModelEvent.RegisterGeometryLoaders event) {
        event.register("separate_transforms", SeparateTransformsModel.Loader.INSTANCE);
    }
}
