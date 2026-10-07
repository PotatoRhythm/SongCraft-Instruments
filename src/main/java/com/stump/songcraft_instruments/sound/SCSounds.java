package com.stump.songcraft_instruments.sound;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentScreen;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import com.stump.songcraft_instruments.sound.registrar.HeldNoteSoundRegistrar;
import com.stump.songcraft_instruments.sound.registrar.NoteSoundRegistrar;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;

public class SCSounds {
    
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SCInstrumentMod.MODID);
    public static void register(final IEventBus bus) {
        SOUNDS.register(bus);
    }

    public static final NoteSound[]
        WINDSONG_LYRE_NOTE_SOUNDS = nsr(loc("windsong_lyre")).stereo().registerGrid(),
        VINTAGE_LYRE_NOTE_SOUNDS = nsr(loc("vintage_lyre")).registerGrid(),

        ZITHER_NEW_NOTE_SOUNDS = nsr(loc("floral_zither_new")).registerGrid(),
        ZITHER_OLD_NOTE_SOUNDS = nsr(loc("floral_zither_old")).registerGrid(),

        UKULELE = nsr(loc("ukulele")).registerGrid(),
        DJEM_DJEM_DRUM = nsr(loc("djem_djem_drum")).registerGrid(2, 4),

        KEYBOARD_GW2 = nsr(loc("keyboard_gw2")).stereo().registerGrid(),
        KEYBOARD_YAMAHA_C5 = nsr(loc("keyboard_yamaha_c5")).stereo().registerGrid(),
        HEARTOPIA = nsr(loc("keyboard_heartopia")).stereo().registerGrid(),
        KEYBOARD_ELECTRIC = nsr(loc("keyboard_electric")).stereo().registerGrid(),
        KEYBOARD_HARPSICHORD = nsr(loc("keyboard_harpsichord")).stereo().registerGrid(),

        // Heartopia instrument (the piano is HEARTOPIA above)
        HEARTOPIA_HARP = nsr(loc("heartopia_harp")).stereo().registerGrid(),
        HEARTOPIA_KALIMBA = nsr(loc("heartopia_kalimba")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        HEARTOPIA_LYRE = nsr(loc("heartopia_lyre")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        HEARTOPIA_HANG = nsr(loc("heartopia_hang")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        HEARTOPIA_ACOUSTIC_BASS = nsr(loc("heartopia_acoustic_bass")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        HEARTOPIA_LUNGHE = nsr(loc("heartopia_lunghe")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        HEARTOPIA_CAJON = nsr(loc("heartopia_cajon")).stereo().registerGrid(2, 4),
        HEARTOPIA_CONGA_BONGOS = nsr(loc("heartopia_conga_bongos")).stereo().registerGrid(2, 4),

        // Sky instrument
        SKY_PIANO = nsr(loc("sky_piano")).stereo().registerGrid(),
        SKY_GRAND_PIANO = nsr(loc("sky_grand_piano")).stereo().registerGrid(),
        SKY_WINTER_PIANO = nsr(loc("sky_winter_piano")).stereo().registerGrid(),
        SKY_GUITAR = nsr(loc("sky_guitar")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_TOY_UKULELE = nsr(loc("sky_toy_ukulele")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_PIPA = nsr(loc("sky_pipa")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_KALIMBA = nsr(loc("sky_kalimba")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_XYLOPHONE = nsr(loc("sky_xylophone")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_CONTRABASS = nsr(loc("sky_contrabass")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_BIRD_CALL = nsr(loc("sky_bird_call")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_CRAB_CALL = nsr(loc("sky_crab_call")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_FISH_CALL = nsr(loc("sky_fish_call")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_JELLY_CALL = nsr(loc("sky_jelly_call")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_MANTA_CALL = nsr(loc("sky_manta_call")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_SPIRIT_MANTA_CALL = nsr(loc("sky_spirit_manta_call")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_MOTH_CALL = nsr(loc("sky_moth_call")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_BELLS = nsr(loc("sky_bells")).stereo().registerGrid(2, 4),
        SKY_SMALL_BELL = nsr(loc("sky_small_bell")).stereo().registerGrid(2, 4),
        SKY_HANDPAN = nsr(loc("sky_handpan")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_TRIUMPH_HANDPAN = nsr(loc("sky_triumph_handpan")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_KRILL_HORN = nsr(loc("sky_krill_horn")).stereo().registerGrid(2, 4),
        SKY_DRUM = nsr(loc("sky_drum")).stereo().registerGrid(2, 4),
        SKY_DUNDUN = nsr(loc("sky_dundun")).stereo().registerGrid(2, 4),
        SKY_FORTUNE_DRUM = nsr(loc("sky_fortune_drum")).stereo().registerGrid(2, 2),
        SKY_CYMBALS = nsr(loc("sky_cymbals")).stereo().registerGrid(2, 2),
        SKY_BASS_SYNTH = nsr(loc("sky_bass_synth")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_CHIME_SYNTH = nsr(loc("sky_chime_synth")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        SKY_TR_909 = nsr(loc("sky_tr_909")).stereo().registerGrid(2, 4),
        SKY_DANCE = nsr(loc("sky_dance")).stereo().registerGrid(2, 3),

        FF14_HARP = nsr(loc("ff14_harp")).registerGrid(),
        FF14_PIANO = nsr(loc("ff14_piano")).registerGrid(),
        FF14_LUTE = nsr(loc("ff14_lute")).registerGrid(),
        FF14_FIDDLE = nsr(loc("ff14_fiddle")).registerGrid(),
        FF14_TIMPANI = nsr(loc("ff14_timpani")).registerGrid(),
        FF14_BONGO = nsr(loc("ff14_bongo")).registerGrid(2, 4),
        // The bass drum, snare drum and cymbal, a row each from the bottom
        FF14_MARCHING_DRUMS = nsr(loc("ff14_marching_drums")).registerGrid(),
        FF14_DRUM_KIT = nsr(loc("ff14_drum_kit")).registerGrid(2, 4),
        FF14_MUTED_GUITAR = nsr(loc("ff14_muted_guitar")).registerGrid(),
        FF14_SPECIAL_GUITAR = nsr(loc("ff14_special_guitar")).registerGrid(),

        WWM_1 = nsr(loc("wwm_1")).stereo().registerGrid(),
        WWM_2 = nsr(loc("wwm_2")).stereo().registerGrid(),
        WWM_3 = nsr(loc("wwm_3")).stereo().registerGrid(),
        WWM_6 = nsr(loc("wwm_6")).stereo().registerGrid(),
        WWM_7 = nsr(loc("wwm_7")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),


        GUITAR = nsr(loc("guitar")).registerGrid(),
        GUITAR_STEEL = nsr(loc("guitar_steel")).stereo().registerGrid(),

        GUITAR_CLEAN = nsr(loc("guitar_clean")).stereo().registerGrid(),
        GUITAR_HARMONICS = nsr(loc("guitar_harmonics")).stereo().registerGrid(),

        BASS_ACOUSTIC = nsr(loc("bass_acoustic")).stereo().registerGrid(),
        BASS_FINGER = nsr(loc("bass_finger")).stereo().registerGrid(),
        BASS_SLAP = nsr(loc("bass_slap")).stereo().registerGrid(),
        BASS_PICKED = nsr(loc("bass_picked")).stereo().registerGrid(),

        SHAMISEN = nsr(loc("shamisen")).stereo().registerGrid(),
        KOTO = nsr(loc("koto")).registerGrid(),

        PIPA_REGULAR = nsr(loc("pipa_regular")).registerGrid(),
        PIPA_TERMOLO = nsr(loc("pipa_tremolo")).registerGrid(),

        VIOLIN_PIZZICATO = nsr(loc("violin_pizzicato")).stereo().registerGrid(),

        GW2_BASS = nsr(loc("gw2_bass")).stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        GW2_BELL = nsr(loc("gw2_bell")).registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2),
        GW2_HARP = nsr(loc("gw2_harp")).stereo().registerGrid(),
        GW2_LUTE = nsr(loc("gw2_lute")).stereo().registerGrid(),
        GW2_MINSTREL = nsr(loc("gw2_minstrel")).stereo().registerGrid(),
        GW2_PELL = nsr(loc("gw2_pell")).stereo().registerGrid(),
        GW2_FRAME_DRUM = nsr(loc("gw2_frame_drum")).stereo().registerGrid(2, 5),

        GLORIOUS_DRUM = nsr(loc("glorious_drum"))
                .chain(loc("glorious_drum_don")).add()
                .chain(loc("glorious_drum_ka")).stereo().add()
                .registerAll(),

        DRUMSET_GW2 = nsr(loc("drumset_gw2"))
                .chain(loc("drumset_bass")).stereo().add()
                .chain(loc("drumset_snare")).stereo().add()
                .chain(loc("drumset_cross_stick")).stereo().add()
                .chain(loc("drumset_ghost")).stereo().add()
                .chain(loc("drumset_high_tom")).stereo().add()
                .chain(loc("drumset_low_tom")).stereo().add()

                .chain(loc("drumset_bass2")).stereo().add()
                .chain(loc("drumset_snare2")).stereo().add()
                .chain(loc("drumset_ghost2")).stereo().add()
                .chain(loc("drumset_mid_tom")).stereo().add()

                .chain(loc("drumset_crash_cymbal")).stereo().add()
                .chain(loc("drumset_ride_cymbal")).stereo().add()
                .chain(loc("drumset_hi-hat_closed")).stereo().add()
                .chain(loc("drumset_hi-hat_open")).stereo().add()
                .chain(loc("drumset_hi-hat_foot")).stereo().add()
            .registerAll(),

        DRUMSET_STANDARD = nsr(loc("drumset_standard"))
                .chain(loc("drumset_standard_bass")).stereo().add()
                .chain(loc("drumset_standard_snare")).stereo().add()
                .chain(loc("drumset_standard_cross_stick")).stereo().add()
                .chain(loc("drumset_standard_ghost")).stereo().add()
                .chain(loc("drumset_standard_high_tom")).stereo().add()
                .chain(loc("drumset_standard_low_tom")).stereo().add()

                .chain(loc("drumset_standard_bass2")).stereo().add()
                .chain(loc("drumset_standard_snare2")).stereo().add()
                .chain(loc("drumset_standard_ghost2")).stereo().add()
                .chain(loc("drumset_standard_mid_tom")).stereo().add()

                .chain(loc("drumset_standard_crash_cymbal")).stereo().add()
                .chain(loc("drumset_standard_ride_cymbal")).stereo().add()
                .chain(loc("drumset_standard_hi-hat_closed")).stereo().add()
                .chain(loc("drumset_standard_hi-hat_open")).stereo().add()
                .chain(loc("drumset_standard_hi-hat_foot")).stereo().add()
            .registerAll(),

        DRUMSET_POWER = nsr(loc("drumset_power"))
                .chain(loc("drumset_power_bass")).stereo().add()
                .chain(loc("drumset_power_snare")).stereo().add()
                .chain(loc("drumset_power_cross_stick")).stereo().add()
                .chain(loc("drumset_power_ghost")).stereo().add()
                .chain(loc("drumset_power_high_tom")).stereo().add()
                .chain(loc("drumset_power_low_tom")).stereo().add()

                .chain(loc("drumset_power_bass2")).stereo().add()
                .chain(loc("drumset_power_snare2")).stereo().add()
                .chain(loc("drumset_power_ghost2")).stereo().add()
                .chain(loc("drumset_power_mid_tom")).stereo().add()

                .chain(loc("drumset_power_crash_cymbal")).stereo().add()
                .chain(loc("drumset_power_ride_cymbal")).stereo().add()
                .chain(loc("drumset_power_hi-hat_closed")).stereo().add()
                .chain(loc("drumset_power_hi-hat_open")).stereo().add()
                .chain(loc("drumset_power_hi-hat_foot")).stereo().add()
            .registerAll(),

        DRUMSET_ORCHESTRAL = nsr(loc("drumset_orchestral"))
                .chain(loc("drumset_orchestral_bass")).stereo().add()
                .chain(loc("drumset_orchestral_snare")).stereo().add()
                .chain(loc("drumset_orchestral_cross_stick")).stereo().add()
                .chain(loc("drumset_orchestral_ghost")).stereo().add()
                .chain(loc("drumset_orchestral_high_tom")).stereo().add()
                .chain(loc("drumset_orchestral_low_tom")).stereo().add()

                .chain(loc("drumset_orchestral_bass2")).stereo().add()
                .chain(loc("drumset_orchestral_snare2")).stereo().add()
                .chain(loc("drumset_orchestral_ghost2")).stereo().add()
                .chain(loc("drumset_orchestral_mid_tom")).stereo().add()

                .chain(loc("drumset_orchestral_crash_cymbal")).stereo().add()
                .chain(loc("drumset_orchestral_ride_cymbal")).stereo().add()
                .chain(loc("drumset_orchestral_hi-hat_closed")).stereo().add()
                .chain(loc("drumset_orchestral_hi-hat_open")).stereo().add()
                .chain(loc("drumset_orchestral_hi-hat_foot")).stereo().add()
            .registerAll(),

        DRUMSET_COZY = nsr(loc("drumset_cozy"))
                .chain(loc("drumset_cozy_bass")).stereo().add()
                .chain(loc("drumset_cozy_snare")).stereo().add()
                .chain(loc("drumset_cozy_cross_stick")).stereo().add()
                .chain(loc("drumset_cozy_ghost")).stereo().add()
                .chain(loc("drumset_cozy_high_tom")).stereo().add()
                .chain(loc("drumset_cozy_low_tom")).stereo().add()

                .chain(loc("drumset_cozy_bass2")).stereo().add()
                .chain(loc("drumset_cozy_snare2")).stereo().add()
                .chain(loc("drumset_cozy_ghost2")).stereo().add()
                .chain(loc("drumset_cozy_mid_tom")).stereo().add()

                .chain(loc("drumset_cozy_crash_cymbal")).stereo().add()
                .chain(loc("drumset_cozy_ride_cymbal")).stereo().add()
                .chain(loc("drumset_cozy_hi-hat_closed")).stereo().add()
                .chain(loc("drumset_cozy_hi-hat_open")).stereo().add()
                .chain(loc("drumset_cozy_hi-hat_foot")).stereo().add()
            .registerAll()
    ;
    
    private static final float
        HOLD_DURATION = 3f,
        FADE_TIME = .25f
    ;

    public static final HeldNoteSound[]
        NIGHTWIND_HORN = hnsr(loc("nightwind_horn"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)

            // Test for release sound:
//            .releaseBuilder((builder) -> builder
//                .chain(SoundEvents.COW_DEATH.getLocation())
//                .alreadyRegistered()
//                .add(GridInstrumentScreen.DEF_COLUMNS * 2)
//                .registerAll()
//            )

            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),


        IRINA_BROCHIN = hnsr(loc("microphone_irina_brochin"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 8)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        BASS_CHOIR = hnsr(loc("microphone_bass_choir"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 8)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        NOT_MIKU = hnsr(loc("microphone_not_miku"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 6)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        NOT_TETO = hnsr(loc("microphone_not_teto"))
                .holdBuilder(SCSounds::threeOctaveSoundBuilder)
                .attackBuilder(SCSounds::threeOctaveSoundBuilder)
                .holdDelay(.03f)
                .chainedHoldDelay(-FADE_TIME * 2)
                .releaseFadeOut(FADE_TIME / 6)
                .fullHoldFadeoutTime(2)
                .decays(7)
                .register(HOLD_DURATION),
        NOT_TETO_SNEAKY = hnsr(loc("microphone_not_teto_sneaky"))
                .holdBuilder(SCSounds::threeOctaveSoundBuilder)
                .attackBuilder(SCSounds::threeOctaveSoundBuilder)
                .holdDelay(.03f)
                .chainedHoldDelay(-FADE_TIME * 2)
                .releaseFadeOut(FADE_TIME / 6)
                .fullHoldFadeoutTime(2)
                .decays(7)
                .register(HOLD_DURATION),
        SAXOPHONE_BARITONE = hnsr(loc("saxophone_baritone"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        SAXOPHONE_TENOR = hnsr(loc("saxophone_tenor"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        TRUMPET_WESTGATE_STUDIOS = hnsr(loc("trumpet_westgate_studios"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 8)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        TROMBONE_PHGM = hnsr(loc("trombone_phgm"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 8)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        GW2_HORN = hnsr(loc("gw2_horn"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(2.5f),
        GW2_FLUTE = hnsr(loc("gw2_flute"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        GW2_VERDARACH = hnsr(loc("gw2_verdarach"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(1f),
        GW2_ORGAN = hnsr(loc("gw2_organ"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(2.5f),
        GW2_QUAGGAN_ORGAN = hnsr(loc("gw2_quaggan_organ"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        GUITAR_DISTORTION = hnsr(loc("guitar_distortion"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            // About 1.5 s to fade out from full volume when released
            .releaseFadeOut(FADE_TIME / 7.5f)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        HEARTOPIA_BAGPIPES = hnsr(loc("heartopia_bagpipes"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        HEARTOPIA_CELLO = hnsr(loc("heartopia_cello"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        HEARTOPIA_VIOLIN = hnsr(loc("heartopia_violin"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        HEARTOPIA_OCARINA = hnsr(loc("heartopia_ocarina"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        HEARTOPIA_SOPRANO_RECORDER = hnsr(loc("heartopia_soprano_recorder"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        HEARTOPIA_SAX = hnsr(loc("heartopia_sax"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        HEARTOPIA_CONCERTINA = hnsr(loc("heartopia_concertina"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        HEARTOPIA_XIAO = hnsr(loc("heartopia_xiao"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        HEARTOPIA_CONCH_SHELLS = hnsr(loc("heartopia_conch_shells"))
            .holdBuilder(SCSounds::oneOctaveSoundBuilder)
            .attackBuilder(SCSounds::oneOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SKY_CELLO = hnsr(loc("sky_cello"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SKY_VIOLIN = hnsr(loc("sky_violin"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SKY_TRANSVERSE_FLUTE = hnsr(loc("sky_transverse_flute"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SKY_SAXOPHONE = hnsr(loc("sky_saxophone"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SKY_AURORA = hnsr(loc("sky_aurora"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SKY_FLUTE = hnsr(loc("sky_flute"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SKY_PANFLUTE = hnsr(loc("sky_panflute"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SKY_OCARINA = hnsr(loc("sky_ocarina"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SKY_MANTA_OCARINA = hnsr(loc("sky_manta_ocarina"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SKY_HORN = hnsr(loc("sky_horn"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SKY_TRUMPET = hnsr(loc("sky_trumpet"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SKY_HARMONICA = hnsr(loc("sky_harmonica"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SYNTH_SINE = hnsr(loc("synth_sine"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            // About 0.5 s to fade out from full volume when released
            .releaseFadeOut(FADE_TIME / 2.5f)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SYNTH_SQUARE = hnsr(loc("synth_square"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            // About 0.5 s to fade out from full volume when released
            .releaseFadeOut(FADE_TIME / 2.5f)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SYNTH_SAW = hnsr(loc("synth_saw"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            // About 0.5 s to fade out from full volume when released
            .releaseFadeOut(FADE_TIME / 2.5f)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        SYNTH_TRIANGLE = hnsr(loc("synth_triangle"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            // About 0.5 s to fade out from full volume when released
            .releaseFadeOut(FADE_TIME / 2.5f)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),


        FF14_FLUTE = hnsr(loc("ff14_flute"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_OBOE = hnsr(loc("ff14_oboe"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_CLARINET = hnsr(loc("ff14_clarinet"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_FIFE = hnsr(loc("ff14_fife"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_PANPIPES = hnsr(loc("ff14_panpipes"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_TRUMPET = hnsr(loc("ff14_trumpet"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_TROMBONE = hnsr(loc("ff14_trombone"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_TUBA = hnsr(loc("ff14_tuba"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_HORN = hnsr(loc("ff14_horn"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_SAXOPHONE = hnsr(loc("ff14_saxophone"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_VIOLIN = hnsr(loc("ff14_violin"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_VIOLA = hnsr(loc("ff14_viola"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_CELLO = hnsr(loc("ff14_cello"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_DOUBLE_BASS = hnsr(loc("ff14_double_bass"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        // Sky's Electric Guitar
        SKY_LIGHT_GUITAR = hnsr(loc("sky_light_guitar"))
            .holdBuilder(SCSounds::twoOctaveSoundBuilder)
            .attackBuilder(SCSounds::twoOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            // About 1.5 s to fade out from full volume when released, like the distortion guitar
            .releaseFadeOut(FADE_TIME / 7.5f)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_CLEAN_GUITAR = hnsr(loc("ff14_clean_guitar"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            // About 1.5 s to fade out from full volume when released, like the distortion guitar
            .releaseFadeOut(FADE_TIME / 7.5f)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_OVERDRIVEN_GUITAR = hnsr(loc("ff14_overdriven_guitar"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            // About 1.5 s to fade out from full volume when released, like the distortion guitar
            .releaseFadeOut(FADE_TIME / 7.5f)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        FF14_POWER_CHORDS_GUITAR = hnsr(loc("ff14_power_chords_guitar"))
            .holdBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveMonoSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            // About 1.5 s to fade out from full volume when released, like the distortion guitar
            .releaseFadeOut(FADE_TIME / 7.5f)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        WWM_4 = hnsr(loc("wwm_4"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        WWM_5 = hnsr(loc("wwm_5"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION)
    ;

    private static NoteSound[] oneOctaveSoundBuilder(final NoteSoundRegistrar builder) {
        return builder.stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 1);
    }

    private static NoteSound[] twoOctaveSoundBuilder(final NoteSoundRegistrar builder) {
        return builder.stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 2);
    }

    private static NoteSound[] threeOctaveSoundBuilder(final NoteSoundRegistrar builder) {
        return builder.stereo().registerGrid(GridInstrumentScreen.DEF_COLUMNS, 3);
    }
    /**
     * For sounds that only come in mono, such as the FF14 instrument's
     */
    private static NoteSound[] threeOctaveMonoSoundBuilder(final NoteSoundRegistrar builder) {
        return builder.registerGrid(GridInstrumentScreen.DEF_COLUMNS, 3);
    }

    /**
     * Shorthand for {@code new ResourceLocation(Main.MODID, name)}
     */
    private static ResourceLocation loc(final String name) {
        return new ResourceLocation(SCInstrumentMod.MODID, name);
    }
    /**
     * Shorthand for {@code new NoteSoundRegistrar(soundRegistrar, instrumentId)}
     */
    private static NoteSoundRegistrar nsr(ResourceLocation instrumentId) {
        return new NoteSoundRegistrar(SCSounds.SOUNDS, instrumentId);
    }
    /**
     * Shorthand for {@code new HeldNoteSoundRegistrar(soundRegistrar, instrumentId)}
     */
    private static HeldNoteSoundRegistrar hnsr(ResourceLocation instrumentId) {
        return new HeldNoteSoundRegistrar(SCSounds.SOUNDS, instrumentId);
    }

    private static final HashMap<NoteBlockInstrument, NoteSound[]> NOTE_BLOCK_SOUNDS = registerNoteBlockSounds();
    /**
     * @return The 3 octave grid of the given note block sound, starting on C
     */
    public static NoteSound[] getNoteBlockSounds(final NoteBlockInstrument instrument) {
        return NOTE_BLOCK_SOUNDS.get(instrument);
    }

    /**
     * Registers a 3 octave grid for every tunable note block sound.
     * Their sounds.json entries pitch the vanilla sound files; see {@code SoundJsonGenerator#registerNoteBlockSounds}.
     */
    private static HashMap<NoteBlockInstrument, NoteSound[]> registerNoteBlockSounds() {
        final HashMap<NoteBlockInstrument, NoteSound[]> result = new HashMap<>();

        for (final NoteBlockInstrument instrument : NoteBlockInstrument.values()) {
            if (!instrument.isTunable())
                continue;

            result.put(instrument,
                threeOctaveMonoSoundBuilder(nsr(loc("note_block_" + instrument.getSerializedName())))
            );
        }

        return result;
    }
}
