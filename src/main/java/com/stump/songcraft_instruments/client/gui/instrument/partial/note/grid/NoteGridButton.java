package com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.NoteIconStyle;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.NoteGrid;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.render.NoteButtonRenderer;
import com.stump.songcraft_instruments.client.keyMaps.InstrumentKeyMappings;
import com.stump.songcraft_instruments.networking.buttonidentifier.NoteGridButtonIdentifier;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import com.stump.songcraft_instruments.util.LabelUtil;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NoteGridButton extends NoteButton {
    private static final ResourceLocation[] GRID_LABELS = new ResourceLocation[LabelUtil.ABC.length];
    static {
        for (int i = 0; i < LabelUtil.ABC.length; i++) {
            GRID_LABELS[i] = InstrumentScreen.getInternalResourceFromGlob(
                "note/label/grid/" + Character.toLowerCase(LabelUtil.ABC[i]) + ".png"
            );
        }
    }
    /**
     * Jianpu labels, indexed by [octave][note].
     * Octaves are ordered low, middle, high.
     */
    private static final ResourceLocation[][] JIANPU_LABELS = new ResourceLocation[3][LabelUtil.ABC.length];
    private static final String[] JIANPU_OCTAVE_SUFFIXES = {"_low", "", "_high"};
    static {
        for (int octave = 0; octave < JIANPU_LABELS.length; octave++) {
            for (int i = 0; i < LabelUtil.ABC.length; i++) {
                JIANPU_LABELS[octave][i] = InstrumentScreen.getInternalResourceFromGlob(
                    "note/label/grid_generic/" + Character.toLowerCase(LabelUtil.ABC[i])
                        + JIANPU_OCTAVE_SUFFIXES[octave] + ".png"
                );
            }
        }
    }


    public final int row, column;
    private NoteSound lastPlayedSound;
    private int lastPlayedPitch;

    public NoteGridButton(int row, int column, GridInstrumentScreen instrumentScreen) {
        super(
            getSoundFromArr(instrumentScreen, instrumentScreen.getInitSounds(), row, column),
            GridInstrumentScreen.getInitLabelSupplier(), instrumentScreen
        );
        
        this.row = row;
        this.column = column;
    }
    /**
     * Creates a button for an SSTI-type instrument
     */
    public NoteGridButton(int row, int column, GridInstrumentScreen instrumentScreen,
            int pitch) {
        super(instrumentScreen.getInitSounds()[0], instrumentScreen.getInitLabelSupplier(), instrumentScreen, pitch);

        this.row = row;
        this.column = column;
    }

    public GridInstrumentScreen gridInstrument() {
        return (GridInstrumentScreen) instrumentScreen;
    }


    public void updateSoundArr() {
        final NoteGrid grid = gridInstrument().noteGrid;
        final NoteSound[] sounds = grid.getNoteSounds();

        setSound(gridInstrument().isSSTI() ? sounds[0]
            : sounds[posToIndex()]
        );
    }

    /**
     * @return The position of this button ({@link NoteGridButton#row}, {@link NoteGridButton#column})
     * as an array index
     */
    public int posToIndex() {
        return row + NoteGrid.getFlippedColumn(column, gridInstrument().columns()) * gridInstrument().rows();
    }
    /**
     * Evaluates the sound at the current position.
     * Meant for static initialization of sounds.
     * @param sounds The sound array of the instrument
     * @see NoteGridButton#posToIndex
     */
    protected static NoteSound getSoundFromArr(GridInstrumentScreen gridInstrument, NoteSound[] sounds, int row, int column) {
        return sounds[row + NoteGrid.getFlippedColumn(column, gridInstrument.columns()) * gridInstrument.rows()];
    }


    public Key getKey() {
        return InstrumentKeyMappings.GENSHIN_INSTRUMENT_MAPPINGS[column][row];
    }


    @Override
    public NoteGridButtonIdentifier getIdentifier() {
        return new NoteGridButtonIdentifier(this);
    }


    @Override
    protected NoteButtonRenderer initNoteRenderer() {
        return new NoteButtonRenderer(this, this::getLabelTexture);
    }

    protected int getLabelTextureRow() {
        return ModClientConfigs.ACCURATE_NOTES.get() ? getABCOffset() : (row % GRID_LABELS.length);
    }

    protected ResourceLocation getLabelTextureAt(final int row) {
        return GRID_LABELS[row];
    }
    protected ResourceLocation getLabelTexture() {
        if (gridInstrument().getNoteIconStyle() == NoteIconStyle.JIANPU)
            return JIANPU_LABELS[getJianpuOctave()][getLabelTextureRow()];

        return getLabelTextureAt(getLabelTextureRow());
    }

    /**
     * @return The Jianpu octave index of this button's column:
     * 0 for columns below the middle one, 1 for the middle column, and 2 for columns above it
     */
    protected int getJianpuOctave() {
        final int columns = gridInstrument().columns();
        final int soundColumn = NoteGrid.getFlippedColumn(column, columns);
        return Integer.signum(soundColumn - columns / 2) + 1;
    }

    protected boolean isJianpu() {
        return gridInstrument().getNoteIconStyle() == NoteIconStyle.JIANPU;
    }
    protected boolean hasLabel() {
        return !getMessage().getString().isEmpty();
    }

    // Without a label, Jianpu symbols get the bigger pixel-exact look
    @Override
    public boolean usesPixelGridSymbol() {
        return isJianpu() && !hasLabel();
    }

    // Balance the octave dots, so the whole symbol looks centered rather than just the number
    @Override
    public int getPixelGridSymbolOffsetY() {
        return JIANPU_PIXEL_GRID_NUDGE[getJianpuOctave()];
    }
    /** Downward offset per Jianpu octave (low, middle, high), in button texture pixels */
    private static final int[] JIANPU_PIXEL_GRID_NUDGE = {0, 0, 0};

    /** The first and last visible rows of the Jianpu symbols per octave (low, middle, high) */
    private static final int[][] JIANPU_VISIBLE_ROWS = {{2, 8}, {2, 6}, {0, 6}};
    /** Where the inside of the button's inner ring begins, in button texture pixels */
    private static final int BUTTON_INNER_TOP = 2;

    /**
     * Centers the visible Jianpu symbol (number and octave dot) between the inside of the
     * button's ring and the top of the label, so the space above and below it is equal.
     */
    @Override
    public int getSymbolOffsetY() {
        if (!isJianpu() || !hasLabel())
            return 0;

        // The symbol is stretched over half the button; see NoteButtonRenderer#renderNoteSymbol
        final int symbolBoxHeight = getHeight() / 2;
        final float symbolPixel = symbolBoxHeight / (float) NoteButtonRenderer.JIANPU_SYMBOL_HEIGHT;
        final float buttonPixel = getHeight() / (float) NoteButtonRenderer.BUTTON_TEXTURE_SIZE;

        final int[] rows = JIANPU_VISIBLE_ROWS[getJianpuOctave()];
        final float visibleHeight = (rows[1] - rows[0] + 1) * symbolPixel;

        final float spaceTop = getY() + BUTTON_INNER_TOP * buttonPixel;
        final float labelTop = getInitY() + getInitHeight() / 2 + NoteButtonRenderer.LABEL_OFFSET_Y;
        final float visibleTop = spaceTop + (labelTop - spaceTop - visibleHeight) / 2;

        final float drawY = visibleTop - rows[0] * symbolPixel;
        final int defaultY = getY() + symbolBoxHeight / 2;
        return Math.round(drawY - defaultY);
    }


    @Override
    public int getNoteOffset() {
        return row + column * gridInstrument().rows();
    }

    private static final int[] NATURAL_NOTE_PITCHES = {
            0, 2, 4, 5, 7, 9, 11
    };

    public int getChromaticPitch() {
        int soundColumn = NoteGrid.getFlippedColumn(column, gridInstrument().columns());
        return soundColumn * 12 + NATURAL_NOTE_PITCHES[row];
    }

    private record TransposedSound(NoteSound sound, int pitch) {}

    private TransposedSound getTransposedSound() {
        final GridInstrumentScreen screen = gridInstrument();

        /*
         * SSTI instruments do not use the standard C-major grid layout,
         * so do not calculate their pitch from row/column.
         */
        if (screen.isSSTI()) {
            return new TransposedSound(getSound(), getPitch() + ModClientConfigs.TRANSPOSE.get());
        }

        final int transpose = screen.getPitch() + ModClientConfigs.TRANSPOSE.get();

        if ((transpose == 0) && canTransposeTo(posToIndex())) {
            return new TransposedSound(getSound(), 0);
        }

        final int targetPitch = getChromaticPitch() + transpose;

        final NoteSound[] sounds = screen.getInitSounds();

        if (sounds == null || sounds.length == 0)
            return new TransposedSound(getSound(), getPitch() + transpose);

        NoteSound closestSound = null;
        int closestPitch = 0;
        int closestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < sounds.length; i++) {
            if (!canTransposeTo(i))
                continue;

            final int samplePitch = getSampleChromaticPitch(i);
            final int distance = Math.abs(targetPitch - samplePitch);

            if (distance < closestDistance) {
                closestSound = sounds[i];
                closestPitch = samplePitch;
                closestDistance = distance;
            }
        }

        if (closestSound == null)
            return new TransposedSound(getSound(), getPitch() + transpose);

        return new TransposedSound(closestSound, targetPitch - closestPitch);
    }

    /**
     * @param sampleIndex An index of the instrument's sound array
     * @return Whether this note may play the given sample, pitched to its own note.
     * For instruments whose sound array holds more than one octave of single notes, such as the Ukulele's chords.
     */
    public boolean canTransposeTo(final int sampleIndex) {
        return true;
    }
    /**
     * @return Whether this note's sound is a plain note at its {@link #getChromaticPitch() chromatic pitch},
     * that other notes may be pitched from
     */
    public boolean isTransposeSource() {
        return true;
    }

    private int getSampleChromaticPitch(int index) {
        final int row = index % gridInstrument().rows();
        final int column = index / gridInstrument().rows();

        return column * 12 + NATURAL_NOTE_PITCHES[row];
    }

    @Override
    public boolean play() {
        final TransposedSound transposedSound = getTransposedSound();

        if (this instanceof HeldGridNoteButton heldButton) {
            HeldNoteSound[] heldSounds = gridInstrument().getHeldNoteSounds();

            if (heldSounds != null) {
                final HeldNoteSound heldSound = findHeldSound(heldSounds, transposedSound.sound());

                if (heldSound != null)
                    heldButton.setHeldNoteSound(heldSound);
            }
        }

        lastPlayedSound = transposedSound.sound();
        lastPlayedPitch = transposedSound.pitch();

        return play(transposedSound.sound(), transposedSound.pitch());
    }

    public NoteSound getLastPlayedSound() {
        return lastPlayedSound;
    }

    public int getLastPlayedPitch() {
        return lastPlayedPitch;
    }

    private HeldNoteSound findHeldSound(HeldNoteSound[] heldSounds, NoteSound attackSound) {
        for (HeldNoteSound heldSound : heldSounds) {
            if (heldSound != null && attackSound.equals(heldSound.attack()))
                return heldSound;
        }

        return null;
    }
}
