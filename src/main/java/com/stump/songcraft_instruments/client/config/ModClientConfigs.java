package com.stump.songcraft_instruments.client.config;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.enumType.*;
import com.stump.songcraft_instruments.client.gui.instrument.djemdjemdrum.DjemDjemDrumNoteLabel;
import com.stump.songcraft_instruments.client.gui.instrument.drumset.DrumsetNoteLabel;
import com.stump.songcraft_instruments.client.gui.instrument.gloriousdrum.DominantGloriousDrumType;
import com.stump.songcraft_instruments.client.gui.instrument.gloriousdrum.GloriousDrumNoteLabel;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_drumset.Gw2DrumsetNoteLabel;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_frame_drum.Gw2FrameDrumNoteLabel;
import com.stump.songcraft_instruments.client.gui.instrument.ukelele.Ukulele3rdOctaveType;
import com.stump.songcraft_instruments.client.gui.options.MidiOptionsScreen;
import com.stump.songcraft_instruments.client.util.ClientUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.MOD, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public class ModClientConfigs {
    //TODO: Prefix common configs the same

    public static final ForgeConfigSpec CONFIGS;

    public static final IntValue MIDI_DEVICE_INDEX, OCTAVE_SHIFT, MIDI_CHANNEL;
    public static final IntValue TRANSPOSE;
    public static final DoubleValue VOLUME, MIDI_IN_SENSITIVITY;

    public static final EnumValue<NoteGridLabel> GRID_LABEL_TYPE;
    public static final EnumValue<NoteIconStyle> NOTE_ICON_STYLE;
    public static final EnumValue<InstrumentChannelType> CHANNEL_TYPE;
    public static final EnumValue<ControlModeType> CONTROL_MODE;

    public static final BooleanValue
        STOP_MUSIC_ON_PLAY, SHARED_INSTRUMENT,
        RENDER_BACKGROUND, ACCEPTED_GENSHIN_CONSENT, ACCEPTED_GW2_CONSENT, ACCURATE_NOTES,
        MIDI_ENABLED, EXTEND_OCTAVES, FIXED_TOUCH, ACCEPT_ALL_CHANNELS,
        NORMALIZE_VINTAGE_LYRE, SERVER_AUDIO, EXTEND_RANGE
    ;

    public static final EnumValue<ZitherSoundType> ZITHER_SOUND_TYPE;
    public static final EnumValue<GloriousDrumNoteLabel> GLORIOUS_DRUM_LABEL_TYPE;
    public static final EnumValue<DjemDjemDrumNoteLabel> DJEM_DJEM_DRUM_LABEL_TYPE;
    public static final EnumValue<Gw2FrameDrumNoteLabel> GW2_FRAME_DRUM_LABEL_TYPE;
    public static final EnumValue<DrumsetNoteLabel> DRUMSET_LABEL_TYPE;
    public static final EnumValue<Gw2DrumsetNoteLabel> GW2_DRUMSET_LABEL_TYPE;
    public static final EnumValue<DominantGloriousDrumType> DOMINANT_DRUM_TYPE;
    public static final EnumValue<Ukulele3rdOctaveType> UKULELE_3RD_OCTAVE_TYPE;

    public static final EnumValue<PipaSoundType> PIPA_SOUND_TYPE;
    public static final EnumValue<KeyboardSoundType> KEYBOARD_SOUND_TYPE;
    public static final EnumValue<DrumsetSoundType> DRUMSET_SOUND_TYPE;
    public static final EnumValue<BassGuitarSoundType> BASS_GUITAR_SOUND_TYPE;
    public static final EnumValue<ElectricGuitarSoundType> ELECTRIC_GUITAR_SOUND_TYPE;
    public static final EnumValue<ViolinSoundType> VIOLIN_SOUND_TYPE;
    public static final EnumValue<MicrophoneSoundType> MICROPHONE_SOUND_TYPE;
    public static final EnumValue<SaxophoneSoundType> SAXOPHONE_SOUND_TYPE;
    public static final EnumValue<TromboneSoundType> TROMBONE_SOUND_TYPE;

    static {
        final ForgeConfigSpec.Builder configBuilder = new Builder();

        VOLUME = configBuilder.defineInRange("instrument_volume",
            0.5, 0, 1
        );

        TRANSPOSE = configBuilder.defineInRange("transpose",
                0, -12, 12
        );

        GRID_LABEL_TYPE = configBuilder.defineEnum("label_type", NoteGridLabel.KEYBOARD_LAYOUT);
        NOTE_ICON_STYLE = configBuilder.comment("Selects the note symbols drawn on grid instruments. INSTRUMENT_DEFAULT uses each instrument's own style")
            .defineEnum("note_icon_style", NoteIconStyle.INSTRUMENT_DEFAULT);
        CHANNEL_TYPE = configBuilder.defineEnum("channel_type", InstrumentChannelType.MIXED);
        CONTROL_MODE = configBuilder.comment("Selects the instrument control mode").defineEnum("control_mode", ControlModeType.GENSHIN);

        STOP_MUSIC_ON_PLAY = configBuilder.comment(
            "Stops all background music when you or someone else within "+ ClientUtil.STOP_SOUND_DISTANCE+" blocks of range plays an instrument"
        ).define("stop_music_on_play", true);
        SHARED_INSTRUMENT = configBuilder.comment("Defines whether you will see others playing on your instrument's screen")
            .define("display_other_players", true);

        RENDER_BACKGROUND = configBuilder.define("render_background", true);
        ACCURATE_NOTES = configBuilder.define("accurate_notes", true);

        NORMALIZE_VINTAGE_LYRE = configBuilder.define("normalize_vintage_lyre", true);

        SERVER_AUDIO = configBuilder.define("server_audio", false);
        EXTEND_RANGE = configBuilder.comment("Extend Range to 5 octaves").define("extend_range", true);

        ACCEPTED_GENSHIN_CONSENT = configBuilder.define("accepted_genshin_consent", false);
        ACCEPTED_GW2_CONSENT = configBuilder.define("accepted_gw2_consent", false);

        ZITHER_SOUND_TYPE = configBuilder.defineEnum("zither_sound_type", ZitherSoundType.NEW);
        GLORIOUS_DRUM_LABEL_TYPE = configBuilder.defineEnum("glorious_drum_label_type", GloriousDrumNoteLabel.KEYBOARD_LAYOUT);
        DJEM_DJEM_DRUM_LABEL_TYPE = configBuilder.defineEnum("djem_djem_drum_label_type", DjemDjemDrumNoteLabel.KEYBOARD_LAYOUT);
        GW2_FRAME_DRUM_LABEL_TYPE = configBuilder.defineEnum("gw2_frame_drum_label_type", Gw2FrameDrumNoteLabel.KEYBOARD_LAYOUT);
        DRUMSET_LABEL_TYPE = configBuilder.defineEnum("drumset_label_type", DrumsetNoteLabel.KEYBOARD_LAYOUT);
        GW2_DRUMSET_LABEL_TYPE = configBuilder.defineEnum("gw2_drumset_label_type", Gw2DrumsetNoteLabel.KEYBOARD_LAYOUT);
        KEYBOARD_SOUND_TYPE = configBuilder.defineEnum("keyboard_sound_type", KeyboardSoundType.YAMAHA_C5);
        DRUMSET_SOUND_TYPE = configBuilder.defineEnum("drumset_sound_type", DrumsetSoundType.GW2);
        BASS_GUITAR_SOUND_TYPE = configBuilder.defineEnum("bass_guitar_sound_type", BassGuitarSoundType.FINGER);
        ELECTRIC_GUITAR_SOUND_TYPE = configBuilder.defineEnum("electric_guitar_sound_type", ElectricGuitarSoundType.CLEAN);
        VIOLIN_SOUND_TYPE = configBuilder.defineEnum("violin_sound_type", ViolinSoundType.FAST);
        PIPA_SOUND_TYPE = configBuilder.defineEnum("pipa_sound_type", PipaSoundType.REGULAR);
        MICROPHONE_SOUND_TYPE = configBuilder.defineEnum("microphone_sound_type", MicrophoneSoundType.MIKU);
        SAXOPHONE_SOUND_TYPE = configBuilder.defineEnum("saxophone_sound_type", SaxophoneSoundType.TENOR);
        TROMBONE_SOUND_TYPE = configBuilder.defineEnum("trombone_sound_type", TromboneSoundType.PHGM);


        MIDI_ENABLED = configBuilder.define("midi_enabled", false);
        MIDI_DEVICE_INDEX = configBuilder.defineInRange("midi_device_index", -1, -1, Integer.MAX_VALUE);
        MIDI_IN_SENSITIVITY = configBuilder.defineInRange("midi_in_sensitivity", .8, 0, 1);

        EXTEND_OCTAVES = configBuilder.comment(
            "When a note that is higher/lower than the usual octave range is played, will automatically adjust the pitch to match your playings. Can only extend up to 1 octave per side: high and low C."
        ).define("extend_octaves", true);
        FIXED_TOUCH = configBuilder.comment(
            "Defines whether the velocity of a note press will not affect the instrument's volume"
        ).define("fixed_touch", false);
        ACCEPT_ALL_CHANNELS = configBuilder.define("accept_all_channels", true);

        OCTAVE_SHIFT = configBuilder.defineInRange("midi_octave_shift",
            0, MidiOptionsScreen.MIN_OCTAVE_SHIFT, MidiOptionsScreen.MAX_OCTAVE_SHIFT
        );
        MIDI_CHANNEL = configBuilder.defineInRange("midi_channel",
            0, MidiOptionsScreen.MIN_MIDI_CHANNEL, MidiOptionsScreen.MAX_MIDI_CHANNEL
        );


        DOMINANT_DRUM_TYPE = configBuilder.comment(
            "Defines the MIDI split behaviour of the Arataki's Great and Glorious Drum"
        ).defineEnum("dominant_drum_type", DominantGloriousDrumType.BOTH);

        UKULELE_3RD_OCTAVE_TYPE = configBuilder.defineEnum("ukulele_3rd_octave_type", Ukulele3rdOctaveType.CHORDS);

        CONFIGS = configBuilder.build();
    }


    @SubscribeEvent
    public static void registerConfigs(final FMLConstructModEvent event) {
        ModLoadingContext.get().registerConfig(Type.CLIENT, CONFIGS, "songcraft_instruments_configs.toml");
    }
}
