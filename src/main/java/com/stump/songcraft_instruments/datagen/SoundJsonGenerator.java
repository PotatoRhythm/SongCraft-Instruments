package com.stump.songcraft_instruments.datagen;

import com.stump.songcraft_instruments.sound.NoteSound;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.common.data.SoundDefinitionsProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Generates sounds.json.
 * <p>Instrument volumes are balanced for playing together, by their loudness as heard in game (ITU-R BS.1770).
 * Relative to melody instruments (winds, brass, bowed strings, voices, keyboards): basses +4 dB, drums +3 dB,
 * electric guitars 0 dB, plucked melody instruments, synths and cymbals -1 dB, animal calls -2 dB,
 * guitars, pads and bells -3 dB. Chord instruments sit lower since their notes add up.
 * Held instruments sit 2 dB lower again: they are measured at their steady level, while the other notes
 * are measured at their strike and then decay, so a held melody would otherwise drown out a played one.
 * Drums whose hits already peak near full scale could not be raised all the way.
 * After listening tests, heartopia_violin, nightwind_horn, bass_acoustic and microphone_not_teto
 * sit 2 dB higher than their group.</p>
 */
public class SoundJsonGenerator extends SoundDefinitionsProvider {

    private static final String STEREO_SUFFIX = ".stereo";

    private final ExistingFileHelper fileHelper;

