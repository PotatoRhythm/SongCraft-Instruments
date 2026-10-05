package com.stump.songcraft_instruments.util;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;

import java.util.Map;

import static java.util.Map.entry;

public abstract class LabelUtil {
    
    public static final String[]
        DO_RE_MI = {
            "do", "re", "mi", "fa", "so", "la", "ti"
        }
    ;
    public static final char[]
        ABC = {
            'C', 'D', 'E', 'F', 'G', 'A', 'B'
        }
    ;


    // Pitch system implementation
    // Literally could not have been done w/o Specy's instrument app, oh my gosh their method is genius
    // Either that or I'm just too much of a novice on music theory

    /**
     * @implNote Scales map taken from Specy's
     * <a href=https://github.com/Specy/genshin-music/blob/19dfe0e2fb8081508bd61dd47289dcb2d89ad5e3/src/Config.ts#L89>
     * Genshin Music app configs
     * </a>
     */
    public static final Map<String, String[]> NOTE_SCALES = Map.ofEntries(
        entry("Cb", strArr("Cb", "Dbb", "Db", "Ebb", "Eb", "Fb", "Gbb", "Gb", "Abb", "Ab", "Bbb", "Bb")),
        entry("C", strArr("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")),
        entry("C#", strArr("C#", "D", "D#", "E", "E#", "F#", "G", "G#", "A", "A#", "B", "B#")),
        entry("Db", strArr("Db", "Ebb", "Eb", "Fb", "F", "Gb", "Abb", "Ab", "Bbb", "Bb", "Cb", "C")),
        entry("D", strArr("D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B", "C", "C#")),
        entry("D#", strArr("D#", "E", "E#", "F#", "F##", "G#", "A", "A#", "B", "B#", "C#", "C##")),
        entry("Eb", strArr("Eb", "Fb", "F", "Gb", "G", "Ab", "Bbb", "Bb", "Cb", "C", "Db", "D")),
        entry("E", strArr("E", "F", "F#", "G", "G#", "A", "Bb", "B", "C", "C#", "D", "D#")),
        entry("E#", strArr("E#", "F#", "F##", "G#", "G##", "A#", "B", "B#", "C#", "C##", "D#", "D##")),
        entry("Fb", strArr("Fb", "Gbb", "Gb", "Abb", "Ab", "Bbb", "Cbb", "Cb", "Dbb", "Db", "Ebb", "Eb")),
        entry("F", strArr("F", "Gb", "G", "Ab", "A", "Bb", "Cb", "C", "Db", "D", "Eb", "E")),
        entry("F#", strArr("F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "E#")),
        entry("Gb", strArr("Gb", "Abb", "Ab", "Bbb", "Bb", "Cb", "Dbb", "Db", "Ebb", "Eb", "Fb", "F")),
        entry("G", strArr("G", "Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E", "F", "F#")),
        entry("G#", strArr("G#", "A", "A#", "B", "B#", "C#", "D", "D#", "E", "E#", "F#", "F##")),
        entry("Ab", strArr("Ab", "Bbb", "Bb", "Cb", "C", "Db", "Ebb", "Eb", "Fb", "F", "Gb", "G")),
        entry("A", strArr("A", "Bb", "B", "C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#")),
        entry("A#", strArr("A#", "B", "B#", "C#", "C##", "D#", "E", "E#", "F#", "F##", "G#", "G##")),
        entry("Bb", strArr("Bb", "Cb", "C", "Db", "D", "Eb", "Fb", "F", "Gb", "G", "Ab", "A")),
        entry("B", strArr("B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#")),
        entry("B#", strArr("B#", "C#", "C##", "D#", "D##", "E#", "F#", "F##", "G#", "G##", "A#", "A##")),

        // Ukulele-specific (chords):
        entry("Am", strArr("Am", "A#m", "Bm", "Cm", "C#m", "Dm", "D#m", "Em", "Fm", "F#m", "Gm", "G#m")),
        entry("Dm", strArr("Dm", "D#m", "Em", "Fm", "F#m", "Gm", "G#m", "Am", "A#m", "Bm", "Cm", "C#m")),
        entry("Em", strArr("Em", "Fm", "F#m", "Gm", "G#m", "Am", "A#m", "Bm", "Cm", "C#m", "Dm", "D#m")),
        entry("G7", strArr("G7", "Ab7", "A7", "Bb7", "B7", "C7", "Db7", "D7", "Eb7", "E7", "F7", "F#7"))
    );
    public static final int NOTES_PER_SCALE = NOTE_SCALES.get("C").length;

    private static String[] strArr(final String... arr) {
        return arr;
    }
    

    public static String getNoteName(final int pitch, final String[] noteLayout, final int offset) {
        final String baseNote = noteLayout[CommonUtil.wrapAround(offset, noteLayout.length)];

        final String[] scale = NOTE_SCALES.get(baseNote);
        return scale[(CommonUtil.doublyPyWrap(pitch, scale.length))];
    }

    /**
     * The 12 notes of an octave from C, named with sharps or with flats
     */
    public static final String[]
        SHARP_NOTES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"},
        FLAT_NOTES = {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"},
        // F# major's 7th is E#, so that its scale names every letter once
        F_SHARP_MAJOR_NOTES = {"C", "C#", "D", "D#", "E", "E#", "F#", "G", "G#", "A", "A#", "B"};
    private static final int[] NATURAL_SEMITONES = {9, 11, 0, 2, 4, 5, 7}; // A to G

    /**
     * @param transpose The semitones C major is transposed by
     * @return Whether the major key it lands on is written with flats:
     * Db, Eb, F, Ab and Bb. The rest, including F# over Gb, are written with sharps.
     */
    public static boolean isFlatKey(final int transpose) {
        return switch (Math.floorMod(transpose, 12)) {
            case 1, 3, 5, 8, 10 -> true;
            default -> false;
        };
    }
    /**
     * @return The common name of the major key C major is transposed to, such as Db rather than C#
     */
    public static String getKeyName(final int transpose) {
        return getNotesOfKey(transpose)[Math.floorMod(transpose, 12)];
    }
    private static String[] getNotesOfKey(final int transpose) {
        if (Math.floorMod(transpose, 12) == 6)
            return F_SHARP_MAJOR_NOTES;

        return isFlatKey(transpose) ? FLAT_NOTES : SHARP_NOTES;
    }

    /**
     * @param noteName A note or chord name such as {@code C}, {@code F#}, {@code Bb} or {@code Dm}
     * @param pitch The semitones to transpose the note by
     * @return The transposed note, named with the sharps or flats of the key it is transposed to,
     * keeping any chord suffix
     * @see #isFlatKey
     */
    public static String transposeNote(final String noteName, final int pitch) {
        int semitone = NATURAL_SEMITONES[noteName.charAt(0) - 'A'];

        int i = 1;
        for (; i < noteName.length(); i++) {
            if (noteName.charAt(i) == '#')
                semitone++;
            else if (noteName.charAt(i) == 'b')
                semitone--;
            else
                break;
        }

        return getNotesOfKey(pitch)[Math.floorMod(semitone + pitch, 12)] + noteName.substring(i);
    }

    /**
     * @param omitIfAccurate If the {@link ModClientConfigs#ACCURATE_NOTES} setting is enabled,
     * get the natural version of the note only
     * @return The given note, replaced with accurate accidentals unicodes
     */
    public static String formatNoteName(final String noteName, final boolean omitIfAccurate) {
        if (noteName.isEmpty())
            return "";
            
        String result = String.valueOf(noteName.charAt(0));
        if (!(omitIfAccurate && ModClientConfigs.ACCURATE_NOTES.get()))
            result += noteName.substring(1)
                .replaceAll("##", "\u00D7")
                .replaceAll("#", "♯")
                .replaceAll("b", "\u266D");

        return result;
    }

}