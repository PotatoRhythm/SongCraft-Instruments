package com.stump.songcraft_instruments.item.emirecord;

import com.stump.songcraft_instruments.block.util.WritableNoteType;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.util.CommonUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import static com.stump.songcraft_instruments.block.blockentity.looper.LooperRecordWriter.PARTICLE_COLOR_TAG;
import static com.stump.songcraft_instruments.block.blockentity.looper.LooperRecordWriter.PERFORMER_TAG;
import static com.stump.songcraft_instruments.item.emirecord.EMIRecordItem.*;

/**
 * The notes of a record's channel, packed as one int array per note.
 * <p>
 * Records travel to clients with their item, and Minecraft refuses item NBT over 2MB.
 * A note as a compound of named tags costs ~1KB of that, so long (group) recordings would get players kicked.
 * Packed, a note costs ~64 bytes. Instrument and sound IDs are stored once per record, in palettes the notes index into.
 * <p>
 * Records written before packing are converted by {@link #pack}.
 */
public final class RecordNotes {
    public static final String
        NOTE_DATA_TAG = "NoteData",
        INSTRUMENT_IDS_TAG = "InstrumentIds",
        SOUND_TYPES_TAG = "SoundTypes"
    ;

    // The fields of a packed note, by index.
    // Their order is stored in records; only ever append new fields.
    public static final int
        TIMESTAMP = 0,
        // A WritableNoteType ordinal
        TYPE = 1,
        PERFORMER = 2,
        // Dampens end here
        PITCH = 3,
        // In percent
        VOLUME = 4,
        PARTICLE_COLOR = 5,
        // Indices into the INSTRUMENT_IDS_TAG and SOUND_TYPES_TAG palettes
        INSTRUMENT_ID = 6,
        SOUND_TYPE = 7,
        SOUND_INDEX = 8,
        // Held notes only: a HeldSoundPhase ordinal
        HELD_PHASE = 9
    ;

    private RecordNotes() {}


    public static ListTag getNotes(final CompoundTag channel) {
        return channel.getList(NOTE_DATA_TAG, Tag.TAG_INT_ARRAY);
    }
    public static boolean hasNotes(final CompoundTag channel) {
        return !getNotes(channel).isEmpty() || !channel.getList(NOTES_TAG, Tag.TAG_COMPOUND).isEmpty();
    }

    public static void clear(final CompoundTag channel) {
        channel.remove(NOTE_DATA_TAG);
        channel.remove(INSTRUMENT_IDS_TAG);
        channel.remove(SOUND_TYPES_TAG);
        channel.remove(NOTES_TAG);
    }


    public static void addDampen(final CompoundTag channel, final int timestamp, final int performer) {
        CommonUtil.getOrCreateListTag(channel, NOTE_DATA_TAG).add(new IntArrayTag(new int[] {
            timestamp, WritableNoteType.DAMPEN.ordinal(), performer
        }));
    }

    public static void addNote(final CompoundTag channel, final int timestamp, final int performer,
                               final int pitch, final int volume, final int particleColor,
                               final ResourceLocation instrumentId, final ResourceLocation soundType, final int soundIndex) {
        CommonUtil.getOrCreateListTag(channel, NOTE_DATA_TAG).add(new IntArrayTag(new int[] {
            timestamp, WritableNoteType.REGULAR.ordinal(), performer,
            pitch, volume, particleColor,
            paletteIndex(channel, INSTRUMENT_IDS_TAG, instrumentId), paletteIndex(channel, SOUND_TYPES_TAG, soundType), soundIndex
        }));
    }

    public static void addHeldNote(final CompoundTag channel, final int timestamp, final int performer,
                                   final int pitch, final int volume, final int particleColor,
                                   final ResourceLocation instrumentId, final ResourceLocation soundType, final int soundIndex,
                                   final HeldSoundPhase phase) {
        CommonUtil.getOrCreateListTag(channel, NOTE_DATA_TAG).add(new IntArrayTag(new int[] {
            timestamp, WritableNoteType.HELD.ordinal(), performer,
            pitch, volume, particleColor,
            paletteIndex(channel, INSTRUMENT_IDS_TAG, instrumentId), paletteIndex(channel, SOUND_TYPES_TAG, soundType), soundIndex,
            phase.ordinal()
        }));
    }


    public static WritableNoteType getType(final int[] note) {
        return WritableNoteType.values()[note[TYPE]];
    }
    public static HeldSoundPhase getHeldPhase(final int[] note) {
        return HeldSoundPhase.values()[note[HELD_PHASE]];
    }
    public static ResourceLocation getInstrumentId(final CompoundTag channel, final int[] note) {
        return new ResourceLocation(channel.getList(INSTRUMENT_IDS_TAG, Tag.TAG_STRING).getString(note[INSTRUMENT_ID]));
    }
    public static ResourceLocation getSoundType(final CompoundTag channel, final int[] note) {
        return new ResourceLocation(channel.getList(SOUND_TYPES_TAG, Tag.TAG_STRING).getString(note[SOUND_TYPE]));
    }

    /**
     * @return The index of the value in the palette, added to it if not yet present
     */
    private static int paletteIndex(final CompoundTag channel, final String paletteKey, final ResourceLocation value) {
        final ListTag palette = CommonUtil.getOrCreateListTag(channel, paletteKey);
        final String rawValue = value.toString();

        for (int i = 0; i < palette.size(); i++) {
            if (palette.getString(i).equals(rawValue))
                return i;
        }

        palette.add(StringTag.valueOf(rawValue));
        return palette.size() - 1;
    }


    /**
     * Converts the channel's notes from compounds of named tags (as recorded before packing) to packed notes.
     * @return Whether there was anything to convert
     */
    public static boolean pack(final CompoundTag channel) {
        if (!channel.contains(NOTES_TAG, Tag.TAG_LIST))
            return false;

        final ListTag legacyNotes = channel.getList(NOTES_TAG, Tag.TAG_COMPOUND);
        channel.remove(NOTES_TAG);

        for (final Tag tag : legacyNotes) {
            final CompoundTag note = (CompoundTag) tag;
            final String rawType = note.getString(NOTE_TYPE);
            // Older versions only had regular notes
            final WritableNoteType type = rawType.isEmpty() ? WritableNoteType.REGULAR : WritableNoteType.valueOf(rawType);

            final int timestamp = note.getInt(TIMESTAMP_TAG), performer = note.getInt(PERFORMER_TAG);

            if (type == WritableNoteType.DAMPEN) {
                addDampen(channel, timestamp, performer);
                continue;
            }

            final int pitch = note.getInt(PITCH_TAG),
                volume = (int) (note.getFloat(VOLUME_TAG) * 100),
                particleColor = note.getInt(PARTICLE_COLOR_TAG),
                soundIndex = note.getInt(SOUND_INDEX_TAG);
            // Records from before group recordings hold a single instrument for all notes
            final ResourceLocation instrumentId = new ResourceLocation(note.contains(INSTRUMENT_ID_TAG, Tag.TAG_STRING)
                    ? note.getString(INSTRUMENT_ID_TAG) : channel.getString(INSTRUMENT_ID_TAG)),
                soundType = new ResourceLocation(note.getString(SOUND_TYPE_TAG));

            if (type == WritableNoteType.HELD) {
                addHeldNote(channel, timestamp, performer, pitch, volume, particleColor, instrumentId, soundType, soundIndex,
                    HeldSoundPhase.valueOf(note.getString(EMIRecordItem.HELD_PHASE)));
            } else {
                addNote(channel, timestamp, performer, pitch, volume, particleColor, instrumentId, soundType, soundIndex);
            }
        }

        return true;
    }
}
