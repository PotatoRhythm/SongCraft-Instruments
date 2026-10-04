package com.stump.songcraft_instruments.client.gui.instrument.partial.grid;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid.NoteGridButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.held.IHoldableNoteButton;
import com.stump.songcraft_instruments.client.keyMaps.InstrumentKeyMappings;
import com.stump.songcraft_instruments.client.midi.PressedMIDINote;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;

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

        final Integer pitchOffset = InstrumentKeyMappings.HEARTOPIA_KEY_TO_PITCH.get(keyCode);

        if (pitchOffset == null)
            return false;

        NoteGridButton visualButton = null;
        int visualPitch = Integer.MIN_VALUE;

        for (int column = 0; column < screen.columns(); column++) {
            for (int row = 0; row < screen.rows(); row++) {

                final NoteButton button = screen.getNoteButton(row, column);

                if (!(button instanceof NoteGridButton gridButton))
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

        final int targetPitch = pitchOffset + ModClientConfigs.TRANSPOSE.get();
        NoteGridButton closestButton = null;
        int closestDistance = Integer.MAX_VALUE;
        int closestSamplePitch = 0;

        for (int column = 0; column < screen.columns(); column++) {
            for (int row = 0; row < screen.rows(); row++) {
                final NoteButton button = screen.getNoteButton(row, column);

                if (!(button instanceof NoteGridButton gridButton))
                    continue;

                final int samplePitch = gridButton.getChromaticPitch();
                final int distance = Math.abs(targetPitch - samplePitch);

                if (distance < closestDistance) {
                    closestButton = gridButton;
                    closestDistance = distance;
                    closestSamplePitch = samplePitch;
                }
            }
        }

        if (closestButton == null)
            return true;

        final int newPitch = NoteSound.clampPitch(
                targetPitch - closestSamplePitch
        );

        final NoteSound sound = closestButton.getSound();

        if (visualButton instanceof IHoldableNoteButton heldButton) {
            final HeldNoteSound[] heldSounds =
                    screen.getHeldNoteSounds();

            if (heldSounds != null) {
                for (HeldNoteSound heldSound : heldSounds) {

                    if (heldSound != null
                            && sound.equals(heldSound.attack())) {

                        heldButton.setHeldNoteSound(heldSound);
                        break;
                    }
                }
            }
        }

        visualButton.unlockInput();
        final boolean played = visualButton.play(sound, newPitch);

        if (!played)
            return true;

        pressedNotes.put(keyCode, new PressedMIDINote(newPitch, visualButton, sound)
        );

        return true;
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