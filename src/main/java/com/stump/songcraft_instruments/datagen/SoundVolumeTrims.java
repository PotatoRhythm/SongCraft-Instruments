package com.stump.songcraft_instruments.datagen;

import java.util.HashMap;
import java.util.Map;

/**
 * Volume trims of individual sound files in dB, relative to their instrument's volume in {@link SoundJsonGenerator}.
 * <p>Generated from loudness measurements (ITU-R BS.1770) of every note, as played through OpenAL:</p>
 * <ul>
 *     <li>Notes that stand out from their neighbours by over 2.5 dB are brought within 1.5 dB of them,
 *     and a pitch tilt steeper than 1.75 dB per octave is flattened to that. Drums keep their recorded balance.</li>
 *     <li>Stereo files whose channels differ are matched to their Mono files (see NoteSound#STEREO_VOLUME).</li>
 *     <li>Files that were re-exported louder from their WAV masters are turned down by the same amount.</li>
 * </ul>
 * Keys are paths inside the instrument's sound folder, without ".ogg". Missing keys are not trimmed.
 */
final class SoundVolumeTrims {
    private static final Map<String, Float> TRIMS = new HashMap<>();

    private SoundVolumeTrims() {}

    /**
     * @param soundPath A sound file location, such as "songcraft_instruments:windsong_lyre/3.stereo"
     * @return The volume multiplier of that sound file
     */
    static float of(final String soundPath) {
        final String path = soundPath.substring(soundPath.indexOf(':') + 1);
        return (float) Math.pow(10, TRIMS.getOrDefault(path, 0f) / 20);
    }

    private static void add(final String instrument, final String trims) {
        for (final String trim : trims.split(" ")) {
            final int split = trim.indexOf('=');
            TRIMS.put(instrument + "/" + trim.substring(0, split), Float.parseFloat(trim.substring(split + 1)));
        }
    }

