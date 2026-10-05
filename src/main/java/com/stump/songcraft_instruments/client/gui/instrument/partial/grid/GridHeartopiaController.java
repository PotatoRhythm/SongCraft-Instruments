package com.stump.songcraft_instruments.client.gui.instrument.partial.grid;

import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid.NoteGridButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.held.IHoldableNoteButton;
import com.stump.songcraft_instruments.client.keyMaps.InstrumentKeyMappings;
import com.stump.songcraft_instruments.client.midi.PressedMIDINote;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class GridHeartopiaController {

    private final GridInstrumentScreen screen;
    private final GridInstrumentMidiReceiver midiReceiver;

    private final Map<Integer, PressedMIDINote> pressedNotes = new HashMap<>();

    public GridHeartopiaController(GridInstrumentScreen screen) {
        this.screen = screen;
        this.midiReceiver = new GridInstrumentMidiReceiver(screen);
    }

    // ---------------------------------------------------
    // KEY PRESS
    // ---------------------------------------------------

    public boolean handleKeyPress(int keyCode, int scanCode) {

        if (pressedNotes.containsKey(keyCode))
            return true;

        final Integer pitchOffset = getKeyPitch(keyCode);

        if (pitchOffset == null)
            return false;

        if (screen.isSSTI())
            return handleSSTIKeyPress(keyCode, pitchOffset);

        NoteGridButton visualButton = null;
        int visualPitch = Integer.MIN_VALUE;

        for (int row = 0; row < screen.rows(); row++) {
            for (int column = 0; column < screen.columns(); column++) {

                final NoteButton button = screen.getNoteButton(column, row);

                if (!(button instanceof NoteGridButton gridButton) || !gridButton.hasChromaticPitch())
                    continue;

                final int buttonPitch = gridButton.getChromaticPitch();

                if (buttonPitch <= pitchOffset && buttonPitch > visualPitch) {
                    visualButton = gridButton;
                    visualPitch = buttonPitch;
                }
            }
        }

        if (visualButton == null)
            return true;

        // Sharps are played on the natural note below them, raised a semitone.
        // The button picks its own sample, so the Ukulele's chords stay chords.
        visualButton.unlockInput();
        if (visualButton.play(pitchOffset - visualPitch))
            pressedNotes.put(keyCode, new PressedMIDINote(
                visualButton.getLastPlayedPitch(), visualButton, visualButton.getLastPlayedSound()
            ));

        return true;
    }

    /**
     * Single sound type instruments are chromatic, so each key plays the button of its exact pitch, sharps included
     */
    private boolean handleSSTIKeyPress(final int keyCode, final int pitch) {
        final int note = pitch - screen.getSSTILowestNote();
        if (note < 0)
            return true;

        final NoteButton button = screen.getNoteButtonByMIDINote(note);
        if (!(button instanceof NoteGridButton gridButton))
            return true;

        gridButton.unlockInput();
        if (gridButton.play())
            pressedNotes.put(keyCode, new PressedMIDINote(
                gridButton.getLastPlayedPitch(), gridButton, gridButton.getLastPlayedSound()
            ));

        return true;
    }

    /** The number of octaves the Heartopia keys span */
    private static final int HEARTOPIA_OCTAVES = 3;

    /**
     * Instruments with fewer octaves than the keys span are played with the highest keys,
     * so the pitches of the keys are lowered by the octaves left out
     * @return The pitch of the given key on this instrument, or null if it is not a Heartopia key
     */
    private @Nullable Integer getKeyPitch(final int keyCode) {
        final Integer pitch = InstrumentKeyMappings.HEARTOPIA_KEY_TO_PITCH.get(keyCode);
        if (pitch == null)
            return null;

        return pitch - Math.max(0, HEARTOPIA_OCTAVES - screen.rows()) * 12;
    }

    /**
     * @return The key whose pitch is exactly the given note's, or null if none is
     */
    public @Nullable Key getKey(final NoteGridButton button) {
        final int pitch;

        if (screen.isSSTI()) {
            // The inverse of GridInstrumentScreen#getNoteButtonByMIDINote
            pitch = button.column + NoteGrid.getFlippedRow(button.row, screen.rows()) * screen.columns()
                + screen.getSSTILowestNote();
        } else if (button.hasChromaticPitch()) {
            pitch = button.getChromaticPitch();
        } else {
            return null;
        }

        for (final int keyCode : InstrumentKeyMappings.HEARTOPIA_KEY_TO_PITCH.keySet())
            if (getKeyPitch(keyCode) == pitch)
                return InputConstants.Type.KEYSYM.getOrCreate(keyCode);

        return null;
    }

    // ---------------------------------------------------
    // KEY RELEASE
    // ---------------------------------------------------

    public boolean handleKeyRelease(int keyCode) {

        final PressedMIDINote prevNote = pressedNotes.remove(keyCode);

        if (prevNote == null)
            return InstrumentKeyMappings.HEARTOPIA_KEY_TO_PITCH.containsKey(keyCode);

        final NoteButton prevButton =
                prevNote.pressedNote();

        if (!(prevButton instanceof IHoldableNoteButton)) {
            prevButton.release();
            return true;
        }

        final IHoldableNoteButton heldButton = (IHoldableNoteButton) prevButton;
        heldButton.releaseHeld(prevNote.notePitch(), true, heldButton.toHeldSound(prevNote.sound()));

        return true;
    }
}