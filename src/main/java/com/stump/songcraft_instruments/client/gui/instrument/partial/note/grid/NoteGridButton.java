package com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.config.enumType.NoteIconStyle;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.NoteGrid;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.render.NoteButtonRenderer;
import com.stump.songcraft_instruments.networking.buttonidentifier.NoteGridButtonIdentifier;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import com.stump.songcraft_instruments.util.LabelUtil;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

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
     * Jianpu labels, indexed by note.
     * Their octave dots are drawn separately; see {@link #getOctaveDot()}.
     */
    private static final ResourceLocation[] JIANPU_LABELS = new ResourceLocation[LabelUtil.ABC.length];
    static {
        for (int i = 0; i < LabelUtil.ABC.length; i++) {
            JIANPU_LABELS[i] = InstrumentScreen.getInternalResourceFromGlob(
                "note/label/grid_generic/" + Character.toLowerCase(LabelUtil.ABC[i]) + ".png"
            );
        }
    }


    public final int column, row;
    private NoteSound lastPlayedSound;
    private int lastPlayedPitch;

    public NoteGridButton(int column, int row, GridInstrumentScreen instrumentScreen) {
        super(
            getSoundFromArr(instrumentScreen, instrumentScreen.getInitSounds(), column, row),
            GridInstrumentScreen.getInitLabelSupplier(), instrumentScreen
        );
        
        this.column = column;
        this.row = row;
    }
    /**
     * Creates a button for an SSTI-type instrument
     */
    public NoteGridButton(int column, int row, GridInstrumentScreen instrumentScreen,
            int pitch) {
        super(instrumentScreen.getInitSounds()[0], instrumentScreen.getInitLabelSupplier(), instrumentScreen, pitch);

        this.column = column;
        this.row = row;
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
     * @return The position of this button ({@link NoteGridButton#column}, {@link NoteGridButton#row})
     * as an array index
     */
    public int posToIndex() {
        return column + NoteGrid.getFlippedRow(row, gridInstrument().rows()) * gridInstrument().columns();
    }
    /**
     * Evaluates the sound at the current position.
     * Meant for static initialization of sounds.
     * @param sounds The sound array of the instrument
     * @see NoteGridButton#posToIndex
     */
    protected static NoteSound getSoundFromArr(GridInstrumentScreen gridInstrument, NoteSound[] sounds, int column, int row) {
        return sounds[column + NoteGrid.getFlippedRow(row, gridInstrument.rows()) * gridInstrument.columns()];
    }


    /**
     * @return The key that plays this note in the current control mode, or null if none plays it as is
     */
    public @Nullable Key getKey() {
        return gridInstrument().getControlKey(this);
    }


    @Override
    public NoteGridButtonIdentifier getIdentifier() {
        return new NoteGridButtonIdentifier(this);
    }


    @Override
    protected NoteButtonRenderer initNoteRenderer() {
        return new NoteButtonRenderer(this, this::getLabelTexture);
    }

    protected int getLabelTextureColumn() {
        return ModClientConfigs.ACCURATE_NOTES.get() ? getABCOffset() : (column % GRID_LABELS.length);
    }

    protected ResourceLocation getLabelTextureAt(final int column) {
        return GRID_LABELS[column];
    }
    protected ResourceLocation getLabelTexture() {
        if (gridInstrument().getNoteIconStyle() == NoteIconStyle.JIANPU)
            return JIANPU_LABELS[getLabelTextureColumn()];

        return getLabelTextureAt(getLabelTextureColumn());
    }

    /**
     * @return The Jianpu octave index of this button's row:
     * 0 for rows below the middle one, 1 for the middle row, and 2 for rows above it
     */
    protected int getJianpuOctave() {
        final int rows = gridInstrument().rows();
        final int soundRow = NoteGrid.getFlippedRow(row, rows);
        return Integer.signum(soundRow - rows / 2) + 1;
    }

    @Override
    public int getOctaveDot() {
        return isJianpu() ? (getJianpuOctave() - 1) : 0;
    }

    protected boolean isJianpu() {
        return gridInstrument().getNoteIconStyle() == NoteIconStyle.JIANPU;
    }

    @Override
    public boolean usesGw2Buttons() {
        return gridInstrument().getNoteIconStyle() == NoteIconStyle.GW2;
    }
    // Genshin and Jianpu symbols are drawn pixel-exact and centered on the 15x15 buttons
    @Override
    public boolean usesPixelGridSymbol() {
        return !usesGw2Buttons();
    }
    @Override
    public boolean usesJianpuSymbol() {
        return isJianpu();
    }

    // Balance the octave dots, so the whole symbol looks centered rather than just the number
    @Override
    public int getPixelGridSymbolOffsetY() {
        return JIANPU_PIXEL_GRID_NUDGE[getJianpuOctave()];
    }
    /** Downward offset per Jianpu octave (low, middle, high), in button texture pixels */
    private static final int[] JIANPU_PIXEL_GRID_NUDGE = {0, 0, 0};


    @Override
    public int getNoteOffset() {
        return column + row * gridInstrument().columns();
    }

    private static final int[] NATURAL_NOTE_PITCHES = {
            0, 2, 4, 5, 7, 9, 11
    };

    /**
     * @return Whether this note has a {@link #getChromaticPitch() chromatic pitch};
     * notes past the 7 natural notes of a row, as on the Note Block Instrument, do not
     */
    public boolean hasChromaticPitch() {
        return column < NATURAL_NOTE_PITCHES.length;
    }

    public int getChromaticPitch() {
        int soundRow = NoteGrid.getFlippedRow(row, gridInstrument().rows());
        return soundRow * 12 + NATURAL_NOTE_PITCHES[column];
    }

    private record TransposedSound(NoteSound sound, int pitch) {}

    private TransposedSound getTransposedSound() {
        final GridInstrumentScreen screen = gridInstrument();

        /*
         * SSTI instruments do not use the standard C-major grid layout,
         * so do not calculate their pitch from column/row.
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
        final int column = index % gridInstrument().columns();
        final int row = index / gridInstrument().columns();

        return row * 12 + NATURAL_NOTE_PITCHES[column];
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
