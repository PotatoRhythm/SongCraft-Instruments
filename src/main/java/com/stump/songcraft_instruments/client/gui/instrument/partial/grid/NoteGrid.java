package com.stump.songcraft_instruments.client.gui.instrument.partial.grid;

import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid.NoteGridButton;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.gui.layouts.AbstractLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.GridLayout.RowHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;


/**
 * A class holding an abstract {@link NoteButton note} grid for {@link GridInstrumentScreen}.
 * All fields are described in there.
 */
@OnlyIn(Dist.CLIENT)
public class NoteGrid implements Iterable<NoteGridButton> {

    public static int getPaddingHorz() {
        return 9;
    }
    public static int getPaddingVert() {
        return 7;
    }

    
    public final GridInstrumentScreen instrumentScreen;
    protected final NoteGridButton[][] notes;
    private NoteSound[] noteSounds;

    public final int columns, rows;

    public NoteGrid(GridInstrumentScreen instrumentScreen, SSTIPitchProvider pitchProvider) {
        this.instrumentScreen = instrumentScreen;
        
        columns = instrumentScreen.columns();
        rows = instrumentScreen.rows();
        
        noteSounds = instrumentScreen.getInitSounds();


        // Construct the note grid
        notes = new NoteGridButton[rows][columns];
        for (int i = 0; i < rows; i++) {
            final NoteGridButton[] buttonRow = new NoteGridButton[columns];
            // Rows should start from the bottom/lowest pitch, unlike how an array axis' structure is sorted.
            // Hence, we flip the Y index:
            final int row = getFlippedRow(i);

            for (int j = 0; j < columns; j++)
                if (instrumentScreen.isSSTI()) {
                    buttonRow[j] = instrumentScreen.createNoteButton(j, row,
                        // Provide the flipped row to the provider
                        // because the lowest pitch should be at the bottom and not vice-versa
                        pitchProvider.get(j, i)
                    );
                } else
                    buttonRow[j] = instrumentScreen.createNoteButton(j, row);

            
            notes[i] = buttonRow;
        }
    }
    public NoteGrid(GridInstrumentScreen instrumentScreen) {
        this(instrumentScreen, null);
    }

    /**
     * Constructs a linearly increasing pitch note grid for an SSTI-type instrument.
     * @param beginningNote The note to start the linear pitch increment.
     * @param noteSkip The amount of pitch to skip over every note in the linear pitch increment.
     */
    public NoteGrid(GridInstrumentScreen instrumentScreen, int beginningNote, int noteSkip) {
        this(instrumentScreen, (column, row) -> beginningNote +
                (noteSkip * (column + row * instrumentScreen.columns()))
        );
    }
    /**
     * Constructs a linearly increasing pitch note grid for an SSTI-type instrument. The increment is set to 1.
     * @param beginningNote The note to start the linear pitch increment.
     */
    public NoteGrid(GridInstrumentScreen instrumentScreen, int beginningNote) {
        this(instrumentScreen, beginningNote, 1);
    }

    public NoteSound[] getNoteSounds() {
        return noteSounds;
    }
    public void setNoteSounds(final NoteSound[] noteSounds) {
        this.noteSounds = noteSounds;
        forEach(NoteGridButton::updateSoundArr);
    }


    public HashMap<Key, NoteButton> genKeyboardMap(final Key[][] keyMap) {
        final HashMap<Key, NoteButton> result = new HashMap<>(columns * rows);

        for (int i = 0; i < rows; i++)
            for (int j = 0; j < columns; j++)
                result.put(keyMap[i][j], notes[getFlippedRow(i)][j]);
                
        return result;
    }
    

    /**
     * Constructs a new grid of notes as described in this object.
     * @param vertAlignment A percentage determining the vertical offset of the grid
     * @param screenWidth The width of the screen
     * @param screenHeight The height of the screen
     * @return A new {@link NoteButton} grid
     */
    public AbstractLayout initNoteGridLayout(final float vertAlignment, final int screenWidth, final int screenHeight) {
        final GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(getPaddingHorz(), getPaddingVert());

        final RowHelper rowHelper = grid.createRowHelper(columns);
        forEach(rowHelper::addChild);

        
        grid.arrangeElements();
        FrameLayout.alignInRectangle(grid, 0, 0, screenWidth, screenHeight, 0.5f, vertAlignment);
        grid.arrangeElements();
        
        // Initialize all the notes
        forEach(NoteButton::init);

        return grid;
    }


    public NoteButton getNoteButton(final int column, final int row) throws IndexOutOfBoundsException {
        return notes[row][column];
    }

    /**
     * Maps an array row to a note grid row by flipping it
     * @return The flipped row of {@code row}
     */
    public int getFlippedRow(final int row) {
        return getFlippedRow(row, rows);
    }
    /**
     * Maps an array row to a note grid row by flipping it
     * @return The flipped row of {@code row}
     */
    public static int getFlippedRow(final int row, final int rows) {
        return rows - 1 - row;
    }


    @FunctionalInterface
    public static interface SSTIPitchProvider {
        int get(final int column, final int row);
    }

    @Override
    public @NotNull Iterator<NoteGridButton> iterator() {
        // This is a basic 2x2 matrix iterator
        return new Iterator<NoteGridButton>() {

            private int i, j;

            @Override
            public boolean hasNext() {
                return i < rows;
            }

            @Override
            public NoteGridButton next() {
                final NoteGridButton btn = notes[getFlippedRow(i)][j];

                if (j >= (columns - 1)) {
                    j = 0;
                    i++;
                } else
                    j++;

                return btn;
            }

        };
    }
}
