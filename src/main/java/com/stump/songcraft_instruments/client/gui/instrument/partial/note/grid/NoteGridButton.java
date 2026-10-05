package com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
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
        return getLabelTextureAt(getLabelTextureRow());
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
