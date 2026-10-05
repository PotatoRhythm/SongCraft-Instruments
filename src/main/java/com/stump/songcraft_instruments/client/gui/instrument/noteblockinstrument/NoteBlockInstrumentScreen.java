package com.stump.songcraft_instruments.client.gui.instrument.noteblockinstrument;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.item.NoteBlockInstrumentItem;
import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.client.gui.instrument.floralzither.FloralZitherScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.InstrumentThemeLoader;
import com.stump.songcraft_instruments.client.gui.instrument.partial.grid.GridInstrumentScreen;
import com.stump.songcraft_instruments.client.gui.instrument.partial.note.grid.NoteGridButton;
import com.stump.songcraft_instruments.client.midi.InstrumentMidiReceiver;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NoteBlockInstrumentScreen extends GridInstrumentScreen {
    public static final String[] NOTES_LAYOUT = {"F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F"};

    public final NoteBlockInstrument instrumentType;
    public final ResourceLocation instrumentId;
    private final SoundOption soundOption;
    
    public NoteBlockInstrumentScreen(final NoteBlockInstrument instrumentType) {
        this.instrumentType = instrumentType;
        instrumentId = new ResourceLocation(SCInstrumentMod.MODID, NoteBlockInstrumentItem.getId(instrumentType));

        // Update the sound to match the note block's
        this.soundOption = new SoundOption(SCSounds.getNoteblockSounds(instrumentType));
    }

    @Override
    public SoundOption getSoundOption() {
        return soundOption;
    }


    @Override
    public int columns() {
        return 8;
    }

    public int getNoteSize() {
        return (int)(super.getNoteSize() * .85f);
    }
    @Override
    public NoteGridButton createNoteButton(int column, int row, int pitch) {
        return new NoteBlockInstrumentNote(column, row, this, pitch);
    }


    @Override
    public ResourceLocation getInstrumentId() {
        return instrumentId;
    }
    

    @Override
    public NoteSound[] getInitSounds() {
        return getSoundOption().getNoteSounds();
    }

    @Override
    public String[] noteLayout() {
        return NOTES_LAYOUT;
    }

    @Override
    public boolean isSSTI() {
        return true;
    }
    // Starts at F#
    @Override
    public int getSSTILowestNote() {
        return 6;
    }


    @Override
    public InstrumentMidiReceiver initMidiReceiver() {
        return new NoteBlockInstrumentMIDIReceiver(this);
    }

    public static final InstrumentThemeLoader THEME_LOADER = InstrumentThemeLoader.fromOther(
        FloralZitherScreen.THEME_LOADER,
        new ResourceLocation(SCInstrumentMod.MODID, "note_block_instrument")
    );
        
    @Override
    public InstrumentThemeLoader getThemeLoader() {
        return THEME_LOADER;
    }
}
