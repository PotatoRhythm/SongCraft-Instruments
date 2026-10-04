package com.stump.songcraft_instruments.datagen;

import com.stump.songcraft_instruments.sound.NoteSound;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.common.data.SoundDefinitionsProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class SoundJsonGenerator extends SoundDefinitionsProvider {

    public SoundJsonGenerator(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, "songcraft_instruments", fileHelper);
    }

    @Override
    public void registerSounds() {
        registerInstrument("windsong_lyre", 20, 0.8f, false, true);

        registerInstrument("vintage_lyre", 20, 0.90f, false, false);

        registerInstrument("floral_zither_new", 20, 0.85f, false, false);
        registerInstrument("floral_zither_old", 20, 0.95f, false, false);

        registerInstrument("ukulele", 20, 0.85f, false, false);

        registerInstrument("djem_djem_drum", 7, 1f, false, false);

        registerInstrument("nightwind_horn", 13, 0.85f, true, true);

        registerInstrument("shamisen", 20, 0.8f, false, true);

        registerInstrument("koto", 20, 0.6f, false, false);

        registerInstrument("pipa_regular", 20, 0.9f, false, false);
        registerInstrument("pipa_tremolo", 20, 1f, false, false);

        registerInstrument("keyboard", 20, 0.8f, false, true);
        registerInstrument("keyboard_gw2", 20, 0.60f, false, true);
        registerInstrument("keyboard_yamaha_c5", 20, 0.55f, false, true);
        registerInstrument("keyboard_heartopia", 20, 0.85f, false, true);
        registerInstrument("keyboard_electric", 20, 1f, false, true);
        registerInstrument("keyboard_harpsichord", 20, 0.6f, false, true);

        registerInstrument("trombone", 20, 0.85f, false, false);
        registerInstrument("trumpet_westgate_studios", 20, 0.65f, true, true);
        registerInstrument("trombone_phgm", 20, 0.85f, true, true);

        registerInstrument("saxophone", 20, 1f, false, false);
        registerInstrument("saxophone_baritone", 20, 0.60f, true, true);
        registerInstrument("saxophone_tenor", 20, 0.25f, true, true);

        registerInstrument("guitar", 20, 0.9f, false, false);
        registerInstrument("guitar_nylon", 20, 0.9f, false, true);
        registerInstrument("guitar_steel", 20, 0.9f, false, true);

        registerInstrument("guitar_clean", 20, 0.6f, false, true);
        registerInstrument("guitar_distortion", 20, 0.8f, true, true);
        registerInstrument("guitar_harmonics", 20, 0.7f, false, true);

        registerInstrument("bass_acoustic", 20, 1.0f, false, true);
        registerInstrument("bass_finger", 20, 1.0f, false, true);
        registerInstrument("bass_slap", 20, 0.9f, false, true);
        registerInstrument("bass_picked", 20, 1.0f, false, true);

        registerInstrument("violin_slow", 20, 0.8f, true, true);
        registerInstrument("violin_fast", 20, 0.5f, true, true);
        registerInstrument("violin_pizzicato", 20, 0.7f, false, true);

        registerInstrument("microphone_irina_brochin", 20, 0.3f, true, true);
        registerInstrument("microphone_bass_choir", 20, 0.7f, true, true);
        registerInstrument("microphone_not_miku", 20, 0.85f, true, true);
        registerInstrument("microphone_not_teto", 20, 0.9f, true, true);
        registerInstrument("microphone_not_teto_sneaky", 20, 1.0f, true, true);

        registerInstrument("gw2_bass", 13, 1f, false, true);
        registerInstrument("gw2_bell", 13, 0.7f, false, true);
        registerInstrument("gw2_harp", 20, 0.8f, false, true);
        registerInstrument("gw2_lute", 20, 0.9f, false, true);
        registerInstrument("gw2_minstrel", 20, 0.8f, false, true);
        registerInstrument("gw2_pell", 20, 0.8f, false, true);
        registerInstrument("gw2_frame_drum", 9, 0.95f, false, true);
        registerInstrument("gw2_horn", 20, 0.95f, true, true);
        registerInstrument("gw2_flute", 13, 0.95f, true, true);
        registerInstrument("gw2_verdarach", 20, 0.5f, true, true);
        registerInstrument("gw2_organ", 20, 0.9f, true, true);
        registerInstrument("gw2_quaggan_organ", 20, 0.9f, true, true);

        add("glorious_drum_don", definition().with(sound("songcraft_instruments:glorious_drum/don").volume(1f)));
        add("glorious_drum_ka", definition().with(sound("songcraft_instruments:glorious_drum/ka").volume(1f)));
        add("glorious_drum_ka_stereo", definition().with(sound("songcraft_instruments:glorious_drum/ka.stereo").volume(1f)));


        float drumset_gw2_V = 0.7f;
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

        float drumset_standard_V = 0.7f;
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

        float drumset_power_V = 0.7f;
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

        float drumset_orchestral_V = 0.7f;
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

        float drumset_cozy_V = 0.7f;
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
     * @param volume            Adjusted volume level
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
                        sound(basePath)
                                .volume(volume)
                                .attenuationDistance(MONO_DISTANCE)
                ));

                if (supportsStereo) {
                    String stereoPath = "songcraft_instruments:" + instrumentName + "/" + i + ".stereo";
                    String stereoEvent = instrumentName + "_note_" + i + "_stereo";

                    // STEREO (attenuated manually, see NoteSound#stereoGain)
                    add(stereoEvent, definition().with(
                            sound(stereoPath).volume(volume)
                    ));
                }
            }
            else {
                String holdPath = "songcraft_instruments:" + instrumentName + "/hold/" + i;
                String holdEvent = instrumentName + "_hold_note_" + i;

                // MONO
                add(holdEvent, definition().with(
                        sound(holdPath)
                                .volume(volume)
                                .attenuationDistance(MONO_DISTANCE)
                ));

                if (supportsStereo) {
                    String holdStereoPath = "songcraft_instruments:" + instrumentName + "/hold/" + i + ".stereo";
                    String holdStereoEvent = instrumentName + "_hold_note_" + i + "_stereo";

                    add(holdStereoEvent, definition().with(
                            sound(holdStereoPath).volume(volume)
                    ));
                }

                String attackPath = "songcraft_instruments:" + instrumentName + "/attack/" + i;
                String attackEvent = instrumentName + "_attack_note_" + i;

                // MONO
                add(attackEvent, definition().with(
                        sound(attackPath)
                                .volume(volume)
                                .attenuationDistance(MONO_DISTANCE)
                ));

                if (supportsStereo) {
                    String attackStereoPath = "songcraft_instruments:" + instrumentName + "/attack/" + i + ".stereo";
                    String attackStereoEvent = instrumentName + "_attack_note_" + i + "_stereo";

                    add(attackStereoEvent, definition().with(
                            sound(attackStereoPath).volume(volume)
                    ));
                }
            }
        }
    }

    private void addSoundVariants(String event, String soundPath, float volume, boolean supportsStereo) {
        String[] parts = soundPath.split(":", 2);

        Path soundDirectory = Paths.get(System.getProperty("user.dir"), "..",
                "src", "main", "resources", "assets", parts[0], "sounds", parts[1]).normalize();

        SoundDefinition monoDefinition = definition();
        SoundDefinition stereoDefinition = supportsStereo ? definition() : null;

        try {
            Files.list(soundDirectory)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".ogg"))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String soundName = fileName.substring(0, fileName.length() - 4);

                        boolean isStereo = soundName.endsWith(".stereo");

                        if (isStereo) {
                            if (!supportsStereo)
                                return;

                            stereoDefinition.with(
                                    sound(soundPath + "/" + soundName)
                                            .volume(volume)
                            );
                        }
                        else {
                            monoDefinition.with(
                                    sound(soundPath + "/" + soundName)
                                            .volume(volume)
                                            .attenuationDistance(MONO_DISTANCE)
                            );
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to parse sound directory: " + soundDirectory,
                    e
            );
        }

        add(event, monoDefinition);

        if (supportsStereo)
            add(event + "_stereo", stereoDefinition);
    }
}