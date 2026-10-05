package com.stump.songcraft_instruments.client.gui.instrument.partial.grid;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid.NoteGridButton;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.held.IHoldableNoteButton;
import com.stump.songcraft_instruments.client.keyMaps.InstrumentKeyMappings;
import com.stump.songcraft_instruments.client.midi.MidiOverflowResult;
import com.stump.songcraft_instruments.client.midi.PressedMIDINote;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * High-jack InstrumentMidiReceiver logic to implement octave mode controls
 */
public class GridOctaveSwapController {
    private final GridInstrumentScreen screen;
    private final GridInstrumentMidiReceiver midiReceiver;
    private final Map<Integer, PressedMIDINote> pressedNotes = new HashMap<>();

    public GridOctaveSwapController(GridInstrumentScreen screen) {
        this.screen = screen;
        this.midiReceiver = new GridInstrumentMidiReceiver(screen);
    }

    // ---------------------------------------------------
    // KEY PRESS
    // ---------------------------------------------------

    public void handleKeyPress(int keyCode, int scanCode) {
        if (pressedNotes.containsKey(keyCode)) return;

        if (InstrumentKeyMappings.OCTAVE_UP.get().matches(keyCode, scanCode)) {
            screen.setCurrentOctave(Math.min(screen.getCurrentOctave() + 1, screen.getMaxOctave()));
            return;
        }
        if (InstrumentKeyMappings.OCTAVE_DOWN.get().matches(keyCode, scanCode)) {
            screen.setCurrentOctave(Math.max(screen.getCurrentOctave() - 1, screen.getMinOctave()));
            return;
        }

        KeyMapping pressedHotkey = InstrumentKeyMappings.HOTKEY_TO_PITCH.keySet().stream()
                .filter(km -> km.matches(keyCode, scanCode))
                .findFirst()
                .orElse(null);
        if (pressedHotkey == null)
            return;

        int baseNote = InstrumentKeyMappings.HOTKEY_TO_PITCH.get(pressedHotkey);
        int targetNote = baseNote + (screen.getCurrentOctave() * 12) + 12;

        // Forbidden high 8
        if (screen.rows() == 3 && targetNote == 48 || screen.rows() == 2 && targetNote == 36) {
            return;
        }

        screen.resetTransposition();

        final MidiOverflowResult overflowRes = midiReceiver.handleMidiOverflow(targetNote);
        if (overflowRes != null) {
            targetNote = overflowRes.fixedOctaveNote();
            int newInsPitch = overflowRes.pitchOffset() + screen.getPitch();
            if ((newInsPitch < NoteSound.getMinPitch()) || (newInsPitch > NoteSound.getMaxPitch())) {
                screen.setPitch(0);
            }
        }

        int basePitch = screen.getPitch();
        final NoteButton pressedNote = midiReceiver.handleMidiPress(targetNote, 0);
        if (pressedNote != null) {
            pressedNote.unlockInput();
            final PressedMIDINote pressedNoteObj = midiReceiver.playNote(pressedNote, overflowRes, basePitch);
            if (pressedNoteObj != null) {
                pressedNotes.put(keyCode, pressedNoteObj);
            }
        }
    }

    /**
     * @return The hotkey of the given note's natural, the same in every octave, or null if it has none.
     * The high C hotkey is left out, as the C hotkey already labels every C.
     */
    public @Nullable Key getKey(final NoteGridButton button) {
        if (!button.hasChromaticPitch())
            return null;

        final int pitch = button.getChromaticPitch() % 12;

        for (final Map.Entry<KeyMapping, Integer> hotkey : InstrumentKeyMappings.HOTKEY_TO_PITCH.entrySet())
            if ((hotkey.getValue() == pitch) && !hotkey.getKey().isUnbound())
                return hotkey.getKey().getKey();

        return null;
    }

    // ---------------------------------------------------
    // KEY RELEASE
    // ---------------------------------------------------

    public void handleKeyRelease(int keyCode) {
        final PressedMIDINote prevNoteBtn = pressedNotes.get(keyCode);
        NoteButton prevButton = null;
        boolean isHoldableBtn = false;

        if (prevNoteBtn != null) {
            prevButton = prevNoteBtn.pressedNote();
            isHoldableBtn = prevButton instanceof IHoldableNoteButton;

            if (!isHoldableBtn) {
                prevButton.release();
            }

            pressedNotes.remove(keyCode);
        }

        if (isHoldableBtn) {
            IHoldableNoteButton heldBtn = (IHoldableNoteButton) prevButton;
            heldBtn.releaseHeld(prevNoteBtn.notePitch(), true, heldBtn.toHeldSound(prevNoteBtn.sound()));
        }
    }
}