    public SoundJsonGenerator(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, "songcraft_instruments", fileHelper);
        this.fileHelper = fileHelper;
    }

    @Override
    public void registerSounds() {
        trackMissingTwins();

        registerInstrument("windsong_lyre", 20, 0.973f, false, true);

        registerInstrument("vintage_lyre", 20, 0.735f, false, false);

        registerInstrument("floral_zither_new", 20, 1f, false, false);
        registerInstrument("floral_zither_old", 20, 1f, false, false);

        registerInstrument("ukulele", 20, 0.752f, false, false);

        registerInstrument("djem_djem_drum", 7, 1f, false, false);

        registerInstrument("nightwind_horn", 13, 0.675f, true, true);

        registerInstrument("shamisen", 20, 0.828f, false, true);

        registerInstrument("koto", 20, 0.349f, false, false);

        registerInstrument("pipa_regular", 20, 0.382f, false, false);
        registerInstrument("pipa_tremolo", 20, 0.583f, false, false);

        registerInstrument("keyboard_gw2", 20, 0.396f, false, true);
        registerInstrument("keyboard_yamaha_c5", 20, 0.573f, false, true);
        registerInstrument("keyboard_heartopia", 20, 1f, false, true);

        registerInstrument("heartopia_harp", 20, 1f, false, true);
        registerInstrument("heartopia_kalimba", 13, 1f, false, true);
        registerInstrument("heartopia_lyre", 13, 0.959f, false, true);
        registerInstrument("heartopia_hang", 13, 0.878f, false, true);
        registerInstrument("heartopia_acoustic_bass", 13, 1f, false, true);
        registerInstrument("heartopia_concertina", 13, 1f, true, true);
        registerInstrument("heartopia_lunghe", 13, 0.759f, false, true);
        registerInstrument("heartopia_xiao", 13, 1f, true, true);
        registerInstrument("heartopia_bagpipes", 13, 1f, true, true);
        registerInstrument("heartopia_cello", 13, 1f, true, true);
        registerInstrument("heartopia_violin", 13, 1f, true, true);
        registerInstrument("heartopia_ocarina", 13, 1f, true, true);
        registerInstrument("heartopia_soprano_recorder", 13, 1f, true, true);
        registerInstrument("heartopia_sax", 13, 0.934f, true, true);
        registerInstrument("heartopia_conch_shells", 6, 0.932f, true, true);
        registerInstrument("heartopia_cajon", 7, 1f, false, true);
        registerInstrument("heartopia_conga_bongos", 7, 1f, false, true);

        registerInstrument("sky_piano", 20, 1f, false, true);
        registerInstrument("sky_grand_piano", 20, 1f, false, true);
        registerInstrument("sky_winter_piano", 20, 1f, false, true);
        registerInstrument("sky_guitar", 13, 1f, false, true);
        registerInstrument("sky_light_guitar", 13, 0.833f, true, true);
        registerInstrument("sky_toy_ukulele", 13, 1f, false, true);
        registerInstrument("sky_pipa", 13, 0.83f, false, true);
        registerInstrument("sky_kalimba", 13, 0.799f, false, true);
        registerInstrument("sky_xylophone", 13, 0.581f, false, true);
        registerInstrument("sky_contrabass", 13, 1f, false, true);
        registerInstrument("sky_cello", 13, 1f, true, true);
        registerInstrument("sky_violin", 13, 1f, true, true);
        registerInstrument("sky_flute", 13, 1f, true, true);
        registerInstrument("sky_transverse_flute", 13, 0.896f, true, true);
        registerInstrument("sky_panflute", 13, 1f, true, true);
        registerInstrument("sky_ocarina", 13, 0.819f, true, true);
        registerInstrument("sky_manta_ocarina", 13, 0.75f, true, true);
        registerInstrument("sky_horn", 13, 1f, true, true);
        registerInstrument("sky_trumpet", 13, 0.893f, true, true);
        registerInstrument("sky_saxophone", 13, 1f, true, true);
        registerInstrument("sky_harmonica", 13, 1f, true, true);
        registerInstrument("sky_aurora", 13, 0.913f, true, true);
        registerInstrument("sky_bells", 7, 0.908f, false, true);
        registerInstrument("sky_small_bell", 7, 0.546f, false, true);
        registerInstrument("sky_handpan", 13, 0.647f, false, true);
        registerInstrument("sky_triumph_handpan", 13, 0.908f, false, true);
        registerInstrument("sky_krill_horn", 7, 0.663f, false, true);
        registerInstrument("sky_drum", 7, 1f, false, true);
        registerInstrument("sky_dundun", 7, 1f, false, true);
        registerInstrument("sky_fortune_drum", 3, 1f, false, true);
        registerInstrument("sky_cymbals", 3, 0.707f, false, true);
        registerInstrument("sky_bird_call", 13, 0.617f, false, true);
        registerInstrument("sky_crab_call", 13, 0.472f, false, true);
        registerInstrument("sky_fish_call", 13, 0.68f, false, true);
        registerInstrument("sky_jelly_call", 13, 0.612f, false, true);
        registerInstrument("sky_manta_call", 13, 0.627f, false, true);
        registerInstrument("sky_spirit_manta_call", 13, 0.737f, false, true);
        registerInstrument("sky_moth_call", 13, 0.635f, false, true);
        registerInstrument("sky_bass_synth", 13, 1f, false, true);
        registerInstrument("sky_chime_synth", 13, 0.774f, false, true);
        registerInstrument("synth_sine", 20, 0.603f, true, true);
        registerInstrument("synth_square", 20, 1f, true, true);
        registerInstrument("synth_saw", 20, 1f, true, true);
        registerInstrument("synth_triangle", 20, 0.464f, true, true);
        registerInstrument("sky_tr_909", 7, 1f, false, true);
        registerInstrument("sky_dance", 5, 1f, false, true);

        registerInstrument("ff14_harp", 20, 0.673f, false, false);
        registerInstrument("ff14_piano", 20, 0.751f, false, false);
        registerInstrument("ff14_lute", 20, 0.57f, false, false);
        registerInstrument("ff14_fiddle", 20, 0.86f, false, false);
        registerInstrument("ff14_flute", 20, 0.994f, true, false);
        registerInstrument("ff14_oboe", 20, 1f, true, false);
        registerInstrument("ff14_clarinet", 20, 0.925f, true, false);
        registerInstrument("ff14_fife", 20, 0.934f, true, false);
        registerInstrument("ff14_panpipes", 20, 0.869f, true, false);
        registerInstrument("ff14_timpani", 20, 1f, false, false);
        registerInstrument("ff14_bongo", 7, 1f, false, false);
        registerInstrument("ff14_marching_drums", 20, 1f, false, false);
        registerInstrument("ff14_drum_kit", 7, 1f, false, false);
        registerInstrument("ff14_trumpet", 20, 0.915f, true, false);
        registerInstrument("ff14_trombone", 20, 0.935f, true, false);
        registerInstrument("ff14_tuba", 20, 1f, true, false);
        registerInstrument("ff14_horn", 20, 0.818f, true, false);
        registerInstrument("ff14_saxophone", 20, 0.973f, true, false);
        registerInstrument("ff14_violin", 20, 0.788f, true, false);
        registerInstrument("ff14_viola", 20, 0.966f, true, false);
        registerInstrument("ff14_cello", 20, 0.899f, true, false);
        registerInstrument("ff14_double_bass", 20, 1f, true, false);
        registerInstrument("ff14_overdriven_guitar", 20, 0.977f, true, false);
        registerInstrument("ff14_clean_guitar", 20, 0.852f, true, false);
        registerInstrument("ff14_muted_guitar", 20, 0.816f, false, false);
        registerInstrument("ff14_power_chords_guitar", 20, 1f, true, false);
        registerInstrument("ff14_special_guitar", 20, 0.849f, false, false);

        registerInstrument("wwm_1", 20, 1f, false, true);
        registerInstrument("wwm_2", 20, 0.837f, false, true);
        registerInstrument("wwm_3", 20, 0.689f, false, true);
        registerInstrument("wwm_4", 20, 1f, true, true);
        registerInstrument("wwm_5", 20, 1f, true, true);
        registerInstrument("wwm_6", 20, 0.706f, false, true);
        registerInstrument("wwm_7", 13, 0.948f, false, true);
        registerInstrument("keyboard_electric", 20, 0.943f, false, true);
        registerInstrument("keyboard_harpsichord", 20, 0.886f, false, true);

        registerInstrument("trumpet_westgate_studios", 20, 0.493f, true, true);
        registerInstrument("trombone_phgm", 20, 0.399f, true, true);

        registerInstrument("saxophone_baritone", 20, 0.639f, true, true);
        registerInstrument("saxophone_tenor", 20, 0.739f, true, true);

        registerInstrument("guitar", 20, 0.35f, false, false);
        registerInstrument("guitar_steel", 20, 0.525f, false, true);

        registerInstrument("guitar_clean", 20, 0.328f, false, true);
        registerInstrument("guitar_distortion", 20, 0.386f, true, true);
        registerInstrument("guitar_harmonics", 20, 0.306f, false, true);

        registerInstrument("bass_acoustic", 20, 0.815f, false, true);
        registerInstrument("bass_finger", 20, 0.653f, false, true);
        registerInstrument("bass_slap", 20, 0.592f, false, true);
        registerInstrument("bass_picked", 20, 0.67f, false, true);

        registerInstrument("violin_pizzicato", 20, 0.814f, false, true);

        registerInstrument("microphone_irina_brochin", 20, 0.212f, true, true);
        registerInstrument("microphone_bass_choir", 20, 0.355f, true, true);
        registerInstrument("microphone_not_miku", 20, 1f, true, true);
        registerInstrument("microphone_not_teto", 20, 1f, true, true);
        registerInstrument("microphone_not_teto_sneaky", 20, 1f, true, true);
        registerInstrument("microphone_cat", 13, 0.241f, false, false);
        registerInstrument("microphone_dog", 13, 0.385f, false, false);
        registerInstrument("microphone_meatball_man", 20, 0.282f, true, true);

        registerInstrument("gw2_bass", 13, 0.751f, false, true);
        registerInstrument("gw2_bell", 13, 0.39f, false, true);
        registerInstrument("gw2_harp", 20, 0.609f, false, true);
        registerInstrument("gw2_lute", 20, 1f, false, true);
        registerInstrument("gw2_minstrel", 20, 0.886f, false, true);
        registerInstrument("gw2_pell", 20, 0.527f, false, true);
        registerInstrument("gw2_frame_drum", 9, 1f, false, true);
        registerInstrument("gw2_horn", 20, 0.709f, true, true);
        registerInstrument("gw2_flute", 13, 0.542f, true, true);
        registerInstrument("gw2_verdarach", 20, 0.509f, true, true);
        registerInstrument("gw2_organ", 20, 0.405f, true, true);
        registerInstrument("gw2_quaggan_organ", 20, 0.781f, true, true);

        registerNoteBlockSounds();

        float glorious_drum_V = 1f;
        add("glorious_drum_don", definition().with(trimmedSound("songcraft_instruments:glorious_drum/don", glorious_drum_V)));
        add("glorious_drum_ka", definition().with(trimmedSound("songcraft_instruments:glorious_drum/ka", glorious_drum_V)));
        add("glorious_drum_ka_stereo", definition().with(trimmedSound("songcraft_instruments:glorious_drum/ka.stereo", glorious_drum_V)));

        // The Lo-fi pedal's tape deck. The hiss's level is set in game, by the pedal's Age knob
        add("pedal_tape_hiss", definition().with(sound("songcraft_instruments:lofi_pedal/tape_hiss").attenuationDistance(32)));
        add("pedal_tape_start", definition().with(sound("songcraft_instruments:lofi_pedal/tape_start")));
        add("pedal_tape_stop", definition().with(sound("songcraft_instruments:lofi_pedal/tape_stop")));


        float drumset_gw2_V = 0.758f;
        addSoundVariants("drumset_bass", "songcraft_instruments:drumset_gw2/0", drumset_gw2_V, true);
        addSoundVariants("drumset_bass2", "songcraft_instruments:drumset_gw2/0", drumset_gw2_V, true);
        addSoundVariants("drumset_snare", "songcraft_instruments:drumset_gw2/1", drumset_gw2_V, true);
        addSoundVariants("drumset_snare2", "songcraft_instruments:drumset_gw2/1", drumset_gw2_V, true);
        addSoundVariants("drumset_cross_stick", "songcraft_instruments:drumset_gw2/2", drumset_gw2_V, true);
        addSoundVariants("drumset_ghost", "songcraft_instruments:drumset_gw2/3", drumset_gw2_V, true);
        addSoundVariants("drumset_ghost2", "songcraft_instruments:drumset_gw2/3", drumset_gw2_V, true);
        addSoundVariants("drumset_high_tom", "songcraft_instruments:drumset_gw2/4", drumset_gw2_V, true);
        addSoundVariants("drumset_mid_tom", "songcraft_instruments:drumset_gw2/5", drumset_gw2_V, true);
        addSoundVariants("drumset_low_tom", "songcraft_instruments:drumset_gw2/6", drumset_gw2_V, true);
        addSoundVariants("drumset_crash_cymbal", "songcraft_instruments:drumset_gw2/7", drumset_gw2_V, true);
        addSoundVariants("drumset_ride_cymbal", "songcraft_instruments:drumset_gw2/8", drumset_gw2_V, true);
        addSoundVariants("drumset_hi-hat_closed", "songcraft_instruments:drumset_gw2/9", drumset_gw2_V, true);
        addSoundVariants("drumset_hi-hat_open", "songcraft_instruments:drumset_gw2/10", drumset_gw2_V, true);
        addSoundVariants("drumset_hi-hat_foot", "songcraft_instruments:drumset_gw2/11", drumset_gw2_V, true);

        float drumset_standard_V = 0.953f;
        addSoundVariants("drumset_standard_bass", "songcraft_instruments:drumset_standard/0", drumset_standard_V, true);
        addSoundVariants("drumset_standard_bass2", "songcraft_instruments:drumset_standard/0", drumset_standard_V, true);
        addSoundVariants("drumset_standard_snare", "songcraft_instruments:drumset_standard/1", drumset_standard_V, true);
        addSoundVariants("drumset_standard_snare2", "songcraft_instruments:drumset_standard/1", drumset_standard_V, true);
        addSoundVariants("drumset_standard_cross_stick", "songcraft_instruments:drumset_standard/2", drumset_standard_V, true);
        addSoundVariants("drumset_standard_ghost", "songcraft_instruments:drumset_standard/3", drumset_standard_V, true);
        addSoundVariants("drumset_standard_ghost2", "songcraft_instruments:drumset_standard/3", drumset_standard_V, true);
        addSoundVariants("drumset_standard_high_tom", "songcraft_instruments:drumset_standard/4", drumset_standard_V, true);
        addSoundVariants("drumset_standard_mid_tom", "songcraft_instruments:drumset_standard/5", drumset_standard_V, true);
        addSoundVariants("drumset_standard_low_tom", "songcraft_instruments:drumset_standard/6", drumset_standard_V, true);
        addSoundVariants("drumset_standard_crash_cymbal", "songcraft_instruments:drumset_standard/7", drumset_standard_V, true);
        addSoundVariants("drumset_standard_ride_cymbal", "songcraft_instruments:drumset_standard/8", drumset_standard_V, true);
        addSoundVariants("drumset_standard_hi-hat_closed", "songcraft_instruments:drumset_standard/9", drumset_standard_V, true);
        addSoundVariants("drumset_standard_hi-hat_open", "songcraft_instruments:drumset_standard/10", drumset_standard_V, true);
        addSoundVariants("drumset_standard_hi-hat_foot", "songcraft_instruments:drumset_standard/11", drumset_standard_V, true);

        float drumset_power_V = 0.678f;
        addSoundVariants("drumset_power_bass", "songcraft_instruments:drumset_power/0", drumset_power_V, true);
        addSoundVariants("drumset_power_bass2", "songcraft_instruments:drumset_power/0", drumset_power_V, true);
        addSoundVariants("drumset_power_snare", "songcraft_instruments:drumset_power/1", drumset_power_V, true);
        addSoundVariants("drumset_power_snare2", "songcraft_instruments:drumset_power/1", drumset_power_V, true);
        addSoundVariants("drumset_power_cross_stick", "songcraft_instruments:drumset_power/2", drumset_power_V, true);
        addSoundVariants("drumset_power_ghost", "songcraft_instruments:drumset_power/3", drumset_power_V, true);
        addSoundVariants("drumset_power_ghost2", "songcraft_instruments:drumset_power/3", drumset_power_V, true);
        addSoundVariants("drumset_power_high_tom", "songcraft_instruments:drumset_power/4", drumset_power_V, true);
        addSoundVariants("drumset_power_mid_tom", "songcraft_instruments:drumset_power/5", drumset_power_V, true);
        addSoundVariants("drumset_power_low_tom", "songcraft_instruments:drumset_power/6", drumset_power_V, true);
        addSoundVariants("drumset_power_crash_cymbal", "songcraft_instruments:drumset_power/7", drumset_power_V, true);
        addSoundVariants("drumset_power_ride_cymbal", "songcraft_instruments:drumset_power/8", drumset_power_V, true);
        addSoundVariants("drumset_power_hi-hat_closed", "songcraft_instruments:drumset_power/9", drumset_power_V, true);
        addSoundVariants("drumset_power_hi-hat_open", "songcraft_instruments:drumset_power/10", drumset_power_V, true);
        addSoundVariants("drumset_power_hi-hat_foot", "songcraft_instruments:drumset_power/11", drumset_power_V, true);

        float drumset_orchestral_V = 0.748f;
        addSoundVariants("drumset_orchestral_bass", "songcraft_instruments:drumset_orchestral/0", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_bass2", "songcraft_instruments:drumset_orchestral/0", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_snare", "songcraft_instruments:drumset_orchestral/1", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_snare2", "songcraft_instruments:drumset_orchestral/1", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_cross_stick", "songcraft_instruments:drumset_orchestral/2", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_ghost", "songcraft_instruments:drumset_orchestral/3", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_ghost2", "songcraft_instruments:drumset_orchestral/3", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_high_tom", "songcraft_instruments:drumset_orchestral/4", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_mid_tom", "songcraft_instruments:drumset_orchestral/5", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_low_tom", "songcraft_instruments:drumset_orchestral/6", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_crash_cymbal", "songcraft_instruments:drumset_orchestral/7", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_ride_cymbal", "songcraft_instruments:drumset_orchestral/8", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_hi-hat_closed", "songcraft_instruments:drumset_orchestral/9", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_hi-hat_open", "songcraft_instruments:drumset_orchestral/10", drumset_orchestral_V, true);
        addSoundVariants("drumset_orchestral_hi-hat_foot", "songcraft_instruments:drumset_orchestral/11", drumset_orchestral_V, true);

        float drumset_cozy_V = 1f;
        addSoundVariants("drumset_cozy_bass", "songcraft_instruments:drumset_cozy/0", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_bass2", "songcraft_instruments:drumset_cozy/0", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_snare", "songcraft_instruments:drumset_cozy/1", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_snare2", "songcraft_instruments:drumset_cozy/1", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_cross_stick", "songcraft_instruments:drumset_cozy/2", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_ghost", "songcraft_instruments:drumset_cozy/3", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_ghost2", "songcraft_instruments:drumset_cozy/3", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_high_tom", "songcraft_instruments:drumset_cozy/4", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_mid_tom", "songcraft_instruments:drumset_cozy/5", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_low_tom", "songcraft_instruments:drumset_cozy/6", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_crash_cymbal", "songcraft_instruments:drumset_cozy/7", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_ride_cymbal", "songcraft_instruments:drumset_cozy/8", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_hi-hat_closed", "songcraft_instruments:drumset_cozy/9", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_hi-hat_open", "songcraft_instruments:drumset_cozy/10", drumset_cozy_V, true);
        addSoundVariants("drumset_cozy_hi-hat_foot", "songcraft_instruments:drumset_cozy/11", drumset_cozy_V, true);
    }

    private static final int MONO_DISTANCE = NoteSound.MONO_DISTANCE;

    /**
     * @param instrumentName    Instrument name (gw2_quaggan_organ, nightwind_horn, etc)
     * @param maxNotes          Max note index
     * @param volume            Volume of the instrument's loudest sound file. The others are trimmed below it by
     *                          {@link SoundVolumeTrims}, so changing this turns the whole instrument up or down.
     * @param heldSound         If true, generates attack + hold variants
     * @param supportsStereo    If true, generates .stereo variants
     */
    private void registerInstrument(String instrumentName, int maxNotes, float volume, boolean heldSound, boolean supportsStereo) {
        for (int i = 0; i <= maxNotes; i++) {
            if (!heldSound) {
                String basePath = "songcraft_instruments:" + instrumentName + "/" + i;
                String event = instrumentName + "_note_" + i;

                // MONO
                add(event, definition().with(
                        trimmedSound(basePath, volume)
                                .attenuationDistance(MONO_DISTANCE)
                ));

                if (supportsStereo) {
                    String stereoPath = "songcraft_instruments:" + instrumentName + "/" + i + ".stereo";
                    String stereoEvent = instrumentName + "_note_" + i + "_stereo";

                    // STEREO (attenuated manually, see NoteSound#stereoGain)
                    add(stereoEvent, definition().with(
                            trimmedSound(stereoPath, volume)
                    ));
                }
            }
            else {
                String holdPath = "songcraft_instruments:" + instrumentName + "/hold/" + i;
                String holdEvent = instrumentName + "_hold_note_" + i;

                // MONO
                add(holdEvent, definition().with(
                        trimmedSound(holdPath, volume)
                                .attenuationDistance(MONO_DISTANCE)
                ));

                if (supportsStereo) {
                    String holdStereoPath = "songcraft_instruments:" + instrumentName + "/hold/" + i + ".stereo";
                    String holdStereoEvent = instrumentName + "_hold_note_" + i + "_stereo";

                    add(holdStereoEvent, definition().with(
                            trimmedSound(holdStereoPath, volume)
                    ));
                }

                String attackPath = "songcraft_instruments:" + instrumentName + "/attack/" + i;
                String attackEvent = instrumentName + "_attack_note_" + i;

                // MONO
                add(attackEvent, definition().with(
                        trimmedSound(attackPath, volume)
                                .attenuationDistance(MONO_DISTANCE)
                ));

                if (supportsStereo) {
                    String attackStereoPath = "songcraft_instruments:" + instrumentName + "/attack/" + i + ".stereo";
                    String attackStereoEvent = instrumentName + "_attack_note_" + i + "_stereo";

                    add(attackStereoEvent, definition().with(
                            trimmedSound(attackStereoPath, volume)
                    ));
                }
            }
        }
    }

    /**
     * Vanilla note block sound files, by their note block instrument names
     */
    private static final String[][] NOTE_BLOCK_SOUND_FILES = {
        {"harp", "harp"}, {"basedrum", "bd"}, {"snare", "snare"}, {"hat", "hat"},
        {"bass", "bassattack"}, {"flute", "flute"}, {"bell", "bell"}, {"guitar", "guitar"},
        {"chime", "icechime"}, {"xylophone", "xylobone"}, {"iron_xylophone", "iron_xylophone"},
        {"cow_bell", "cow_bell"}, {"didgeridoo", "didgeridoo"}, {"bit", "bit"},
        {"banjo", "banjo"}, {"pling", "pling"}
    };
    /**
     * Volumes of the note block sounds, balancing them like the instruments.
     * They can only be turned down, since the files are vanilla's.
     */
    private static final Map<String, Float> NOTE_BLOCK_VOLUMES = Map.ofEntries(
        Map.entry("basedrum", 0.83f), Map.entry("flute", 0.65f), Map.entry("bell", 0.91f),
        Map.entry("guitar", 0.83f), Map.entry("iron_xylophone", 0.47f), Map.entry("cow_bell", 0.82f),
        Map.entry("banjo", 0.79f), Map.entry("pling", 0.58f)
    );
    /**
     * Semitones above C of each column of the note grid
     */
    private static final int[] GRID_SEMITONES = {0, 2, 4, 5, 7, 9, 11};

    /**
     * Registers a 3 octave grid starting on C for every tunable note block sound, by pitching the vanilla sound files.
     * The vanilla files play an F# at pitch 1, and the grid's lowest C is 1.5 octaves below that,
     * so the vanilla note block's 2 octave range sits within the grid.
     */
    private void registerNoteBlockSounds() {
        for (final String[] noteBlockSound : NOTE_BLOCK_SOUND_FILES) {
            for (int i = 0; i < 21; i++) {
                final int semitones = (i / 7) * 12 + GRID_SEMITONES[i % 7] - 18;

                add("note_block_" + noteBlockSound[0] + "_note_" + i, definition().with(
                        sound("minecraft:note/" + noteBlockSound[1])
                                .volume(NOTE_BLOCK_VOLUMES.getOrDefault(noteBlockSound[0], 1f))
                                .pitch(Math.pow(2, semitones / 12d))
                                .attenuationDistance(MONO_DISTANCE)
                ));
            }
        }
    }

    private void addSoundVariants(String event, String soundPath, float volume, boolean supportsStereo) {
        String[] parts = soundPath.split(":", 2);

        Path soundDirectory = soundsDirectory(parts[0]).resolve(parts[1]);

        SoundDefinition monoDefinition = definition();
        SoundDefinition stereoDefinition = supportsStereo ? definition() : null;

        // A variant may only ship one of its Mono & Stereo files, the other being its twin (see ChannelTwinSounds)
        Set<String> variantNames = new TreeSet<>();

        try (Stream<Path> files = Files.list(soundDirectory)) {
            files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.toLowerCase().endsWith(".ogg"))
                    .map(fileName -> {
                        String soundName = fileName.substring(0, fileName.length() - 4);
                        return soundName.endsWith(STEREO_SUFFIX)
                                ? soundName.substring(0, soundName.length() - STEREO_SUFFIX.length())
                                : soundName;
                    })
                    .forEach(variantNames::add);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to parse sound directory: " + soundDirectory,
                    e
            );
        }

        for (String variantName : variantNames) {
            monoDefinition.with(
                    trimmedSound(soundPath + "/" + variantName, volume)
                            .attenuationDistance(MONO_DISTANCE)
            );

            if (supportsStereo) {
                stereoDefinition.with(
                        trimmedSound(soundPath + "/" + variantName + STEREO_SUFFIX, volume)
                );
            }
        }

        add(event, monoDefinition);

        if (supportsStereo)
            add(event + "_stereo", stereoDefinition);
    }

    /**
     * Marks the Mono/Stereo twin of every sound file that only ships one of them as existing,
     * since the game creates the missing twin from the other file (see ChannelTwinSounds).
     */
    private void trackMissingTwins() {
        String namespace = "songcraft_instruments";
        Path soundDirectory = soundsDirectory(namespace);

        try (Stream<Path> files = Files.walk(soundDirectory)) {
            files.filter(Files::isRegularFile)
                    .map(path -> soundDirectory.relativize(path).toString().replace('\\', '/'))
                    .filter(path -> path.endsWith(".ogg"))
                    .forEach(path -> {
                        String soundName = path.substring(0, path.length() - 4);
                        String twinName = soundName.endsWith(STEREO_SUFFIX)
                                ? soundName.substring(0, soundName.length() - STEREO_SUFFIX.length())
                                : soundName + STEREO_SUFFIX;

                        if (!Files.exists(soundDirectory.resolve(twinName + ".ogg")))
                            fileHelper.trackGenerated(new ResourceLocation(namespace, twinName),
                                    PackType.CLIENT_RESOURCES, ".ogg", "sounds");
                    });
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to parse sound directory: " + soundDirectory,
                    e
            );
        }
    }

    /**
     * @param volume The instrument's volume, trimmed for this sound file by {@link SoundVolumeTrims}
     */
    private SoundDefinition.Sound trimmedSound(String soundPath, float volume) {
        return sound(soundPath).volume(volume * SoundVolumeTrims.of(soundPath));
    }

    private static Path soundsDirectory(String namespace) {
        return Paths.get(System.getProperty("user.dir"), "..",
                "src", "main", "resources", "assets", namespace, "sounds").normalize();
    }
}