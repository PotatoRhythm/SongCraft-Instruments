package com.stump.songcraft_instruments.item.emirecord;

import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class EMIRecordItem extends Item {
    public static final String
        CHANNEL_TAG = "Channel",

        INSTRUMENT_ID_TAG = "InstrumentId",
        NOTES_TAG = "Notes",
        NOTE_TYPE = "NoteType",

        SOUND_INDEX_TAG = "SoundIndex",
        SOUND_TYPE_TAG = "SoundType",
        PITCH_TAG = "Pitch",
        VOLUME_TAG = "Volume",
        TIMESTAMP_TAG = "Timestamp",
        REPEAT_TICK_TAG = "RepeatTick",

        // Held-exclusive
        HELD_PHASE = "HeldPhase",

        WRITABLE_TAG = "Writable"
    ;


    public EMIRecordItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    public abstract void onInsert(final ItemStack stack, final LooperBlockEntity lbe);

    /**
     * Packs the notes of records written before {@link RecordNotes} existed, as they may be too big to send to clients
     */
    @Override
    public void verifyTagAfterLoad(final CompoundTag tag) {
        if (tag.contains(CHANNEL_TAG, Tag.TAG_COMPOUND))
            RecordNotes.pack(tag.getCompound(CHANNEL_TAG));
    }
}
