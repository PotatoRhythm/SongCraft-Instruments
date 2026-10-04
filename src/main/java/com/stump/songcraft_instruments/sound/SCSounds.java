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

    static {
        NOTEBLOCK_SOUNDS = new HashMap<>();
        registerNoteBlockSounds();
    }

    public static final NoteSound[]
        WINDSONG_LYRE_NOTE_SOUNDS = nsr(loc("windsong_lyre")).stereo().registerGrid(),
        VINTAGE_LYRE_NOTE_SOUNDS = nsr(loc("vintage_lyre")).registerGrid(),

        ZITHER_NEW_NOTE_SOUNDS = nsr(loc("floral_zither_new")).registerGrid(),
        ZITHER_OLD_NOTE_SOUNDS = nsr(loc("floral_zither_old")).registerGrid(),

        UKULELE = nsr(loc("ukulele")).registerGrid(),
        DJEM_DJEM_DRUM = nsr(loc("djem_djem_drum")).registerGrid(2, 4),

        KEYBOARD = nsr(loc("keyboard")).stereo().registerGrid(),
        KEYBOARD_GW2 = nsr(loc("keyboard_gw2")).stereo().registerGrid(),
        KEYBOARD_YAMAHA_C5 = nsr(loc("keyboard_yamaha_c5")).stereo().registerGrid(),
        HEARTOPIA = nsr(loc("keyboard_heartopia")).stereo().registerGrid(),
        KEYBOARD_ELECTRIC = nsr(loc("keyboard_electric")).stereo().registerGrid(),
        KEYBOARD_HARPSICHORD = nsr(loc("keyboard_harpsichord")).stereo().registerGrid(),

        TROMBONE = nsr(loc("trombone")).registerGrid(),
        SAXOPHONE = nsr(loc("saxophone")).registerGrid(),

        GUITAR = nsr(loc("guitar")).registerGrid(),
        GUITAR_NYLON = nsr(loc("guitar_nylon")).stereo().registerGrid(),
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

        GW2_BASS = nsr(loc("gw2_bass")).stereo().registerGrid(GridInstrumentScreen.DEF_ROWS, 2),
        GW2_BELL = nsr(loc("gw2_bell")).registerGrid(GridInstrumentScreen.DEF_ROWS, 2),
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
//                .add(GridInstrumentScreen.DEF_ROWS * 2)
//                .registerAll()
//            )

            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),

        VIOLIN_SLOW = hnsr(loc("violin_slow"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
            .holdDelay(.03f)
            .chainedHoldDelay(-FADE_TIME * 2)
            .releaseFadeOut(FADE_TIME / 10)
            .fullHoldFadeoutTime(2)
            .decays(7)
            .register(HOLD_DURATION),
        VIOLIN_FAST = hnsr(loc("violin_fast"))
            .holdBuilder(SCSounds::threeOctaveSoundBuilder)
            .attackBuilder(SCSounds::threeOctaveSoundBuilder)
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
            .register(HOLD_DURATION)
    ;

    private static NoteSound[] twoOctaveSoundBuilder(final NoteSoundRegistrar builder) {
        return builder.stereo().registerGrid(GridInstrumentScreen.DEF_ROWS, 2);
    }

    private static NoteSound[] threeOctaveSoundBuilder(final NoteSoundRegistrar builder) {
        return builder.stereo().registerGrid(GridInstrumentScreen.DEF_ROWS, 3);
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

    private static final HashMap<NoteBlockInstrument, NoteSound> NOTEBLOCK_SOUNDS;
    public static NoteSound[] getNoteblockSounds(final NoteBlockInstrument instrumentType) {
        return new NoteSound[] {NOTEBLOCK_SOUNDS.get(instrumentType)};
    }

    private static void registerNoteBlockSounds() {
        final NoteSoundRegistrar registrar = nsr(loc("note_block_instrument"));

        for (NoteBlockInstrument noteSound : NoteBlockInstrument.values()) {
            registrar.chain(noteSound.getSoundEvent().get().getLocation())
                    .alreadyRegistered()
                    .add();
            NOTEBLOCK_SOUNDS.put(noteSound, registrar.peek());
        }

        registrar.registerAll();
    }
}
