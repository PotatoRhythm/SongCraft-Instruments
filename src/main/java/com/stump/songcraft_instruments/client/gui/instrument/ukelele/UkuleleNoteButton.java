package com.stump.songcraft_instruments.client.gui.instrument.ukelele;

import com.stump.songcraft_instruments.client.config.enumType.NoteGridLabel;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.render.NoteButtonRenderer;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteNotation;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid.NoteGridButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.label.NoteLabelSupplier;
import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.NoteSound;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class UkuleleNoteButton extends NoteGridButton {
    private static final List<NoteLabelSupplier>
        NOTATIONAL_LABELS = Stream.of(
            NoteGridLabel.NOTE_NAME,
            NoteGridLabel.DO_RE_MI
        ).map(NoteGridLabel::getLabelSupplier).toList()
    ;

    public UkuleleNoteButton(int column, int row, GridInstrumentScreen instrumentScreen) {
        super(column, row, instrumentScreen);
    }

    public UkuleleScreen ukuleleScreen() {
        return (UkuleleScreen) instrumentScreen;
    }


    @Override
    protected NoteButtonRenderer initNoteRenderer() {
        return new UkuleleNoteButtonRenderer(this, this::getLabelTexture);
    }

    @Override
    public NoteNotation getNotation() {
        if (ukuleleScreen().isTopRegular())
            return super.getNotation();

        if (row == 0)
            return NoteNotation.NONE;
        return super.getNotation();
    }

    @Override
    public @NotNull Component getMessage() {
        if (ukuleleScreen().isTopRegular())
            return super.getMessage();

        // Change the top row if it is of a notational label type.
        // (As defined above.)
        if (row == 0) {
            if (NOTATIONAL_LABELS.contains(getLabelSupplier())) {
                return Component.literal(getChordNameOfColumn());
            }
        }

        return super.getMessage();
    }

    /**
     * @return The chord this button plays, following the transposition keys and the transpose setting
     */
    public String getChordNameOfColumn() {
        return getTransposedNoteName();
    }


    /**
     * @return Whether this note plays a chord
     */
    public boolean isChord() {
        return !ukuleleScreen().isTopRegular() && (row == 0);
    }

    // The chord samples sit atop the sound array, where a 3rd octave would.
    // Only chords may be pitched from them, and chords only from them.
    // A chord is only pitched from a chord of its own type, so a minor chord never borrows a major one.
    @Override
    public boolean canTransposeTo(final int sampleIndex) {
        final int columns = gridInstrument().columns();
        final boolean chordSample = (sampleIndex / columns) == (gridInstrument().rows() - 1);
        if (chordSample != isChord())
            return false;
        if (!isChord())
            return true;

        // The chord row is the top row of the layout, so its columns match the chord samples'
        final String[] layout = ukuleleScreen().noteLayout();
        return getChordType(layout[sampleIndex % columns]).equals(getChordType(layout[column]));
    }

    /**
     * @return The chord's type, without its root: empty for major, {@code m} for minor, {@code 7} for dominant 7th
     */
    private static String getChordType(final String chord) {
        int i = 1;
        while (i < chord.length() && (chord.charAt(i) == '#' || chord.charAt(i) == 'b'))
            i++;

        return chord.substring(i);
    }


    // Extending 2nd octave:

    @Override
    public NoteSound getSound() {
        if (!ukuleleScreen().isTopRegular() || row != 0)
            return super.getSound();

        // 13 = B2
        return SCSounds.UKULELE[13];
    }

    @Override
    public int getPitch() {
        if (!ukuleleScreen().isTopRegular() || row != 0)
            return super.getPitch();

        // Bump the pitch from B2 to whatever eow we are in.

        // Lazily do the pitch bumping operation (I'm lazy)
        final int pitchBump = switch (column) {
            case 0 -> 1;
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 6;
            case 4 -> 8;
            case 5 -> 10;
            case 6 -> 12;
            default -> throw new IllegalStateException("Unexpected value: " + column);
        };

        return super.getPitch() + pitchBump;
    }
}
