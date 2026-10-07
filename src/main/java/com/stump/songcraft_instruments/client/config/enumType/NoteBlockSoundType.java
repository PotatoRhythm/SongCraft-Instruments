package com.stump.songcraft_instruments.client.config.enumType;

import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.SoundOption;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * The Note Block Instrument's sounds: every tunable note block sound, each over 3 octaves starting on C.
 */
@OnlyIn(Dist.CLIENT)
public enum NoteBlockSoundType implements LayoutSoundType {
    HARP(NoteBlockInstrument.HARP),
    BASEDRUM(NoteBlockInstrument.BASEDRUM),
    SNARE(NoteBlockInstrument.SNARE),
    HAT(NoteBlockInstrument.HAT),
    BASS(NoteBlockInstrument.BASS),
    FLUTE(NoteBlockInstrument.FLUTE),
    BELL(NoteBlockInstrument.BELL),
    GUITAR(NoteBlockInstrument.GUITAR),
    CHIME(NoteBlockInstrument.CHIME),
    XYLOPHONE(NoteBlockInstrument.XYLOPHONE),
    IRON_XYLOPHONE(NoteBlockInstrument.IRON_XYLOPHONE),
    COW_BELL(NoteBlockInstrument.COW_BELL),
    DIDGERIDOO(NoteBlockInstrument.DIDGERIDOO),
    BIT(NoteBlockInstrument.BIT),
    BANJO(NoteBlockInstrument.BANJO),
    PLING(NoteBlockInstrument.PLING);

    private final Supplier<SoundOption> soundArr;
    private NoteBlockSoundType(final NoteBlockInstrument instrument) {
        this.soundArr = () -> new SoundOption(SCSounds.getNoteBlockSounds(instrument));
    }

    @Override
    public Supplier<SoundOption> getSoundArr() {
        return soundArr;
    }

    @Override
    public int rows() {
        return 3;
    }
}