    static {
        add("bass_acoustic",
                "0.stereo=-0.1 1.stereo=-0.1 2.stereo=-0.1 12.stereo=-0.2 13.stereo=-0.1 14.stereo=-0.1 " +
                "15.stereo=-0.2 16.stereo=-0.5 17.stereo=-1.6 18.stereo=-0.1 19.stereo=-0.4 20.stereo=-0.2");
        add("drumset_cozy",
                "1/0.stereo=-0.3 1/1.stereo=-0.3 1/2.stereo=-0.3 1/3.stereo=-0.3 1/4.stereo=-0.3 3/0.stereo=-0.2 " +
                "3/1.stereo=-0.1 3/3.stereo=-0.1 3/4.stereo=-0.1 7/1.stereo=-0.1");
        add("drumset_gw2",
                "6/0.stereo=-0.1 6/1.stereo=-0.1 6/2.stereo=-0.1 6/3.stereo=-0.1 6/4.stereo=-0.1 7/0.stereo=-0.1 " +
                "7/1.stereo=-0.1 7/2.stereo=-0.1 7/3.stereo=-0.1 7/4.stereo=-0.1 8/0.stereo=-0.1 8/1.stereo=-0.1 " +
                "8/2.stereo=-0.1 8/3.stereo=-0.1 8/4.stereo=-0.1 9/5.stereo=-0.1 9/6.stereo=-0.1 " +
                "10/3.stereo=-0.1 11/1.stereo=-0.2 11/2.stereo=-0.2 11/3.stereo=-0.2 11/4.stereo=-0.1");
        add("drumset_orchestral",
                "0/0=-0.2 0/0.stereo=-0.2 0/1=-0.2 0/1.stereo=-0.2 0/2=-0.2 0/2.stereo=-0.2 0/3=-0.2 " +
                "0/3.stereo=-0.2 0/4=-0.2 0/4.stereo=-0.2 1/0=-0.2 1/0.stereo=-0.9 1/1=-0.2 1/1.stereo=-0.8 " +
                "1/2=-0.2 1/2.stereo=-0.7 1/3=-0.2 1/3.stereo=-0.8 1/4=-0.2 1/4.stereo=-0.8 2/0=-0.2 " +
                "2/0.stereo=-0.2 2/1=-0.2 2/1.stereo=-0.2 2/2=-0.2 2/2.stereo=-0.2 2/3=-0.2 2/3.stereo=-0.2 " +
                "2/4=-0.2 2/4.stereo=-0.2 3/0=-0.2 3/0.stereo=-0.2 3/1=-0.2 3/1.stereo=-0.2 3/2=-0.2 " +
                "3/2.stereo=-0.2 3/3=-0.2 3/3.stereo=-0.2 3/4=-0.2 3/4.stereo=-0.2 4/0=-0.2 4/0.stereo=-0.4 " +
                "4/1=-0.2 4/1.stereo=-0.4 4/2=-0.2 4/2.stereo=-0.4 4/3=-0.2 4/3.stereo=-0.4 4/4=-0.2 " +
                "4/4.stereo=-0.4 5/0=-0.2 5/0.stereo=-0.4 5/1=-0.2 5/1.stereo=-0.4 5/2=-0.2 5/2.stereo=-0.4 " +
                "5/3=-0.2 5/3.stereo=-0.4 5/4=-0.2 5/4.stereo=-0.4 6/0=-0.2 6/0.stereo=-0.4 6/1=-0.2 " +
                "6/1.stereo=-0.4 6/2=-0.2 6/2.stereo=-0.4 6/3=-0.2 6/4=-0.2 6/4.stereo=-0.4 7/0=-0.2 " +
                "7/0.stereo=-1.9 7/1=-0.2 7/1.stereo=-1.7 7/2=-0.2 7/2.stereo=-2.1 7/3=-0.2 7/3.stereo=-1.6 " +
                "7/4=-0.2 7/4.stereo=-1.7 8/0=-0.2 8/0.stereo=-0.2 8/1=-0.2 8/1.stereo=-0.2 8/2=-0.2 " +
                "8/2.stereo=-0.2 8/3=-0.2 8/3.stereo=-0.2 8/4=-0.2 8/4.stereo=-0.2 9/0=-0.2 9/0.stereo=-0.2 " +
                "9/1=-0.2 9/1.stereo=-0.2 9/2=-0.2 9/2.stereo=-0.2 9/3=-0.2 9/3.stereo=-0.2 9/4=-0.2 " +
                "9/4.stereo=-0.2 9/5=-0.2 9/5.stereo=-0.2 9/6=-0.2 9/6.stereo=-0.2 9/7=-0.2 9/7.stereo=-0.2 " +
                "9/8=-0.2 9/8.stereo=-0.2 9/9=-0.2 9/9.stereo=-0.2 10/0=-0.2 10/0.stereo=-0.2 10/1=-0.2 " +
                "10/1.stereo=-0.2 10/2=-0.2 10/2.stereo=-0.2 10/3=-0.2 10/3.stereo=-0.2 10/4=-0.2 " +
                "10/4.stereo=-0.2 11/0=-0.2 11/0.stereo=-0.4 11/1=-0.2 11/1.stereo=-0.5 11/2=-0.2 " +
                "11/2.stereo=-0.5 11/3=-0.2 11/3.stereo=-0.5 11/4=-0.2 11/4.stereo=-0.5");
        add("drumset_power",
                "0/0=-1.0 0/0.stereo=-1.0 0/1=-1.0 0/1.stereo=-1.0 0/2=-1.0 0/2.stereo=-1.0 0/3=-1.0 " +
                "0/3.stereo=-1.0 0/4=-1.0 0/4.stereo=-1.0 1/0=-1.0 1/0.stereo=-1.0 1/1=-1.0 1/1.stereo=-1.0 " +
                "1/2=-1.0 1/2.stereo=-1.0 1/3=-1.0 1/3.stereo=-1.0 1/4=-1.0 1/4.stereo=-1.0 2/0=-1.0 " +
                "2/0.stereo=-1.0 2/1=-1.0 2/1.stereo=-1.0 2/2=-1.0 2/2.stereo=-1.0 2/3=-1.0 2/3.stereo=-1.0 " +
                "2/4=-1.0 2/4.stereo=-1.0 3/0=-1.0 3/0.stereo=-1.0 3/1=-1.0 3/1.stereo=-1.0 3/2=-1.0 " +
                "3/2.stereo=-1.0 3/3=-1.0 3/3.stereo=-1.0 3/4=-1.0 3/4.stereo=-1.0 4/0=-1.0 4/0.stereo=-1.0 " +
                "4/1=-1.0 4/1.stereo=-1.0 4/2=-1.0 4/2.stereo=-1.0 4/3=-1.0 4/3.stereo=-1.0 4/4=-1.0 " +
                "4/4.stereo=-1.0 5/0=-1.0 5/0.stereo=-1.0 5/1=-1.0 5/1.stereo=-1.0 5/2=-1.0 5/2.stereo=-1.0 " +
                "5/3=-1.0 5/3.stereo=-1.0 5/4=-1.0 5/4.stereo=-1.0 6/0=-1.0 6/0.stereo=-1.0 6/1=-1.0 " +
                "6/1.stereo=-1.0 6/2=-1.0 6/2.stereo=-1.0 6/3=-1.0 6/3.stereo=-1.0 6/4=-1.0 6/4.stereo=-1.0 " +
                "7/0=-1.0 7/1=-1.0 7/2=-1.0 7/2.stereo=-0.1 7/3=-1.0 7/4=-1.0 7/4.stereo=-0.1 8/0=-1.0 " +
                "8/0.stereo=-1.0 8/1=-1.0 8/1.stereo=-1.0 8/2=-1.0 8/2.stereo=-1.0 8/3=-1.0 8/3.stereo=-1.0 " +
                "8/4=-1.0 8/4.stereo=-1.0 9/0=-1.0 9/0.stereo=-1.0 9/1=-1.0 9/1.stereo=-1.0 9/2=-1.0 " +
                "9/2.stereo=-1.0 9/3=-1.0 9/3.stereo=-1.0 9/4=-1.0 9/4.stereo=-1.0 9/5=-1.0 9/5.stereo=-1.0 " +
                "9/6=-1.0 9/6.stereo=-1.0 9/7=-1.0 9/7.stereo=-1.0 9/8=-1.0 9/8.stereo=-1.0 9/9=-1.0 " +
                "9/9.stereo=-1.0 10/0=-1.0 10/0.stereo=-0.1 10/1=-1.0 10/1.stereo=-0.3 10/2=-1.0 " +
                "10/2.stereo=-0.6 10/3=-1.0 10/3.stereo=-0.2 10/4=-1.0 10/4.stereo=-0.7 11/0=-1.0 " +
                "11/0.stereo=-1.0 11/1=-1.0 11/1.stereo=-1.0 11/2=-1.0 11/2.stereo=-1.0 11/3=-1.0 " +
                "11/3.stereo=-1.0 11/4=-1.0 11/4.stereo=-1.0");
        add("drumset_standard",
                "0/0=-1.7 0/0.stereo=-1.7 0/1=-1.7 0/1.stereo=-1.7 0/2=-1.7 0/2.stereo=-1.4 0/3=-1.7 " +
                "0/3.stereo=-1.7 0/4=-1.7 0/4.stereo=-1.4 1/0=-1.7 1/0.stereo=-1.6 1/1=-1.7 1/1.stereo=-1.6 " +
                "1/2=-1.7 1/2.stereo=-1.7 1/3=-1.7 1/3.stereo=-1.6 1/4=-1.7 1/4.stereo=-1.7 2/0=-1.7 " +
                "2/0.stereo=-1.6 2/1=-1.7 2/1.stereo=-1.7 2/2=-1.7 2/2.stereo=-1.7 2/3=-1.7 2/3.stereo=-1.7 " +
                "2/4=-1.7 2/4.stereo=-1.6 3/0=-1.7 3/0.stereo=-1.7 3/1=-1.7 3/1.stereo=-1.6 3/2=-1.7 " +
                "3/2.stereo=-1.7 3/3=-1.7 3/3.stereo=-1.6 3/4=-1.7 3/4.stereo=-1.7 4/0=-1.7 4/0.stereo=-1.7 " +
                "4/1=-1.7 4/1.stereo=-1.7 4/2=-1.7 4/2.stereo=-1.7 4/3=-1.7 4/3.stereo=-1.6 4/4=-1.7 " +
                "4/4.stereo=-1.6 5/0=-1.7 5/0.stereo=-1.7 5/1=-1.7 5/1.stereo=-1.7 5/2=-1.7 5/2.stereo=-1.7 " +
                "5/3=-1.7 5/3.stereo=-1.7 5/4=-1.7 5/4.stereo=-1.7 6/0=-1.7 6/0.stereo=-1.6 6/1=-1.7 " +
                "6/1.stereo=-1.7 6/2=-1.7 6/2.stereo=-1.6 6/3=-1.7 6/3.stereo=-1.7 6/4=-1.7 6/4.stereo=-1.7 " +
                "7/0=-1.7 7/0.stereo=-1.5 7/1=-1.7 7/1.stereo=-1.3 7/2=-1.7 7/2.stereo=-1.3 7/3=-1.7 " +
                "7/3.stereo=-1.3 7/4=-1.7 7/4.stereo=-1.3 8/0=-1.7 8/0.stereo=-1.7 8/1=-1.7 8/1.stereo=-1.6 " +
                "8/2=-1.7 8/2.stereo=-1.7 8/3=-1.7 8/3.stereo=-1.6 8/4=-1.7 8/4.stereo=-1.6 9/0=-1.7 " +
                "9/0.stereo=-1.7 9/1=-1.7 9/1.stereo=-1.6 9/2=-1.7 9/2.stereo=-1.7 9/3=-1.7 9/3.stereo=-1.7 " +
                "9/4=-1.7 9/4.stereo=-1.7 9/5=-1.7 9/5.stereo=-1.6 9/6=-1.7 9/6.stereo=-1.6 9/7=-1.7 " +
                "9/7.stereo=-1.7 9/8=-1.7 9/8.stereo=-1.6 9/9=-1.7 9/9.stereo=-1.7 10/0=-1.7 10/1=-1.7 " +
                "10/1.stereo=-0.7 10/2=-1.7 10/2.stereo=-0.7 10/3=-1.7 10/3.stereo=-0.6 10/4=-1.7 " +
                "10/4.stereo=-0.7 11/0=-1.7 11/0.stereo=-1.7 11/1=-1.7 11/1.stereo=-1.7 11/2=-1.7 " +
                "11/2.stereo=-1.7 11/3=-1.7 11/3.stereo=-1.6 11/4=-1.7 11/4.stereo=-1.6");
        add("ff14_clarinet",
                "attack/3=-0.1 attack/4=-0.1 attack/5=-0.1 attack/6=-0.1 attack/7=-0.2 attack/8=-0.2 " +
                "attack/9=-0.2 attack/10=-0.2 attack/11=-0.2 attack/12=-0.3 attack/13=-0.3 attack/14=-0.3 " +
                "attack/15=-0.3 attack/16=-0.4 attack/17=-0.4 attack/18=-0.4 attack/19=-0.4 attack/20=-0.4 " +
                "hold/3=-0.1 hold/4=-0.1 hold/5=-0.1 hold/6=-0.1 hold/7=-0.2 hold/8=-0.2 hold/9=-0.2 " +
                "hold/10=-0.2 hold/11=-0.2 hold/12=-0.3 hold/13=-0.3 hold/14=-0.3 hold/15=-0.3 hold/16=-0.4 " +
                "hold/17=-0.4 hold/18=-0.4 hold/19=-0.4 hold/20=-0.4");
        add("ff14_clean_guitar",
                "attack/0=-0.9 attack/1=-0.8 attack/2=-0.8 attack/3=-0.7 attack/4=-0.7 attack/5=-2.0 " +
                "attack/6=-0.6 attack/7=-0.6 attack/8=-0.5 attack/9=-0.5 attack/10=-0.4 attack/11=-0.4 " +
                "attack/12=-0.3 attack/13=-0.3 attack/14=-0.3 attack/15=-0.2 attack/16=-0.2 attack/17=-0.1 " +
                "attack/18=-0.1 hold/0=-0.9 hold/1=-0.8 hold/2=-0.8 hold/3=-0.7 hold/4=-0.7 hold/5=-2.0 " +
                "hold/6=-0.6 hold/7=-0.6 hold/8=-0.5 hold/9=-0.5 hold/10=-0.4 hold/11=-0.4 hold/12=-0.3 " +
                "hold/13=-0.3 hold/14=-0.3 hold/15=-0.2 hold/16=-0.2 hold/17=-0.1 hold/18=-0.1");
        add("ff14_fiddle",
                "0=-1.4 1=-1.4 2=-1.4 3=-1.4 4=-1.4 5=-1.4 6=-1.4 7=-1.4 8=-1.4 9=-1.4 10=-1.4 11=-1.4 13=-1.4 " +
                "14=-1.4 15=-1.4 16=-1.4 17=-1.4 18=-1.4 19=-1.4 20=-1.4");
        add("ff14_flute",
                "attack/1=-0.1 attack/2=-0.2 attack/3=-0.3 attack/4=-0.4 attack/5=-0.5 attack/6=-0.7 " +
                "attack/7=-0.8 attack/8=-0.9 attack/9=-1.0 attack/10=-1.1 attack/11=-1.2 attack/12=-1.3 " +
                "attack/13=-1.4 attack/14=-1.5 attack/15=-1.6 attack/16=-1.7 attack/17=-1.9 attack/18=-2.0 " +
                "attack/19=-2.1 attack/20=-2.2 hold/1=-0.1 hold/2=-0.2 hold/3=-0.3 hold/4=-0.4 hold/5=-0.5 " +
                "hold/6=-0.7 hold/7=-0.8 hold/8=-0.9 hold/9=-1.0 hold/10=-1.1 hold/11=-1.2 hold/12=-1.3 " +
                "hold/13=-1.4 hold/14=-1.5 hold/15=-1.6 hold/16=-1.7 hold/17=-1.9 hold/18=-2.0 hold/19=-2.1 " +
                "hold/20=-2.2");
        add("ff14_oboe",
                "attack/5=-0.1 attack/6=-0.2 attack/7=-0.2 attack/8=-0.3 attack/9=-0.3 attack/10=-0.4 " +
                "attack/11=-0.4 attack/12=-0.5 attack/13=-0.6 attack/14=-0.6 attack/15=-0.7 attack/16=-0.8 " +
                "attack/17=-0.8 attack/18=-0.9 attack/19=-0.9 attack/20=-1.0 hold/5=-0.1 hold/6=-0.2 hold/7=-0.2 " +
                "hold/8=-0.3 hold/9=-0.3 hold/10=-0.4 hold/11=-0.4 hold/12=-0.5 hold/13=-0.6 hold/14=-0.6 " +
                "hold/15=-0.7 hold/16=-0.8 hold/17=-0.8 hold/18=-0.9 hold/19=-0.9 hold/20=-1.0");
        add("ff14_special_guitar",
                "0=-2.7 1=-2.7 2=-2.7 3=-2.7 4=-2.7 5=-2.7 6=-2.7 7=-2.7 8=-2.7 9=-2.7 10=-2.7 11=-5.5 13=-2.7 " +
                "14=-2.7 15=-2.7 16=-2.7 17=-2.7 18=-2.7 19=-2.7 20=-2.7");
        add("ff14_viola",
                "attack/0=-1.5 attack/1=-1.5 attack/2=-1.5 attack/3=-1.5 attack/4=-1.5 attack/5=-1.5 " +
                "attack/6=-1.5 attack/7=-1.5 attack/8=-0.4 attack/9=-2.6 attack/10=-1.5 attack/11=-1.5 " +
                "attack/12=-2.9 attack/14=-1.5 attack/15=-1.5 attack/16=-1.5 attack/17=-1.5 attack/18=-1.5 " +
                "attack/19=-1.5 attack/20=-1.5 hold/0=-1.5 hold/1=-1.5 hold/2=-1.5 hold/3=-1.5 hold/4=-1.5 " +
                "hold/5=-1.5 hold/6=-1.5 hold/7=-1.5 hold/8=-0.4 hold/9=-2.6 hold/10=-1.5 hold/11=-1.5 " +
                "hold/12=-2.9 hold/14=-1.5 hold/15=-1.5 hold/16=-1.5 hold/17=-1.5 hold/18=-1.5 hold/19=-1.5 " +
                "hold/20=-1.5");
        add("floral_zither_new",
                "0=-0.2 6=-0.7");
        add("glorious_drum",
                "don=-5.9 ka=-5.9");
        add("guitar_clean",
                "0=-3.1 0.stereo=-3.1 1=-2.9 1.stereo=-3.0 2=-2.8 2.stereo=-2.8 3=-2.6 3.stereo=-2.6 4=-2.5 " +
                "4.stereo=-2.5 5=-2.3 5.stereo=-2.4 6=-2.1 6.stereo=-2.2 7=-2.0 7.stereo=-2.0 8=-4.1 " +
                "8.stereo=-4.2 9=-1.7 9.stereo=-1.7 10=-1.5 10.stereo=-1.5 11=-1.4 11.stereo=-1.4 12=-1.2 " +
                "12.stereo=-1.3 13=-1.1 13.stereo=-1.1 14=-0.9 14.stereo=-1.0 15=-0.8 15.stereo=-0.9 16=-0.6 " +
                "16.stereo=-0.6 17=-0.5 17.stereo=-0.5 18=-0.3 18.stereo=-0.4 19=-0.2 19.stereo=-0.2");
        add("gw2_bass",
                "0=-3.3 0.stereo=-3.5 1=-3.3 1.stereo=-3.4 2=-3.3 2.stereo=-3.4 3=-3.3 3.stereo=-3.8 4=-3.3 " +
                "4.stereo=-3.8 5=-3.3 5.stereo=-3.5 6=-3.3 6.stereo=-3.5 7=-3.3 7.stereo=-3.6 8=-3.3 " +
                "8.stereo=-3.4 9=-3.3 9.stereo=-3.3 10=-3.3 10.stereo=-3.4 11=-3.3 11.stereo=-3.4 12.stereo=-0.2 " +
                "13=-2.2 13.stereo=-2.6");
        add("gw2_bell",
                "0=-0.6 0.stereo=-0.6 1=-0.6 1.stereo=-0.6 2=-0.5 2.stereo=-0.5 3=-0.5 3.stereo=-0.5 4=-0.4 " +
                "4.stereo=-0.4 5=-0.4 5.stereo=-0.4 6=-0.3 6.stereo=-0.3 7=-0.3 7.stereo=-0.3 8=-0.2 " +
                "8.stereo=-0.2 9=-0.2 9.stereo=-0.2 10=-0.1 10.stereo=-0.1 11=-0.1 11.stereo=-0.1");
        add("gw2_flute",
                "attack/0=-2.0 attack/0.stereo=-2.0 attack/1=-2.0 attack/1.stereo=-2.0 attack/2=-2.0 " +
                "attack/2.stereo=-2.0 attack/3=-2.0 attack/3.stereo=-2.0 attack/4=-2.0 attack/4.stereo=-2.0 " +
                "attack/5=-2.0 attack/5.stereo=-2.0 attack/6=-2.0 attack/6.stereo=-2.0 attack/7=-2.0 " +
                "attack/7.stereo=-2.0 attack/8=-0.3 attack/8.stereo=-0.4 attack/9=-2.0 attack/9.stereo=-2.0 " +
                "attack/10=-3.9 attack/10.stereo=-3.9 attack/11=-0.7 attack/11.stereo=-1.3 attack/12.stereo=-0.5 " +
                "attack/13=-2.0 attack/13.stereo=-2.1 hold/0=-2.0 hold/0.stereo=-2.0 hold/1=-2.0 " +
                "hold/1.stereo=-2.0 hold/2=-2.0 hold/2.stereo=-2.0 hold/3=-2.0 hold/3.stereo=-2.0 hold/4=-2.0 " +
                "hold/4.stereo=-2.0 hold/5=-2.0 hold/5.stereo=-2.0 hold/6=-2.0 hold/6.stereo=-2.0 hold/7=-2.0 " +
                "hold/7.stereo=-2.0 hold/8=-0.3 hold/8.stereo=-0.4 hold/9=-2.0 hold/9.stereo=-2.0 hold/10=-3.9 " +
                "hold/10.stereo=-3.9 hold/11=-0.7 hold/11.stereo=-1.3 hold/12.stereo=-0.5 hold/13=-2.0 " +
                "hold/13.stereo=-2.1");
        add("gw2_frame_drum",
                "0.stereo=-1.4 1.stereo=-1.1 2.stereo=-0.8 3.stereo=-0.6 4.stereo=-3.1 5.stereo=-0.5 " +
                "6.stereo=-0.7 7.stereo=-1.0 8.stereo=-0.8 9.stereo=-2.5");
        add("gw2_harp",
                "0=-2.3 0.stereo=-3.7 1=-2.3 1.stereo=-3.4 2=-2.3 2.stereo=-3.4 3=-2.3 3.stereo=-2.7 4=-2.3 " +
                "4.stereo=-2.7 5=-4.9 5.stereo=-5.6 6.stereo=-2.8 7.stereo=-2.9 8=-2.3 8.stereo=-2.8 9=-2.3 " +
                "9.stereo=-2.8 10=-2.3 10.stereo=-2.9 11=-2.3 11.stereo=-2.9 12=-0.3 12.stereo=-4.5 13=-2.3 " +
                "13.stereo=-3.7 14=-2.3 14.stereo=-3.7 15=-2.3 15.stereo=-3.6 16=-2.3 16.stereo=-3.6 17=-2.3 " +
                "17.stereo=-6.7 18=-2.3 18.stereo=-6.7 19=-2.3 19.stereo=-3.0 20=-2.3 20.stereo=-3.9");
        add("gw2_horn",
                "attack/10=-1.1 attack/10.stereo=-1.1 attack/11=-1.7 attack/11.stereo=-1.7 hold/10=-1.1 " +
                "hold/10.stereo=-1.1 hold/11=-1.7 hold/11.stereo=-1.7");
        add("gw2_lute",
                "14=-0.3 14.stereo=-0.3");
        add("gw2_minstrel",
                "0=-4.2 0.stereo=-6.5 1=-4.2 1.stereo=-5.4 2=-4.2 2.stereo=-5.4 3=-4.2 3.stereo=-4.6 4=-4.2 " +
                "4.stereo=-4.6 5=-6.7 5.stereo=-7.6 6=-0.5 6.stereo=-5.1 7.stereo=-4.7 8=-5.6 8.stereo=-6.1 " +
                "9=-4.2 9.stereo=-4.7 10=-4.2 10.stereo=-4.8 11=-4.2 11.stereo=-4.9 12=-1.8 12.stereo=-6.7 " +
                "13=-4.2 13.stereo=-5.9 14=-4.2 14.stereo=-5.9 15=-4.2 15.stereo=-5.0 16=-5.7 16.stereo=-6.7 " +
                "17=-2.0 17.stereo=-6.6 18=-2.9 18.stereo=-7.5 19=-5.8 19.stereo=-6.9 20=-4.2 20.stereo=-6.2");
        add("gw2_organ",
                "attack/0=-1.5 attack/0.stereo=-1.5 attack/1=-1.5 attack/1.stereo=-1.5 attack/2=-1.5 " +
                "attack/2.stereo=-1.8 attack/3=-1.5 attack/3.stereo=-1.7 attack/4=-1.5 attack/4.stereo=-1.5 " +
                "attack/5=-1.5 attack/5.stereo=-1.5 attack/6=-1.5 attack/6.stereo=-1.8 attack/7=-1.5 " +
                "attack/7.stereo=-1.5 attack/8=-1.5 attack/8.stereo=-1.5 attack/9=-1.5 attack/9.stereo=-1.7 " +
                "attack/10=-1.5 attack/10.stereo=-1.7 attack/11=-1.5 attack/11.stereo=-1.5 attack/12=-1.5 " +
                "attack/12.stereo=-1.6 attack/13=-1.5 attack/13.stereo=-1.5 attack/14.stereo=-0.1 attack/15=-1.5 " +
                "attack/15.stereo=-1.6 attack/16=-2.8 attack/16.stereo=-2.9 attack/17=-0.4 attack/17.stereo=-0.6 " +
                "attack/18=-1.5 attack/18.stereo=-1.6 attack/19=-1.5 attack/19.stereo=-1.6 attack/20=-1.5 " +
                "attack/20.stereo=-1.8 hold/0=-1.5 hold/0.stereo=-1.5 hold/1=-1.5 hold/1.stereo=-1.5 hold/2=-1.5 " +
                "hold/2.stereo=-1.8 hold/3=-1.5 hold/3.stereo=-1.7 hold/4=-1.5 hold/4.stereo=-1.5 hold/5=-1.5 " +
                "hold/5.stereo=-1.5 hold/6=-1.5 hold/6.stereo=-1.8 hold/7=-1.5 hold/7.stereo=-1.5 hold/8=-1.5 " +
                "hold/8.stereo=-1.5 hold/9=-1.5 hold/9.stereo=-1.7 hold/10=-1.5 hold/10.stereo=-1.7 hold/11=-1.5 " +
                "hold/11.stereo=-1.5 hold/12=-1.5 hold/12.stereo=-1.6 hold/13=-1.5 hold/13.stereo=-1.5 " +
                "hold/14.stereo=-0.1 hold/15=-1.5 hold/15.stereo=-1.6 hold/16=-2.8 hold/16.stereo=-2.9 " +
                "hold/17=-0.4 hold/17.stereo=-0.6 hold/18=-1.5 hold/18.stereo=-1.6 hold/19=-1.5 " +
                "hold/19.stereo=-1.6 hold/20=-1.5 hold/20.stereo=-1.8");
        add("gw2_pell",
                "2.stereo=-0.1 3.stereo=-0.2 4.stereo=-0.2 5.stereo=-0.3 6.stereo=-0.1 7.stereo=-0.1 " +
                "8.stereo=-0.1 9=-1.8 9.stereo=-1.8 10.stereo=-0.3 11.stereo=-0.5 12.stereo=-0.1 13.stereo=-0.3 " +
                "14.stereo=-0.1 15=-1.1 15.stereo=-1.2 16.stereo=-0.1 18.stereo=-0.7 19.stereo=-1.1 " +
                "20.stereo=-1.8");
        add("gw2_quaggan_organ",
                "attack/0=-9.3 attack/0.stereo=-9.7 attack/1=-5.0 attack/1.stereo=-5.4 attack/2=-7.3 " +
                "attack/2.stereo=-7.7 attack/3=-7.3 attack/3.stereo=-7.6 attack/4=-7.3 attack/4.stereo=-7.6 " +
                "attack/5=-7.3 attack/5.stereo=-7.9 attack/6=-5.6 attack/6.stereo=-6.6 attack/7=-7.3 " +
                "attack/7.stereo=-7.6 attack/8=-10.9 attack/8.stereo=-11.2 attack/9=-5.8 attack/9.stereo=-6.2 " +
                "attack/10=-4.9 attack/10.stereo=-5.5 attack/11=-9.6 attack/11.stereo=-10.1 attack/12=-7.3 " +
                "attack/12.stereo=-7.7 attack/13=-4.6 attack/13.stereo=-5.5 attack/14=-7.3 attack/14.stereo=-7.6 " +
                "attack/15=-10.7 attack/15.stereo=-11.0 attack/16=-7.3 attack/16.stereo=-7.5 attack/17=-5.6 " +
                "attack/17.stereo=-6.0 attack/18=-9.8 attack/18.stereo=-10.1 attack/19=-7.3 " +
                "attack/19.stereo=-7.5 attack/20.stereo=-0.3 hold/0=-9.3 hold/0.stereo=-9.7 hold/1=-5.0 " +
                "hold/1.stereo=-5.4 hold/2=-7.3 hold/2.stereo=-7.7 hold/3=-7.3 hold/3.stereo=-7.6 hold/4=-7.3 " +
                "hold/4.stereo=-7.6 hold/5=-7.3 hold/5.stereo=-7.9 hold/6=-5.6 hold/6.stereo=-6.6 hold/7=-7.3 " +
                "hold/7.stereo=-7.6 hold/8=-10.9 hold/8.stereo=-11.2 hold/9=-5.8 hold/9.stereo=-6.2 hold/10=-4.9 " +
                "hold/10.stereo=-5.5 hold/11=-9.6 hold/11.stereo=-10.1 hold/12=-7.3 hold/12.stereo=-7.7 " +
                "hold/13=-4.6 hold/13.stereo=-5.5 hold/14=-7.3 hold/14.stereo=-7.6 hold/15=-10.7 " +
                "hold/15.stereo=-11.0 hold/16=-7.3 hold/16.stereo=-7.5 hold/17=-5.6 hold/17.stereo=-6.0 " +
                "hold/18=-9.8 hold/18.stereo=-10.1 hold/19=-7.3 hold/19.stereo=-7.5 hold/20.stereo=-0.3");
        add("gw2_verdarach",
                "attack/0.stereo=-0.3 attack/1.stereo=-0.3 attack/2.stereo=-0.4 attack/3.stereo=-0.4 " +
                "attack/4.stereo=-0.4 attack/5.stereo=-0.2 attack/6.stereo=-0.4 attack/7.stereo=-0.4 " +
                "attack/8.stereo=-0.2 attack/9.stereo=-0.2 attack/10.stereo=-0.2 attack/11.stereo=-0.2 " +
                "attack/12.stereo=-0.2 attack/13.stereo=-0.5 attack/14.stereo=-0.5 attack/15.stereo=-0.4 " +
                "attack/16.stereo=-0.4 attack/17.stereo=-0.7 attack/18.stereo=-0.6 attack/19.stereo=-0.2 " +
                "attack/20.stereo=-0.4 hold/0.stereo=-0.3 hold/1.stereo=-0.3 hold/2.stereo=-0.4 " +
                "hold/3.stereo=-0.4 hold/4.stereo=-0.4 hold/5.stereo=-0.2 hold/6.stereo=-0.4 hold/7.stereo=-0.4 " +
                "hold/8.stereo=-0.2 hold/9.stereo=-0.2 hold/10.stereo=-0.2 hold/11.stereo=-0.2 " +
                "hold/12.stereo=-0.2 hold/13.stereo=-0.5 hold/14.stereo=-0.5 hold/15.stereo=-0.4 " +
                "hold/16.stereo=-0.4 hold/17.stereo=-0.7 hold/18.stereo=-0.6 hold/19.stereo=-0.2 " +
                "hold/20.stereo=-0.4");
        add("heartopia_acoustic_bass",
                "13.stereo=-0.6");
        add("heartopia_bagpipes",
                "attack/2=-2.4 attack/2.stereo=-1.0 attack/3=-3.6 attack/3.stereo=-2.9 attack/4=-0.6 " +
                "attack/4.stereo=-1.0 attack/5=-0.6 attack/5.stereo=-0.6 attack/6=-0.6 attack/6.stereo=-1.3 " +
                "attack/7=-0.6 attack/7.stereo=-0.2 attack/9=-0.6 attack/10=-0.6 attack/11=-0.6 attack/12=-0.6 " +
                "attack/13=-0.6 attack/13.stereo=-3.1 hold/2=-2.4 hold/2.stereo=-1.0 hold/3=-3.6 " +
                "hold/3.stereo=-2.9 hold/4=-0.6 hold/4.stereo=-1.0 hold/5=-0.6 hold/5.stereo=-0.6 hold/6=-0.6 " +
                "hold/6.stereo=-1.3 hold/7=-0.6 hold/7.stereo=-0.2 hold/9=-0.6 hold/10=-0.6 hold/11=-0.6 " +
                "hold/12=-0.6 hold/13=-0.6 hold/13.stereo=-3.1");
        add("heartopia_cajon",
                "0=-0.4 0.stereo=-0.4 1=-0.4 1.stereo=-0.2 2=-0.4 2.stereo=-0.2 3=-0.4 3.stereo=-0.7 4=-0.4 " +
                "4.stereo=-0.2 5=-0.4 6=-0.4 6.stereo=-0.3 7=-0.4 7.stereo=-0.4");
        add("heartopia_cello",
                "attack/0=-2.3 attack/0.stereo=-2.9 attack/1=-0.7 attack/1.stereo=-2.3 attack/2=-2.3 " +
                "attack/2.stereo=-1.7 attack/3=-2.3 attack/3.stereo=-3.0 attack/4=-5.7 attack/4.stereo=-5.3 " +
                "attack/5=-2.3 attack/5.stereo=-1.3 attack/6.stereo=-1.3 attack/7=-2.3 attack/7.stereo=-2.3 " +
                "attack/8=-3.8 attack/8.stereo=-3.0 attack/9=-2.3 attack/9.stereo=-1.8 attack/10=-2.3 " +
                "attack/10.stereo=-3.3 attack/11=-3.6 attack/11.stereo=-3.2 attack/12.stereo=-0.6 attack/13=-3.6 " +
                "attack/13.stereo=-3.2 hold/0=-2.3 hold/0.stereo=-2.9 hold/1=-0.7 hold/1.stereo=-2.3 hold/2=-2.3 " +
                "hold/2.stereo=-1.7 hold/3=-2.3 hold/3.stereo=-3.0 hold/4=-5.7 hold/4.stereo=-5.3 hold/5=-2.3 " +
                "hold/5.stereo=-1.3 hold/6.stereo=-1.3 hold/7=-2.3 hold/7.stereo=-2.3 hold/8=-3.8 " +
                "hold/8.stereo=-3.0 hold/9=-2.3 hold/9.stereo=-1.8 hold/10=-2.3 hold/10.stereo=-3.3 hold/11=-3.6 " +
                "hold/11.stereo=-3.2 hold/12.stereo=-0.6 hold/13=-3.6 hold/13.stereo=-3.2");
        add("heartopia_concertina",
                "attack/0=-1.9 attack/0.stereo=-1.0 attack/2=-0.5 attack/2.stereo=-0.1 attack/3=-0.7 " +
                "attack/4=-0.9 attack/4.stereo=-0.2 attack/6=-1.3 attack/7=-1.5 attack/8=-4.1 " +
                "attack/8.stereo=-1.7 attack/9=-1.9 attack/10=-2.1 attack/11=-0.9 attack/11.stereo=-1.1 " +
                "attack/12.stereo=-1.2 attack/13=-5.4 attack/13.stereo=-4.2 hold/0=-1.9 hold/0.stereo=-1.0 " +
                "hold/2=-0.5 hold/2.stereo=-0.1 hold/3=-0.7 hold/4=-0.9 hold/4.stereo=-0.2 hold/6=-1.3 " +
                "hold/7=-1.5 hold/8=-4.1 hold/8.stereo=-1.7 hold/9=-1.9 hold/10=-2.1 hold/11=-0.9 " +
                "hold/11.stereo=-1.1 hold/13=-5.4 hold/13.stereo=-4.2");
        add("heartopia_conch_shells",
                "attack/0=-0.1 attack/1=-0.4 attack/1.stereo=-0.2 attack/2=-3.1 attack/2.stereo=-3.2 " +
                "attack/3=-1.0 attack/3.stereo=-0.9 attack/4=-1.3 attack/4.stereo=-1.4 attack/5=-1.7 " +
                "attack/5.stereo=-1.3 attack/6=-2.0 attack/6.stereo=-1.5 hold/0=-0.1 hold/1=-0.4 " +
                "hold/1.stereo=-0.2 hold/2=-3.1 hold/2.stereo=-3.2 hold/3=-1.0 hold/3.stereo=-0.9 hold/4=-1.3 " +
                "hold/4.stereo=-1.4 hold/5=-1.7 hold/5.stereo=-1.3 hold/6=-2.0 hold/6.stereo=-1.5");
        add("heartopia_conga_bongos",
                "0=-0.4 1=-0.4 1.stereo=-1.0 2=-0.4 2.stereo=-0.5 3=-0.4 3.stereo=-1.3 4=-0.4 4.stereo=-0.8 " +
                "5=-0.4 5.stereo=-0.8 6=-0.4 6.stereo=-0.4 7=-0.4 7.stereo=-0.4");
        add("heartopia_hang",
                "0=-3.0 0.stereo=-3.9 1=-5.1 1.stereo=-5.9 2=-6.1 2.stereo=-7.7 3=-5.6 3.stereo=-5.2 4=-3.0 " +
                "4.stereo=-4.7 5=-1.7 5.stereo=-1.9 6=-2.0 6.stereo=-6.2 7=-7.2 7.stereo=-6.3 8=-2.7 " +
                "8.stereo=-5.2 9=-5.2 9.stereo=-4.1 10=-0.9 10.stereo=-2.5 11=-1.6 11.stereo=-1.6 12=-1.3 " +
                "12.stereo=-1.0 13=-0.9");
        add("heartopia_harp",
                "0=-2.5 0.stereo=-2.5 1=-2.4 1.stereo=-2.5 2=-2.3 2.stereo=-2.4 3=-2.2 3.stereo=-2.3 4=-2.2 " +
                "4.stereo=-2.2 5=-3.6 5.stereo=-3.7 6=-2.0 6.stereo=-2.1 7=-0.9 7.stereo=-1.0 8=-1.8 " +
                "8.stereo=-1.9 9=-4.0 9.stereo=-4.1 10=-1.7 10.stereo=-1.8 12=-1.5 12.stereo=-1.6 13=-5.0 " +
                "13.stereo=-5.0 14=-2.6 14.stereo=-2.7 17=-1.2 17.stereo=-1.3 18=-1.1 18.stereo=-1.1 19=-1.0 " +
                "19.stereo=-0.9 20=-0.9");
        add("heartopia_kalimba",
                "0=-1.5 0.stereo=-2.2 1=-3.4 1.stereo=-1.0 2=-3.4 2.stereo=-5.1 3=-3.4 3.stereo=-3.2 4=-3.4 " +
                "4.stereo=-1.8 5=-3.4 5.stereo=-2.4 6=-5.9 6.stereo=-4.1 7=-1.6 7.stereo=-3.9 8=-2.0 " +
                "8.stereo=-4.2 9=-6.6 9.stereo=-3.8 10=-3.4 10.stereo=-5.0 11.stereo=-6.3 12=-3.4 12.stereo=-1.0 " +
                "13=-6.6 13.stereo=-4.7");
        add("heartopia_lute",
                "0=-2.4 0.stereo=-3.3 1=-2.4 1.stereo=-3.3 2=-1.2 2.stereo=-1.1 3=-2.4 3.stereo=-2.3 4=-2.4 " +
                "4.stereo=-3.1 5=-2.4 5.stereo=-3.7 6.stereo=-3.9 7=-2.4 7.stereo=-2.8 8=-2.4 8.stereo=-2.7 " +
                "9=-2.4 9.stereo=-2.6 10=-2.4 10.stereo=-3.0 11=-4.9 11.stereo=-4.2 12=-0.2 12.stereo=-1.5 " +
                "13=-0.7 13.stereo=-3.1");
        add("heartopia_lyre",
                "0=-2.4 0.stereo=-2.1 1=-2.4 1.stereo=-2.9 2=-2.4 2.stereo=-2.0 3=-2.4 3.stereo=-2.0 4=-2.4 " +
                "4.stereo=-2.0 5=-2.4 5.stereo=-2.0 6=-2.4 6.stereo=-1.8 7=-2.4 7.stereo=-1.5 8=-5.4 " +
                "8.stereo=-5.5 9=-2.4 9.stereo=-2.2 10=-2.4 10.stereo=-2.7 11=-0.2 12=-0.2 12.stereo=-0.4 " +
                "13=-3.8 13.stereo=-4.6");
        add("heartopia_ocarina",
                "attack/0.stereo=-0.8 attack/1=-1.5 attack/1.stereo=-1.1 attack/2=-1.6 attack/2.stereo=-0.3 " +
                "attack/3=-1.6 attack/3.stereo=-1.1 attack/4.stereo=-1.3 attack/5=-1.7 attack/5.stereo=-0.6 " +
                "attack/6=-1.7 attack/6.stereo=-1.9 attack/7=-1.8 attack/7.stereo=-1.8 attack/8.stereo=-0.4 " +
                "attack/9=-1.8 attack/9.stereo=-1.7 attack/10=-1.9 attack/10.stereo=-2.0 attack/11=-1.9 " +
                "attack/11.stereo=-2.0 attack/12=-2.0 attack/12.stereo=-1.7 attack/13=-2.0 attack/13.stereo=-2.4 " +
                "hold/1=-1.5 hold/1.stereo=-1.1 hold/2=-1.6 hold/2.stereo=-0.3 hold/3=-1.6 hold/3.stereo=-1.1 " +
                "hold/4.stereo=-1.3 hold/5=-1.7 hold/5.stereo=-0.6 hold/6=-1.7 hold/6.stereo=-1.9 hold/7=-1.8 " +
                "hold/7.stereo=-1.8 hold/8.stereo=-0.4 hold/9=-1.8 hold/9.stereo=-1.7 hold/10=-1.9 " +
                "hold/10.stereo=-2.0 hold/11=-1.9 hold/11.stereo=-2.0 hold/12=-2.0 hold/12.stereo=-1.7 " +
                "hold/13=-2.0 hold/13.stereo=-2.4");
        add("heartopia_sax",
                "attack/0=-0.3 attack/0.stereo=-0.4 attack/1=-0.3 attack/2=-0.3 attack/2.stereo=-0.4 " +
                "attack/3=-0.3 attack/3.stereo=-0.4 attack/4=-0.3 attack/4.stereo=-0.4 attack/5=-0.3 " +
                "attack/5.stereo=-0.4 attack/6=-0.3 attack/6.stereo=-0.1 attack/7=-0.3 attack/7.stereo=-0.6 " +
                "attack/8=-0.3 attack/8.stereo=-0.4 attack/9=-0.3 attack/9.stereo=-0.4 attack/10=-0.3 " +
                "attack/10.stereo=-0.4 attack/11=-0.3 attack/11.stereo=-0.4 attack/12=-0.3 attack/12.stereo=-0.4 " +
                "attack/13=-0.3 attack/13.stereo=-0.4 hold/0=-0.3 hold/0.stereo=-0.4 hold/1=-0.3 hold/2=-0.3 " +
                "hold/2.stereo=-0.4 hold/3=-0.3 hold/3.stereo=-0.4 hold/4=-0.3 hold/4.stereo=-0.4 hold/5=-0.3 " +
                "hold/5.stereo=-0.4 hold/6=-0.3 hold/6.stereo=-0.1 hold/7=-0.3 hold/7.stereo=-0.6 hold/8=-0.3 " +
                "hold/8.stereo=-0.4 hold/9=-0.3 hold/9.stereo=-0.4 hold/10=-0.3 hold/10.stereo=-0.4 hold/11=-0.3 " +
                "hold/11.stereo=-0.4 hold/12=-0.3 hold/12.stereo=-0.4 hold/13=-0.3 hold/13.stereo=-0.4");
        add("heartopia_soprano_recorder",
                "attack/0=-0.1 attack/1=-0.1 attack/1.stereo=-0.1 attack/2=-0.1 attack/3=-2.0 attack/4=-0.1 " +
                "attack/6=-0.1 attack/7=-0.1 attack/8=-0.1 attack/9=-0.1 attack/10=-0.1 attack/11=-0.1 " +
                "attack/11.stereo=-1.7 attack/12=-0.1 attack/12.stereo=-0.8 attack/13=-0.8 hold/0=-0.1 " +
                "hold/1=-0.1 hold/1.stereo=-0.1 hold/2=-0.1 hold/3=-1.3 hold/4=-0.1 hold/6=-0.1 hold/7=-0.1 " +
                "hold/8=-0.1 hold/9=-0.1 hold/10=-0.1 hold/11=-0.1 hold/11.stereo=-1.7 hold/12=-0.1 " +
                "hold/12.stereo=-0.8 hold/13=-0.1");
        add("heartopia_violin",
                "attack/0=-1.2 attack/0.stereo=-1.0 attack/1=-1.2 attack/1.stereo=-1.2 attack/2=-1.2 " +
                "attack/2.stereo=-0.4 attack/3=-1.2 attack/3.stereo=-0.6 attack/4=-1.2 attack/4.stereo=-0.8 " +
                "attack/5=-1.2 attack/5.stereo=-2.3 attack/6=-1.2 attack/6.stereo=-1.2 attack/7=-2.3 " +
                "attack/7.stereo=-2.6 attack/8=-1.2 attack/8.stereo=-0.2 attack/9=-1.2 attack/9.stereo=-1.1 " +
                "attack/10=-1.2 attack/10.stereo=-0.7 attack/11=-1.2 attack/11.stereo=-2.1 attack/12.stereo=-1.4 " +
                "attack/13=-2.5 attack/13.stereo=-3.5 hold/0=-1.2 hold/0.stereo=-1.0 hold/1=-1.2 " +
                "hold/1.stereo=-1.2 hold/2=-1.2 hold/2.stereo=-0.4 hold/3=-1.2 hold/3.stereo=-0.6 hold/4=-1.2 " +
                "hold/4.stereo=-0.8 hold/5=-1.2 hold/5.stereo=-2.3 hold/6=-1.2 hold/6.stereo=-1.2 hold/7=-2.3 " +
                "hold/7.stereo=-2.6 hold/8=-1.2 hold/8.stereo=-0.2 hold/9=-1.2 hold/9.stereo=-1.1 hold/10=-1.2 " +
                "hold/10.stereo=-0.7 hold/11=-1.2 hold/11.stereo=-2.1 hold/12.stereo=-1.4 hold/13=-2.5 " +
                "hold/13.stereo=-3.5");
        add("heartopia_xiao",
                "attack/0=-4.5 attack/0.stereo=-4.6 attack/1=-2.2 attack/1.stereo=-2.3 attack/3=-5.2 " +
                "attack/3.stereo=-3.3 attack/4=-2.2 attack/4.stereo=-2.2 attack/5=-2.2 attack/5.stereo=-2.3 " +
                "attack/6=-2.2 attack/6.stereo=-2.3 attack/7=-2.2 attack/7.stereo=-2.3 attack/9=-2.2 " +
                "attack/9.stereo=-0.9 attack/10=-2.2 attack/10.stereo=-2.3 attack/11=-2.2 attack/11.stereo=-2.3 " +
                "attack/13=-6.5 attack/13.stereo=-5.7 hold/0=-4.5 hold/0.stereo=-4.6 hold/1=-2.2 " +
                "hold/1.stereo=-2.3 hold/3=-5.2 hold/3.stereo=-3.3 hold/4=-2.2 hold/4.stereo=-2.2 hold/5=-2.2 " +
                "hold/5.stereo=-2.3 hold/6=-2.2 hold/6.stereo=-2.3 hold/7=-2.2 hold/7.stereo=-2.3 hold/9=-2.2 " +
                "hold/9.stereo=-0.9 hold/10=-2.2 hold/10.stereo=-2.3 hold/11=-2.2 hold/11.stereo=-2.3 " +
                "hold/13=-6.5 hold/13.stereo=-5.7");
        add("keyboard_gw2",
                "0=-1.3 0.stereo=-1.3 1=-1.3 1.stereo=-1.3 2=-1.3 2.stereo=-1.3 3=-1.3 3.stereo=-1.3 4=-1.3 " +
                "4.stereo=-1.3 5=-1.3 5.stereo=-1.3 6=-1.3 6.stereo=-1.3 7=-1.3 7.stereo=-1.3 8=-1.3 " +
                "8.stereo=-1.3 9=-1.3 9.stereo=-1.3 10=-1.3 10.stereo=-1.3 12=-1.3 12.stereo=-1.3 13=-1.3 " +
                "13.stereo=-1.3 14=-1.3 14.stereo=-1.3 15=-1.3 15.stereo=-1.3 16=-1.3 16.stereo=-1.3 17=-1.3 " +
                "17.stereo=-1.3 18=-1.3 18.stereo=-1.3 19=-1.3 19.stereo=-1.3 20=-1.3 20.stereo=-1.3");
        add("keyboard_harpsichord",
                "0=-2.5 0.stereo=-4.7 1=-2.5 1.stereo=-3.7 2=-2.5 2.stereo=-4.6 3=-2.5 3.stereo=-6.6 4=-2.5 " +
                "4.stereo=-5.7 5=-2.5 5.stereo=-5.8 6=-2.5 6.stereo=-4.1 7=-2.5 7.stereo=-5.1 8=-2.5 " +
                "8.stereo=-4.6 9=-2.5 9.stereo=-4.2 10=-2.5 10.stereo=-4.5 11=-2.5 11.stereo=-7.2 12=-2.5 " +
                "12.stereo=-8.0 13=-2.5 13.stereo=-6.6 14=-2.5 14.stereo=-5.7 15=-2.5 15.stereo=-5.4 16=-4.7 " +
                "16.stereo=-5.6 17=-1.4 17.stereo=-6.5 18.stereo=-5.7 19=-4.5 19.stereo=-5.5 20=-2.5 " +
                "20.stereo=-6.7");
        add("keyboard_heartopia",
                "0=-3.9 0.stereo=-4.7 1=-1.4 1.stereo=-4.8 2.stereo=-3.8 3=-1.4 3.stereo=-2.6 4=-1.4 " +
                "4.stereo=-3.4 5=-1.4 5.stereo=-3.6 6=-1.4 6.stereo=-2.3 7=-1.4 7.stereo=-3.9 8=-1.4 " +
                "8.stereo=-6.3 9=-1.4 9.stereo=-4.3 10=-2.6 10.stereo=-4.5 11=-1.4 11.stereo=-3.4 12.stereo=-3.8 " +
                "13=-3.4 13.stereo=-4.3 14.stereo=-0.7 15=-1.4 15.stereo=-1.9 16=-1.4 16.stereo=-2.3 17=-1.4 " +
                "17.stereo=-1.8 18.stereo=-1.4 19=-1.4 19.stereo=-4.4 20=-3.4 20.stereo=-5.9");
        add("keyboard_yamaha_c5",
                "0=-1.7 0.stereo=-3.0 1=-1.7 1.stereo=-2.6 2=-3.0 2.stereo=-3.9 3=-1.7 3.stereo=-6.0 4=-1.7 " +
                "4.stereo=-5.9 5.stereo=-4.8 6=-4.4 6.stereo=-5.6 7=-2.7 7.stereo=-4.0 8=-1.7 8.stereo=-3.0 " +
                "9=-1.7 9.stereo=-3.0 10=-1.7 10.stereo=-2.6 11=-1.7 11.stereo=-2.6 12=-1.7 12.stereo=-5.7 " +
                "13=-0.4 13.stereo=-8.1 14=-0.4 14.stereo=-8.1 15=-1.7 15.stereo=-5.6 16=-1.7 16.stereo=-5.6 " +
                "17=-1.7 17.stereo=-4.7 18=-1.7 18.stereo=-4.7 19=-0.6 19.stereo=-2.5 20=-1.7 20.stereo=-3.7");
        add("microphone_bass_choir",
                "attack/4=-1.4 attack/4.stereo=-1.4 hold/4=-1.4 hold/4.stereo=-1.4");
        add("microphone_cat",
                "3=-0.1 4=-0.1 5=-0.1 6=-0.1 7=-0.2 8=-0.2 9=-0.2 10=-0.2 11=-0.3 12=-0.3 13=-0.3");
        add("microphone_dog",
                "0=-0.2 1=-0.2 2=-0.1 3=-0.1 4=-0.1 5=-0.1 6=-0.1 7=-0.1 8=-0.1");
        add("microphone_not_miku",
                "attack/1=-2.6 attack/1.stereo=-2.6 attack/2=-2.7 attack/2.stereo=-2.7 attack/3=-2.8 " +
                "attack/3.stereo=-2.8 attack/4=-4.1 attack/4.stereo=-4.1 attack/5=-1.4 attack/5.stereo=-1.4 " +
                "attack/6=-3.1 attack/6.stereo=-3.1 attack/7=-3.2 attack/7.stereo=-3.2 attack/8=-3.3 " +
                "attack/8.stereo=-3.3 attack/9=-3.4 attack/9.stereo=-3.4 attack/10=-3.4 attack/10.stereo=-3.4 " +
                "attack/11=-3.6 attack/11.stereo=-3.6 attack/12=-4.8 attack/12.stereo=-4.8 attack/13=-3.7 " +
                "attack/13.stereo=-3.7 attack/14=-3.8 attack/14.stereo=-3.8 attack/15=-3.9 attack/15.stereo=-3.9 " +
                "attack/16=-4.0 attack/16.stereo=-4.0 attack/17=-4.1 attack/17.stereo=-4.1 attack/18=-4.2 " +
                "attack/18.stereo=-4.2 attack/19=-4.3 attack/19.stereo=-4.3 attack/20=-2.8 attack/20.stereo=-2.8 " +
                "hold/1=-2.6 hold/1.stereo=-2.6 hold/2=-2.7 hold/2.stereo=-2.7 hold/3=-2.8 hold/3.stereo=-2.8 " +
                "hold/4=-4.1 hold/4.stereo=-4.1 hold/5=-1.4 hold/5.stereo=-1.4 hold/6=-3.1 hold/6.stereo=-3.1 " +
                "hold/7=-3.2 hold/7.stereo=-3.2 hold/8=-3.3 hold/8.stereo=-3.3 hold/9=-3.4 hold/9.stereo=-3.4 " +
                "hold/10=-3.4 hold/10.stereo=-3.4 hold/11=-3.6 hold/11.stereo=-3.6 hold/12=-4.8 " +
                "hold/12.stereo=-4.8 hold/13=-3.7 hold/13.stereo=-3.7 hold/14=-3.8 hold/14.stereo=-3.8 " +
                "hold/15=-3.9 hold/15.stereo=-3.9 hold/16=-4.0 hold/16.stereo=-4.0 hold/17=-4.1 " +
                "hold/17.stereo=-4.1 hold/18=-4.2 hold/18.stereo=-4.2 hold/19=-4.3 hold/19.stereo=-4.3 " +
                "hold/20=-2.8 hold/20.stereo=-2.8");
        add("microphone_not_teto_sneaky",
                "attack/15=-0.2 attack/15.stereo=-0.2 attack/16=-0.8 attack/16.stereo=-0.8 attack/17=-1.3 " +
                "attack/17.stereo=-1.3 attack/19=-2.5 attack/19.stereo=-2.5 attack/20=-3.1 attack/20.stereo=-3.1 " +
                "hold/15=-0.2 hold/15.stereo=-0.2 hold/16=-0.8 hold/16.stereo=-0.8 hold/17=-1.3 " +
                "hold/17.stereo=-1.3 hold/19=-2.5 hold/19.stereo=-2.5 hold/20=-3.1 hold/20.stereo=-3.1");
        add("nightwind_horn",
                "attack/0=-2.3 attack/1=-2.5 attack/1.stereo=-0.4 attack/2=-2.7 attack/2.stereo=-0.7 " +
                "attack/3=-3.0 attack/3.stereo=-0.5 attack/4=-3.2 attack/4.stereo=-2.0 attack/5=-3.4 " +
                "attack/5.stereo=-2.2 attack/6=-3.7 attack/6.stereo=-1.9 attack/7=-3.9 attack/7.stereo=-2.7 " +
                "attack/8=-4.1 attack/8.stereo=-2.1 attack/9=-4.4 attack/9.stereo=-3.3 attack/10=-4.6 " +
                "attack/10.stereo=-2.7 attack/11=-4.8 attack/11.stereo=-2.5 attack/12=-5.1 attack/12.stereo=-2.7 " +
                "attack/13=-5.3 attack/13.stereo=-3.5 hold/0=-2.3 hold/1=-2.5 hold/1.stereo=-0.4 hold/2=-2.7 " +
                "hold/2.stereo=-0.7 hold/3=-3.0 hold/3.stereo=-0.5 hold/4=-3.2 hold/4.stereo=-2.0 hold/5=-3.4 " +
                "hold/5.stereo=-2.2 hold/6=-3.7 hold/6.stereo=-1.9 hold/7=-3.9 hold/7.stereo=-2.7 hold/8=-4.1 " +
                "hold/8.stereo=-2.1 hold/9=-4.4 hold/9.stereo=-3.3 hold/10=-4.6 hold/10.stereo=-2.7 hold/11=-4.8 " +
                "hold/11.stereo=-2.5 hold/12=-5.1 hold/12.stereo=-2.7 hold/13=-5.3 hold/13.stereo=-3.5");
        add("saxophone_baritone",
                "attack/0.stereo=-0.7 attack/1.stereo=-0.7 attack/2.stereo=-0.7 attack/3.stereo=-0.4 " +
                "attack/4.stereo=-0.4 attack/5.stereo=-0.5 attack/6.stereo=-0.5 attack/7.stereo=-0.5 " +
                "attack/8.stereo=-0.6 attack/9.stereo=-0.6 attack/10.stereo=-0.6 attack/11.stereo=-0.2 " +
                "attack/12.stereo=-0.2 attack/13.stereo=-0.3 attack/14.stereo=-0.3 attack/15.stereo=-0.3 " +
                "attack/16.stereo=-0.3 attack/17.stereo=-0.3 attack/18.stereo=-0.3 attack/19.stereo=-0.7 " +
                "attack/20.stereo=-0.7 hold/0.stereo=-0.7 hold/1.stereo=-0.7 hold/2.stereo=-0.7 " +
                "hold/3.stereo=-0.4 hold/4.stereo=-0.4 hold/5.stereo=-0.5 hold/6.stereo=-0.5 hold/7.stereo=-0.5 " +
                "hold/8.stereo=-0.6 hold/9.stereo=-0.6 hold/10.stereo=-0.6 hold/11.stereo=-0.2 " +
                "hold/12.stereo=-0.2 hold/13.stereo=-0.3 hold/14.stereo=-0.3 hold/15.stereo=-0.3 " +
                "hold/16.stereo=-0.3 hold/17.stereo=-0.3 hold/18.stereo=-0.3 hold/19.stereo=-0.7 " +
                "hold/20.stereo=-0.7");
        add("saxophone_tenor",
                "attack/0=-3.1 attack/0.stereo=-3.1 attack/1=-3.2 attack/1.stereo=-3.2 attack/2=-3.2 " +
                "attack/2.stereo=-3.2 attack/3=-3.2 attack/3.stereo=-3.2 attack/4=-3.3 attack/4.stereo=-3.3 " +
                "attack/5=-2.0 attack/5.stereo=-2.0 attack/6=-3.3 attack/6.stereo=-3.3 attack/7=-3.3 " +
                "attack/7.stereo=-3.3 attack/8=-3.4 attack/8.stereo=-3.4 attack/9=-3.4 attack/9.stereo=-3.4 " +
                "attack/10=-3.4 attack/10.stereo=-3.4 attack/12=-3.5 attack/12.stereo=-3.5 attack/13=-3.5 " +
                "attack/13.stereo=-3.5 attack/14=-3.5 attack/14.stereo=-3.5 attack/15=-5.1 attack/15.stereo=-5.1 " +
                "attack/16=-3.6 attack/16.stereo=-3.6 attack/17=-2.1 attack/17.stereo=-2.1 attack/18=-3.6 " +
                "attack/18.stereo=-3.6 attack/19=-5.4 attack/19.stereo=-5.4 attack/20=-3.7 attack/20.stereo=-3.7 " +
                "hold/0=-3.1 hold/0.stereo=-3.1 hold/1=-3.2 hold/1.stereo=-3.2 hold/2=-3.2 hold/2.stereo=-3.2 " +
                "hold/3=-3.2 hold/3.stereo=-3.2 hold/4=-3.3 hold/4.stereo=-3.3 hold/5=-2.0 hold/5.stereo=-2.0 " +
                "hold/6=-3.3 hold/6.stereo=-3.3 hold/7=-3.3 hold/7.stereo=-3.3 hold/8=-3.4 hold/8.stereo=-3.4 " +
                "hold/9=-3.4 hold/9.stereo=-3.4 hold/10=-3.4 hold/10.stereo=-3.4 hold/12=-3.5 " +
                "hold/12.stereo=-3.5 hold/13=-3.5 hold/13.stereo=-3.5 hold/14=-3.5 hold/14.stereo=-3.5 " +
                "hold/15=-5.1 hold/15.stereo=-5.1 hold/16=-3.6 hold/16.stereo=-3.6 hold/17=-2.1 " +
                "hold/17.stereo=-2.1 hold/18=-3.6 hold/18.stereo=-3.6 hold/19=-5.4 hold/19.stereo=-5.4 " +
                "hold/20=-3.7 hold/20.stereo=-3.7");
        add("shamisen",
                "0=-3.0 0.stereo=-0.2 1=-3.0 1.stereo=-0.1 2=-3.0 2.stereo=-0.1 3=-3.0 3.stereo=-0.2 4=-3.0 " +
                "4.stereo=-0.1 5=-3.0 5.stereo=-0.1 6=-3.0 6.stereo=-0.1 7=-3.0 7.stereo=-0.2 8=-3.0 " +
                "8.stereo=-0.1 9=-3.0 10=-3.0 11=-3.0 12=-3.0 12.stereo=-0.1 13=-3.0 13.stereo=-0.1 14=-3.0 " +
                "14.stereo=-0.2 15=-3.0 16=-3.0 16.stereo=-0.1 17=-3.0 17.stereo=-0.1 18=-3.0 18.stereo=-0.2 " +
                "19=-3.0 19.stereo=-0.1 20=-3.0 20.stereo=-0.1");
        add("sky_bells",
                "0=-6.5 0.stereo=-6.6 1=-5.8 1.stereo=-5.8 2=-5.1 2.stereo=-5.1 3=-6.3 3.stereo=-6.3 " +
                "4.stereo=-0.1 5=-1.5 5.stereo=-1.5 6=-2.3 6.stereo=-2.4 7=-2.6 7.stereo=-2.8");
        add("sky_bird_call",
                "0=-0.2 0.stereo=-0.2 1=-0.2 1.stereo=-0.2 2=-0.2 2.stereo=-0.3 3=-0.2 3.stereo=-0.3 4=-0.2 " +
                "5=-0.2 5.stereo=-0.6 6=-0.2 6.stereo=-0.6 7=-0.2 7.stereo=-0.1 8=-0.2 8.stereo=-0.1 9=-0.2 " +
                "9.stereo=-0.5 10=-0.2 10.stereo=-0.5 11=-0.2 11.stereo=-0.4 12=-3.3 12.stereo=-3.1 13=-2.9 " +
                "13.stereo=-2.7");
        add("sky_chime_synth",
                "0=-4.0 0.stereo=-4.0 1=-3.8 1.stereo=-3.8 2=-3.6 2.stereo=-3.7 3=-3.5 3.stereo=-3.5 4=-3.3 " +
                "4.stereo=-3.3 5=-3.1 5.stereo=-3.1 7=-5.5 7.stereo=-5.5 8=-4.2 8.stereo=-4.2 9=-2.4 " +
                "9.stereo=-2.4 10=-2.2 10.stereo=-2.2 11=-2.0 11.stereo=-2.1 12=-1.9 12.stereo=-1.9 13=-0.2 " +
                "13.stereo=-0.3");
        add("sky_contrabass",
                "12=-0.5 12.stereo=-0.6 13.stereo=-0.1");
        add("sky_crab_call",
                "0=-0.2 1=-0.2 2=-0.2 2.stereo=-0.4 3=-0.2 3.stereo=-0.4 4=-0.2 4.stereo=-0.2 5=-0.2 " +
                "5.stereo=-0.1 6=-0.2 6.stereo=-0.1 7=-0.2 8=-0.2 9=-0.2 9.stereo=-0.4 10=-0.2 10.stereo=-0.4 " +
                "11=-1.6 11.stereo=-1.4 12=-0.2 12.stereo=-0.1 13=-0.2 13.stereo=-0.2");
        add("sky_fish_call",
                "0=-1.2 0.stereo=-0.9 1=-1.3 1.stereo=-1.0 2=-1.3 2.stereo=-1.5 3=-1.3 3.stereo=-1.5 4=-1.4 " +
                "4.stereo=-1.5 5=-1.4 5.stereo=-1.3 6=-3.4 6.stereo=-3.3 7=-1.4 7.stereo=-1.5 8=-1.5 " +
                "8.stereo=-1.5 9=-1.5 9.stereo=-1.6 10=-1.5 10.stereo=-1.6 11.stereo=-0.1 12=-5.9 12.stereo=-5.7 " +
                "13=-5.2 13.stereo=-5.0");
        add("sky_flute",
                "attack/0=-0.2 attack/0.stereo=-3.1 attack/1=-4.3 attack/1.stereo=-3.6 attack/2=-4.2 " +
                "attack/2.stereo=-3.5 attack/3=-2.3 attack/3.stereo=-4.7 attack/5=-2.3 attack/5.stereo=-2.5 " +
                "attack/6=-5.2 attack/6.stereo=-5.5 attack/7=-0.8 attack/7.stereo=-4.5 attack/8=-4.1 " +
                "attack/8.stereo=-2.4 attack/9=-2.3 attack/9.stereo=-1.1 attack/10=-2.3 attack/10.stereo=-4.1 " +
                "attack/11=-2.3 attack/11.stereo=-4.0 attack/12.stereo=-2.0 attack/13=-3.6 attack/13.stereo=-1.9 " +
                "hold/0=-0.2 hold/0.stereo=-3.1 hold/1=-4.3 hold/1.stereo=-3.6 hold/2=-4.2 hold/2.stereo=-3.5 " +
                "hold/3=-2.3 hold/3.stereo=-4.7 hold/5=-2.3 hold/5.stereo=-2.5 hold/6=-5.2 hold/6.stereo=-5.5 " +
                "hold/7=-0.8 hold/7.stereo=-4.5 hold/8=-4.1 hold/8.stereo=-2.4 hold/9=-2.3 hold/9.stereo=-1.1 " +
                "hold/10=-2.3 hold/10.stereo=-4.1 hold/11=-2.3 hold/11.stereo=-4.0 hold/12.stereo=-2.0 " +
                "hold/13=-3.6 hold/13.stereo=-1.9");
        add("sky_grand_piano",
                "0=-3.1 0.stereo=-3.2 1=-3.1 1.stereo=-4.4 2=-3.0 2.stereo=-1.4 3=-4.2 3.stereo=-2.6 4=-3.9 " +
                "4.stereo=-2.1 5=-2.8 5.stereo=-1.4 6=-2.8 6.stereo=-0.2 7=-2.7 7.stereo=-2.9 8=-2.6 " +
                "8.stereo=-4.0 9=-2.6 9.stereo=-1.2 10=-3.7 10.stereo=-2.2 11=-3.5 11.stereo=-1.7 12=-2.4 " +
                "12.stereo=-1.1 13=-2.4 14=-2.3 14.stereo=-0.1 15=-2.2 15.stereo=-3.4 16=-2.2 16.stereo=-5.5 " +
                "17=-2.1 17.stereo=-4.9 18=-2.1 18.stereo=-8.1 19=-2.0 19.stereo=-4.5 20=-1.9 20.stereo=-1.5");
        add("sky_guitar",
                "0=-5.5 0.stereo=-5.6 1=-5.5 1.stereo=-5.2 2=-3.9 2.stereo=-4.4 3=-3.6 3.stereo=-3.6 4=-5.5 " +
                "4.stereo=-5.6 5=-5.5 5.stereo=-5.5 6=-5.5 6.stereo=-5.6 7=-5.5 7.stereo=-5.6 8=-5.5 " +
                "8.stereo=-4.9 9=-5.5 9.stereo=-5.5 10=-5.5 10.stereo=-5.6 11=-5.5 11.stereo=-6.5 12=-5.5 " +
                "12.stereo=-5.5 13.stereo=-2.3");
        add("sky_handpan",
                "0=-1.0 0.stereo=-1.1 1=-0.9 1.stereo=-1.0 2=-0.9 2.stereo=-0.9 3=-0.8 3.stereo=-0.9 4=-0.7 " +
                "4.stereo=-0.7 5=-0.6 5.stereo=-0.7 6=-0.6 6.stereo=-0.6 7=-0.5 7.stereo=-0.5 8=-0.4 " +
                "8.stereo=-0.5 9=-0.3 9.stereo=-0.4 10=-0.2 10.stereo=-0.3 11=-0.2 11.stereo=-0.2 12=-0.1 " +
                "12.stereo=-0.1");
        add("sky_harmonica",
                "attack/11=-1.2 attack/11.stereo=-1.2 hold/11=-1.2 hold/11.stereo=-1.2");
        add("sky_horn",
                "attack/5=-2.3 attack/5.stereo=-2.3 attack/7=-1.8 attack/7.stereo=-1.8 attack/9=-0.8 " +
                "attack/9.stereo=-0.8 hold/5=-2.3 hold/5.stereo=-2.3 hold/7=-1.8 hold/7.stereo=-1.8 hold/9=-0.8 " +
                "hold/9.stereo=-0.8");
        add("sky_jelly_call",
                "0=-1.5 0.stereo=-1.6 1=-1.5 1.stereo=-1.6 2=-1.5 2.stereo=-1.6 3=-1.5 3.stereo=-1.6 4=-1.5 " +
                "4.stereo=-1.6 5=-1.5 5.stereo=-1.4 6=-1.5 6.stereo=-1.4 7=-1.5 7.stereo=-1.4 8=-1.5 " +
                "8.stereo=-1.4 9=-1.5 9.stereo=-1.4 10=-1.5 10.stereo=-1.4 11=-0.2 12=-1.5 12.stereo=-1.5 " +
                "13=-1.5 13.stereo=-1.5");
        add("sky_kalimba",
                "0=-1.6 0.stereo=-1.6 2=-1.6 2.stereo=-1.6 3=-1.6 3.stereo=-1.6 4=-1.6 4.stereo=-1.6 5=-1.6 " +
                "5.stereo=-1.6 6=-1.6 6.stereo=-1.6 7=-1.6 7.stereo=-1.6 8=-1.6 8.stereo=-1.6 9=-1.6 " +
                "9.stereo=-1.6 10=-3.5 10.stereo=-3.5 11=-1.6 11.stereo=-1.6 12=-1.6 12.stereo=-1.6 13=-1.6 " +
                "13.stereo=-1.6");
        add("sky_light_guitar",
                "attack/0=-1.1 attack/0.stereo=-1.1 attack/1=-1.1 attack/1.stereo=-1.1 attack/2=-1.1 " +
                "attack/2.stereo=-1.1 attack/3=-1.1 attack/3.stereo=-1.1 attack/4=-2.2 attack/4.stereo=-2.2 " +
                "attack/5=-1.1 attack/5.stereo=-1.1 attack/7=-1.1 attack/7.stereo=-1.1 attack/8=-1.1 " +
                "attack/8.stereo=-1.1 attack/9=-1.1 attack/9.stereo=-1.1 attack/10=-1.1 attack/10.stereo=-1.1 " +
                "attack/11=-1.1 attack/11.stereo=-1.1 attack/12=-1.1 attack/12.stereo=-1.1 attack/13=-1.1 " +
                "attack/13.stereo=-1.1 hold/0=-1.1 hold/0.stereo=-1.1 hold/1=-1.1 hold/1.stereo=-1.1 hold/2=-1.1 " +
                "hold/2.stereo=-1.1 hold/3=-1.1 hold/3.stereo=-1.1 hold/4=-2.2 hold/4.stereo=-2.2 hold/5=-1.1 " +
                "hold/5.stereo=-1.1 hold/7=-1.1 hold/7.stereo=-1.1 hold/8=-1.1 hold/8.stereo=-1.1 hold/9=-1.1 " +
                "hold/9.stereo=-1.1 hold/10=-1.1 hold/10.stereo=-1.1 hold/11=-1.1 hold/11.stereo=-1.1 " +
                "hold/12=-1.1 hold/12.stereo=-1.1 hold/13=-1.1 hold/13.stereo=-1.1");
        add("sky_manta_call",
                "0=-0.4 0.stereo=-0.4 1=-0.4 1.stereo=-0.4 2=-0.4 2.stereo=-0.2 3=-0.4 3.stereo=-0.2 4=-0.4 " +
                "5=-0.4 5.stereo=-0.5 6=-0.4 6.stereo=-0.5 7=-0.4 7.stereo=-0.4 8=-0.4 8.stereo=-0.4 9=-0.4 " +
                "9.stereo=-0.6 10=-0.4 10.stereo=-0.6 11=-0.4 11.stereo=-0.4 12=-0.4 12.stereo=-0.5 13=-0.4 " +
                "13.stereo=-0.4");
        add("sky_manta_ocarina",
                "attack/12=-2.2 attack/12.stereo=-2.2 hold/12=-2.2 hold/12.stereo=-2.2");
        add("sky_moth_call",
                "0.stereo=-0.1 1=-0.1 1.stereo=-0.1 2=-0.1 2.stereo=-0.2 3=-0.2 3.stereo=-0.2 4=-0.2 " +
                "4.stereo=-0.3 5=-0.3 5.stereo=-0.4 6=-0.3 6.stereo=-0.5 7=-0.4 7.stereo=-0.7 8=-0.5 " +
                "8.stereo=-0.8 9=-0.5 9.stereo=-0.8 10=-0.6 10.stereo=-0.9 11=-0.6 11.stereo=-1.2 12=-3.5 " +
                "12.stereo=-3.0 13=-2.1 13.stereo=-1.7");
        add("sky_ocarina",
                "attack/1.stereo=-0.1 attack/3.stereo=-0.1 attack/6.stereo=-0.1 attack/7.stereo=-0.1 " +
                "attack/11.stereo=-0.1 attack/12=-1.3 attack/12.stereo=-1.3 attack/13.stereo=-0.1 " +
                "hold/1.stereo=-0.1 hold/3.stereo=-0.1 hold/6.stereo=-0.1 hold/7.stereo=-0.1 hold/11.stereo=-0.1 " +
                "hold/12=-1.3 hold/12.stereo=-1.3 hold/13.stereo=-0.1");
        add("sky_panflute",
                "attack/0=-1.8 attack/0.stereo=-1.8 attack/1=-2.7 attack/1.stereo=-2.7 attack/2=-1.5 " +
                "attack/2.stereo=-1.5 attack/3=-1.4 attack/3.stereo=-1.4 attack/4=-1.3 attack/4.stereo=-1.3 " +
                "attack/5=-1.1 attack/5.stereo=-1.1 attack/6=-1.0 attack/6.stereo=-1.0 attack/7=-0.9 " +
                "attack/7.stereo=-0.9 attack/8=-0.7 attack/8.stereo=-0.7 attack/9=-0.6 attack/9.stereo=-0.6 " +
                "attack/10=-5.5 attack/10.stereo=-5.5 attack/12=-0.2 attack/12.stereo=-0.2 attack/13=-0.1 " +
                "attack/13.stereo=-0.1 hold/0=-1.8 hold/0.stereo=-1.8 hold/1=-2.7 hold/1.stereo=-2.7 hold/2=-1.5 " +
                "hold/2.stereo=-1.5 hold/3=-1.4 hold/3.stereo=-1.4 hold/4=-1.3 hold/4.stereo=-1.3 hold/5=-1.1 " +
                "hold/5.stereo=-1.1 hold/6=-1.0 hold/6.stereo=-1.0 hold/7=-0.9 hold/7.stereo=-0.9 hold/8=-0.7 " +
                "hold/8.stereo=-0.7 hold/9=-0.6 hold/9.stereo=-0.6 hold/10=-5.5 hold/10.stereo=-5.5 hold/12=-0.2 " +
                "hold/12.stereo=-0.2 hold/13=-0.1 hold/13.stereo=-0.1");
        add("sky_piano",
                "0=-2.8 0.stereo=-1.8 1.stereo=-2.8 2=-2.8 2.stereo=-2.8 3=-5.6 3.stereo=-4.6 4=-2.8 " +
                "4.stereo=-1.5 5=-2.8 5.stereo=-0.5 6=-4.3 6.stereo=-2.0 7=-2.8 7.stereo=-1.9 8.stereo=-1.7 " +
                "9=-2.8 9.stereo=-2.8 10=-5.5 10.stereo=-4.6 11=-2.8 11.stereo=-1.5 12=-2.8 12.stereo=-0.6 " +
                "13=-2.8 13.stereo=-0.6 14=-2.8 14.stereo=-1.4 15=-2.8 15.stereo=-1.6 16=-1.7 16.stereo=-0.3 " +
                "17=-2.8 17.stereo=-1.3 18=-6.2 18.stereo=-4.8 19.stereo=-3.1 20=-0.6 20.stereo=-4.2");
        add("sky_pipa",
                "0=-3.5 0.stereo=-3.5 1=-3.2 1.stereo=-3.2 2=-2.9 2.stereo=-3.0 3=-2.7 3.stereo=-2.7 4=-2.4 " +
                "4.stereo=-2.4 5=-2.1 5.stereo=-2.1 6=-1.9 6.stereo=-1.9 7=-1.6 7.stereo=-1.7 8=-3.3 " +
                "8.stereo=-3.3 9=-1.1 9.stereo=-1.1 10=-0.8 10.stereo=-0.8 11=-0.5 11.stereo=-0.6 12=-0.3 " +
                "12.stereo=-0.3 13.stereo=-0.1");
        add("sky_saxophone",
                "attack/8=-2.2 attack/8.stereo=-2.2 attack/9=-0.1 attack/9.stereo=-0.1 attack/10=-0.2 " +
                "attack/10.stereo=-0.2 attack/11=-0.3 attack/11.stereo=-0.3 attack/12=-0.3 attack/12.stereo=-0.3 " +
                "attack/13=-2.2 attack/13.stereo=-2.2 hold/8=-2.2 hold/8.stereo=-2.2 hold/9=-0.1 " +
                "hold/9.stereo=-0.1 hold/10=-0.2 hold/10.stereo=-0.2 hold/11=-0.3 hold/11.stereo=-0.3 " +
                "hold/12=-0.3 hold/12.stereo=-0.3 hold/13=-2.2 hold/13.stereo=-2.2");
        add("sky_spirit_manta_call",
                "1=-0.2 1.stereo=-0.3 2=-0.4 2.stereo=-0.4 3=-0.6 3.stereo=-0.6 4=-0.8 4.stereo=-0.8 5=-1.0 " +
                "5.stereo=-1.1 6=-1.2 6.stereo=-1.2 7=-1.4 7.stereo=-1.4 8=-1.6 8.stereo=-1.7 9=-1.8 " +
                "9.stereo=-1.7 10=-2.0 10.stereo=-1.9 11=-2.2 11.stereo=-2.1 12=-2.4 12.stereo=-2.5 13=-2.6 " +
                "13.stereo=-2.7");
        add("sky_toy_ukulele",
                "0=-5.3 0.stereo=-5.4 1=-5.3 1.stereo=-5.9 2=-5.3 2.stereo=-5.3 3=-5.3 3.stereo=-5.3 4=-5.3 " +
                "4.stereo=-5.3 5=-5.3 5.stereo=-5.4 6=-5.3 6.stereo=-5.5 7=-5.3 7.stereo=-5.2 8=-5.3 " +
                "8.stereo=-5.5 9=-4.3 9.stereo=-4.3 10=-5.3 10.stereo=-5.3 11=-12.1 11.stereo=-11.5 12=-5.3 " +
                "12.stereo=-5.2");
        add("sky_transverse_flute",
                "attack/0=-1.0 attack/0.stereo=-1.0 attack/2=-1.0 attack/2.stereo=-1.0 attack/3=-1.0 " +
                "attack/3.stereo=-1.0 attack/4=-1.0 attack/4.stereo=-1.0 attack/5=-1.0 attack/5.stereo=-1.0 " +
                "attack/6=-1.0 attack/6.stereo=-1.0 attack/7=-1.0 attack/7.stereo=-1.0 attack/8=-1.0 " +
                "attack/8.stereo=-1.0 attack/9=-1.0 attack/9.stereo=-1.0 attack/10=-1.0 attack/10.stereo=-1.0 " +
                "attack/11=-1.0 attack/11.stereo=-1.0 attack/12=-1.0 attack/12.stereo=-1.0 attack/13=-1.0 " +
                "attack/13.stereo=-1.0 hold/0=-1.0 hold/0.stereo=-1.0 hold/2=-1.0 hold/2.stereo=-1.0 hold/3=-1.0 " +
                "hold/3.stereo=-1.0 hold/4=-1.0 hold/4.stereo=-1.0 hold/5=-1.0 hold/5.stereo=-1.0 hold/6=-1.0 " +
                "hold/6.stereo=-1.0 hold/7=-1.0 hold/7.stereo=-1.0 hold/8=-1.0 hold/8.stereo=-1.0 hold/9=-1.0 " +
                "hold/9.stereo=-1.0 hold/10=-1.0 hold/10.stereo=-1.0 hold/11=-1.0 hold/11.stereo=-1.0 " +
                "hold/12=-1.0 hold/12.stereo=-1.0 hold/13=-1.0 hold/13.stereo=-1.0");
        add("sky_triumph_handpan",
                "0=-2.9 0.stereo=-2.9 1=-2.9 1.stereo=-2.9 2=-2.9 2.stereo=-2.9 3=-2.9 3.stereo=-2.9 4=-0.1 " +
                "4.stereo=-0.1 6=-4.4 6.stereo=-4.4 7=-4.5 7.stereo=-4.5 8=-4.3 8.stereo=-4.3 9=-1.2 " +
                "9.stereo=-1.2 10=-1.5 10.stereo=-1.5 11=-0.1 11.stereo=-0.1 12=-8.7 12.stereo=-8.7 13=-2.9 " +
                "13.stereo=-2.9");
        add("sky_trumpet",
                "attack/0.stereo=-0.1 attack/7.stereo=-0.1 attack/10.stereo=-0.1 attack/11.stereo=-0.1 " +
                "hold/0.stereo=-0.1 hold/7.stereo=-0.1 hold/10.stereo=-0.1 hold/11.stereo=-0.1");
        add("sky_violin",
                "attack/11=-1.3 attack/11.stereo=-1.3 hold/11=-1.3 hold/11.stereo=-1.3");
        add("sky_winter_piano",
                "0=-3.2 0.stereo=-1.5 1=-1.8 1.stereo=-1.2 2=-1.8 2.stereo=-0.2 3=-1.8 3.stereo=-0.3 4=-4.3 " +
                "4.stereo=-2.8 5.stereo=-1.5 6.stereo=-0.4 7=-6.1 7.stereo=-4.5 8=-1.8 8.stereo=-1.2 9=-1.8 " +
                "9.stereo=-0.4 10=-1.8 10.stereo=-0.5 11=-4.0 11.stereo=-2.7 12.stereo=-3.1 13=-0.7 " +
                "13.stereo=-3.7 14=-1.8 14.stereo=-3.0 15=-1.8 15.stereo=-2.6 16=-1.8 16.stereo=-1.4 17=-1.8 " +
                "17.stereo=-0.4 18=-1.8 18.stereo=-1.8 19=-1.8 19.stereo=-1.4 20=-1.8 20.stereo=-1.4");
        add("sky_xylophone",
                "0=-0.5 0.stereo=-0.6 1=-0.6 1.stereo=-0.6 2=-0.7 2.stereo=-0.7 3=-0.8 3.stereo=-0.8 4=-0.9 " +
                "4.stereo=-0.9 5=-1.0 5.stereo=-0.8 6=-1.1 6.stereo=-1.2 7=-1.1 7.stereo=-1.2 8=-1.2 " +
                "8.stereo=-1.3 9=-1.3 9.stereo=-1.4 10.stereo=-0.9 11=-1.5 11.stereo=-1.9 12=-4.0 12.stereo=-4.1 " +
                "13=-0.5 13.stereo=-0.7");
        add("trumpet_westgate_studios",
                "attack/1=-0.3 attack/1.stereo=-0.3 attack/2=-0.5 attack/2.stereo=-0.5 attack/3=-0.7 " +
                "attack/3.stereo=-0.7 attack/4=-0.9 attack/4.stereo=-0.9 attack/5=-2.8 attack/5.stereo=-2.8 " +
                "attack/6=-3.5 attack/6.stereo=-3.5 attack/8=-1.8 attack/8.stereo=-1.8 attack/9=-2.1 " +
                "attack/9.stereo=-2.1 attack/10=-2.3 attack/10.stereo=-2.3 attack/11=-2.5 attack/11.stereo=-2.5 " +
                "attack/12=-2.7 attack/12.stereo=-2.7 attack/13=-2.9 attack/13.stereo=-2.9 attack/14=-3.2 " +
                "attack/14.stereo=-3.2 attack/15=-3.4 attack/15.stereo=-3.4 attack/16=-3.6 attack/16.stereo=-3.6 " +
                "attack/17=-3.8 attack/17.stereo=-3.8 attack/18=-3.0 attack/18.stereo=-3.0 attack/19=-4.3 " +
                "attack/19.stereo=-4.3 attack/20=-4.5 attack/20.stereo=-4.5 hold/1=-0.3 hold/1.stereo=-0.3 " +
                "hold/2=-0.5 hold/2.stereo=-0.5 hold/3=-0.7 hold/3.stereo=-0.7 hold/4=-0.9 hold/4.stereo=-0.9 " +
                "hold/5=-2.8 hold/5.stereo=-2.8 hold/6=-3.5 hold/6.stereo=-3.5 hold/8=-1.8 hold/8.stereo=-1.8 " +
                "hold/9=-2.1 hold/9.stereo=-2.1 hold/10=-2.3 hold/10.stereo=-2.3 hold/11=-2.5 " +
                "hold/11.stereo=-2.5 hold/12=-2.7 hold/12.stereo=-2.7 hold/13=-2.9 hold/13.stereo=-2.9 " +
                "hold/14=-3.2 hold/14.stereo=-3.2 hold/15=-3.4 hold/15.stereo=-3.4 hold/16=-3.6 " +
                "hold/16.stereo=-3.6 hold/17=-3.8 hold/17.stereo=-3.8 hold/18=-3.0 hold/18.stereo=-3.0 " +
                "hold/19=-4.3 hold/19.stereo=-4.3 hold/20=-4.5 hold/20.stereo=-4.5");
        add("ukulele",
                "0=-4.5 1=-4.5 2=-4.5 3=-4.5 4=-4.5 5=-4.5 6=-4.5 7=-4.5 8=-4.5 9=-4.5 10=-4.5 11=-4.5 12=-2.7 " +
                "14=-10.3 15=-4.5 16=-2.6 17=-4.5 18=-4.5 19=-5.7 20=-3.2");
        add("vintage_lyre",
                "0=-1.4 1=-1.4 3=-1.4 4=-1.4 5=-1.4 6=-0.1 7=-1.4 8=-1.4 9=-1.5 10=-1.5 11=-1.5 12=-3.5 13=-1.5 " +
                "15=-1.5 16=-1.5 17=-1.5 18=-1.5 19=-1.5 20=-1.5");
        add("violin_pizzicato",
                "0=-2.9 0.stereo=-5.3 1.stereo=-2.5 2=-5.8 2.stereo=-6.6 3=-6.8 3.stereo=-7.6 4=-1.0 " +
                "4.stereo=-3.7 5=-2.9 5.stereo=-9.0 6=-1.6 6.stereo=-7.7 7=-4.2 7.stereo=-5.7 8=-2.9 " +
                "8.stereo=-4.4 9=-2.9 9.stereo=-4.1 10=-2.9 10.stereo=-4.1 11=-2.9 11.stereo=-7.3 12=-2.9 " +
                "12.stereo=-6.0 13=-2.9 13.stereo=-6.0 14=-2.9 14.stereo=-6.2 15=-2.9 15.stereo=-6.2 16=-2.9 " +
                "16.stereo=-4.7 17=-2.9 17.stereo=-4.7 18=-4.0 18.stereo=-6.1 19=-2.9 19.stereo=-5.5 20=-2.9 " +
                "20.stereo=-5.5");
        add("windsong_lyre",
                "0=-6.9 0.stereo=-4.7 1=-2.7 1.stereo=-0.1 2=-9.7 2.stereo=-7.1 3=-8.3 3.stereo=-6.2 4=-8.1 " +
                "4.stereo=-6.4 5=-8.0 5.stereo=-7.3 6=-7.8 6.stereo=-11.7 7=-4.4 7.stereo=-5.6 8=-8.6 " +
                "8.stereo=-6.3 9=-7.4 9.stereo=-6.4 10=-7.3 10.stereo=-5.2 11=-7.1 11.stereo=-4.5 12=-7.0 " +
                "12.stereo=-6.8 13=-6.8 13.stereo=-4.5 14=-6.7 14.stereo=-4.2 15=-6.5 15.stereo=-4.6 16=-5.1 " +
                "16.stereo=-3.4 17=-7.8 17.stereo=-5.9 18.stereo=-2.0 19=-6.0 19.stereo=-8.8 20=-5.8 " +
                "20.stereo=-3.8");
        add("wwm_1",
                "0=-3.0 0.stereo=-3.0 1=-3.0 1.stereo=-2.2 2=-3.0 2.stereo=-3.7 3=-4.4 3.stereo=-4.5 4=-3.0 " +
                "4.stereo=-4.2 5=-3.0 5.stereo=-2.3 6=-3.0 6.stereo=-6.6 7=-1.0 7.stereo=-2.3 8=-6.0 " +
                "8.stereo=-4.3 9=-3.0 9.stereo=-5.1 10=-3.0 10.stereo=-3.4 11=-3.0 11.stereo=-1.0 12=-3.0 " +
                "12.stereo=-2.3 13=-3.0 13.stereo=-4.0 14=-3.0 14.stereo=-3.5 15=-6.4 15.stereo=-6.3 16=-3.0 " +
                "16.stereo=-4.3 17.stereo=-2.3 18=-3.0 18.stereo=-2.4 19=-5.7 19.stereo=-5.8 20=-3.0 " +
                "20.stereo=-3.6");
        add("wwm_2",
                "0=-3.9 0.stereo=-8.0 1=-3.9 1.stereo=-8.0 2=-3.9 2.stereo=-7.9 3=-3.9 3.stereo=-7.9 4=-3.9 " +
                "4.stereo=-8.0 5=-3.9 5.stereo=-7.9 6=-3.9 6.stereo=-7.9 7=-3.9 7.stereo=-8.0 8=-3.9 " +
                "8.stereo=-7.8 9=-3.9 9.stereo=-7.8 10=-3.9 10.stereo=-7.7 11=-3.9 11.stereo=-7.7 12.stereo=-2.4 " +
                "13=-3.9 13.stereo=-4.1 14=-3.9 14.stereo=-4.5 15=-2.2 15.stereo=-2.5 16=-3.9 16.stereo=-3.8 " +
                "17=-3.9 17.stereo=-3.8 18=-0.8 18.stereo=-0.5 19=-3.9 19.stereo=-3.9 20=-3.9 20.stereo=-2.8");
        add("wwm_4",
                "attack/0=-1.3 attack/0.stereo=-3.0 attack/1=-0.3 attack/1.stereo=-2.7 attack/2=-1.3 " +
                "attack/2.stereo=-1.0 attack/3=-2.9 attack/3.stereo=-0.7 attack/4=-1.3 attack/4.stereo=-2.0 " +
                "attack/5=-1.3 attack/5.stereo=-3.1 attack/6=-1.3 attack/6.stereo=-0.5 attack/7=-3.2 " +
                "attack/7.stereo=-2.5 attack/8=-1.3 attack/8.stereo=-2.3 attack/9=-1.3 attack/9.stereo=-0.5 " +
                "attack/10=-1.3 attack/11=-1.3 attack/11.stereo=-1.7 attack/12.stereo=-2.9 attack/13=-1.3 " +
                "attack/14=-1.3 attack/14.stereo=-4.6 attack/15=-1.3 attack/16=-0.1 attack/16.stereo=-1.9 " +
                "attack/17=-1.3 attack/17.stereo=-1.4 attack/18=-1.3 attack/18.stereo=-1.4 attack/19=-1.3 " +
                "attack/19.stereo=-0.8 attack/20=-1.3 attack/20.stereo=-1.1 hold/0=-1.3 hold/0.stereo=-3.0 " +
                "hold/1=-0.3 hold/1.stereo=-2.7 hold/2=-1.3 hold/2.stereo=-1.0 hold/3=-2.9 hold/3.stereo=-0.7 " +
                "hold/4=-1.3 hold/4.stereo=-2.0 hold/5=-1.3 hold/5.stereo=-3.1 hold/6=-1.3 hold/6.stereo=-0.5 " +
                "hold/7=-3.2 hold/7.stereo=-2.5 hold/8=-1.3 hold/8.stereo=-2.3 hold/9=-1.3 hold/9.stereo=-0.5 " +
                "hold/10=-1.3 hold/11=-1.3 hold/11.stereo=-1.7 hold/12.stereo=-2.9 hold/13=-1.3 hold/14=-1.3 " +
                "hold/14.stereo=-4.6 hold/15=-1.3 hold/16=-0.1 hold/16.stereo=-1.9 hold/17=-1.3 " +
                "hold/17.stereo=-1.4 hold/18=-1.3 hold/18.stereo=-1.4 hold/19=-1.3 hold/19.stereo=-0.8 " +
                "hold/20=-1.3 hold/20.stereo=-1.1");
        add("wwm_5",
                "attack/1=-4.0 attack/1.stereo=-4.2 attack/4.stereo=-1.0 attack/5=-1.8 attack/5.stereo=-1.1 " +
                "attack/9=-2.4 attack/9.stereo=-1.5 attack/13=-5.6 attack/13.stereo=-4.1 attack/14.stereo=-1.4 " +
                "attack/15.stereo=-0.7 attack/16.stereo=-0.2 attack/17.stereo=-0.5 attack/18.stereo=-0.3 " +
                "attack/19.stereo=-0.1 attack/20.stereo=-0.5 hold/1=-4.0 hold/1.stereo=-4.2 hold/4.stereo=-1.0 " +
                "hold/5=-1.8 hold/5.stereo=-1.1 hold/9=-2.4 hold/9.stereo=-1.5 hold/13=-5.6 hold/13.stereo=-4.1 " +
                "hold/14.stereo=-1.4 hold/15.stereo=-0.7 hold/16.stereo=-0.2 hold/17.stereo=-0.5 " +
                "hold/18.stereo=-0.3 hold/19.stereo=-0.1 hold/20.stereo=-0.5");
        add("wwm_6",
                "0=-2.1 0.stereo=-3.0 1=-0.3 1.stereo=-1.5 2=-0.3 2.stereo=-1.3 3=-0.3 3.stereo=-1.5 4=-0.3 " +
                "4.stereo=-1.3 5=-0.3 5.stereo=-1.2 6=-0.3 6.stereo=-0.9 7=-0.3 7.stereo=-0.9 8=-0.3 " +
                "8.stereo=-1.6 9=-0.3 9.stereo=-2.3 10=-0.3 10.stereo=-2.1 11=-0.3 11.stereo=-2.2 12=-0.3 " +
                "12.stereo=-0.2 13=-0.3 13.stereo=-2.2 14=-0.3 14.stereo=-2.1 15=-0.3 15.stereo=-1.2 16=-0.3 " +
                "16.stereo=-1.8 17=-0.3 17.stereo=-1.5 18=-0.3 19=-0.3 20=-0.3 20.stereo=-0.5");
        add("wwm_7",
                "0.stereo=-1.2 1=-7.7 1.stereo=-7.5 2=-4.0 2.stereo=-7.8 3=-3.9 3.stereo=-2.9 4=-3.7 " +
                "4.stereo=-3.4 5=-0.7 5.stereo=-1.8 6=-3.5 6.stereo=-3.9 7=-7.6 7.stereo=-6.5 8=-3.3 " +
                "8.stereo=-6.7 9=-3.1 9.stereo=-1.8 10=-3.0 10.stereo=-1.9 11=-2.9 11.stereo=-1.1 12=-2.8 " +
                "12.stereo=-2.2 13=-2.7 13.stereo=-1.4");
    }
}
