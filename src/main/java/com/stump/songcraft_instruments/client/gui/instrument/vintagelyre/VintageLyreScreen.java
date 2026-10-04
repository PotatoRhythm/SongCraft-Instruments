package com.stump.songcraft_instruments.client.gui.instrument.vintagelyre;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentMidiReceiver;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.NoteButton;
import com.stump.songcraft_instruments.client.gui.options.partial.InstrumentOptionsScreen;
import com.stump.songcraft_instruments.client.midi.InstrumentMidiReceiver;
import com.stump.songcraft_instruments.client.midi.MidiOverflowResult;
import com.stump.songcraft_instruments.client.midi.MidiOverflowResult.OverflowType;
import com.stump.songcraft_instruments.client.midi.PressedMIDINote;
import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class VintageLyreScreen extends GridInstrumentScreen {
    public static final ResourceLocation INSTRUMENT_ID = new ResourceLocation(SCInstrumentMod.MODID, "vintage_lyre");
    public static final String[] NOTE_LAYOUT = {
        "C", "Db", "Eb", "F", "G", "Ab", "Bb",
        "C", "D", "Eb", "F", "G", "A", "Bb",
        "C", "D", "Eb", "F", "G", "A", "Bb"
    };

    @Override
    public ResourceLocation getInstrumentId() {
        return INSTRUMENT_ID;
    }


    @Override
    public SoundOption getSoundOption() {
        return new SoundOption(SCSounds.VINTAGE_LYRE_NOTE_SOUNDS);
    }

    @Override
    public String[] noteLayout() {
        return shouldSoundNormalize()
            ? GridInstrumentScreen.NOTE_LAYOUT
            : NOTE_LAYOUT;
    }

    @Override
    protected InstrumentOptionsScreen initInstrumentOptionsScreen() {
        return new VintageLyreOptionsScreen(this);
    }

    public boolean shouldSoundNormalize() {
        return ModClientConfigs.NORMALIZE_VINTAGE_LYRE.get()
            // To normalize a flattened note, one must go up a note.
            // ...But we're maxed.
            && (getPitch() != NoteSound.getMaxPitch());
    }


    @Override
    public VintageNoteButton createNoteButton(int row, int column) {
        return new VintageNoteButton(row, column, this);
    }


    // Fix MIDI overflow for Vintage Lyre (pitch being offset by 1).
    // IMPORTANT: The last note of the lyre is KNOWN to be
    // flattened. The below fix is based off that assumption.
    @Override
    public InstrumentMidiReceiver initMidiReceiver() {
        return new GridInstrumentMidiReceiver(this) {
            @Override
            public PressedMIDINote playNote(NoteButton noteBtn, @Nullable MidiOverflowResult midiOverflow, int basePitch) {
                if ((midiOverflow == null) || (midiOverflow.type() != OverflowType.TOP))
                    return super.playNote(noteBtn, midiOverflow, basePitch);

                final int offsetFix = shouldSoundNormalize()
                    ? 1
                    : (((VintageNoteButton)(noteBtn)).isDefaultFlat() ? 0 : 1);

                // Account for Minecraft pitch overflow
                final int newPitch = basePitch + offsetFix;
                if (newPitch + midiOverflow.pitchOffset() > NoteSound.getMaxPitch())
                    return null;

                return super.playNote(noteBtn, midiOverflow, newPitch);
            }
        };
    }


    public static final InstrumentThemeLoader THEME_LOADER = new InstrumentThemeLoader(INSTRUMENT_ID);
    @Override
    public InstrumentThemeLoader getThemeLoader() {
        return THEME_LOADER;
    }
}
