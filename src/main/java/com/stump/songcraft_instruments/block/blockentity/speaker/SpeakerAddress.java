package com.stump.songcraft_instruments.block.blockentity.speaker;

import com.stump.songcraft_instruments.block.blockentity.SpeakerBlockEntity;
import com.stump.songcraft_instruments.entity.vehicle.SpeakerVehicle;
import com.stump.songcraft_instruments.entity.vehicle.SpeakerVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * where an instrument finds a speaker it is paired to: a speaker block's position,
 * or the boat or minecart carrying the speaker.
 */
public sealed interface SpeakerAddress {
    String POS_TAG = "pos", ENTITY_TAG = "entity";

    /**
     * return the loaded speaker at this address, or null if there is none (or it isn't loaded)
     */
    @Nullable Speaker resolve(Level level);
    /**
     * return whether whatever is at this address can be checked right now
     */
    boolean isLoaded(Level level);
    /**
     * only asked when this address isn't loaded.
     * return whether the given speaker may still be there once it is
     */
    boolean mayStillExist(Level level, UUID speakerId);

    void write(CompoundTag tag);

    /**
     * return the address written to the tag, or null if there is none
     */
    @Nullable
    static SpeakerAddress read(final CompoundTag tag) {
        if (tag.hasUUID(ENTITY_TAG))
            return new OnEntity(tag.getUUID(ENTITY_TAG));
        if (tag.contains(POS_TAG, CompoundTag.TAG_COMPOUND))
            return new AtBlock(NbtUtils.readBlockPos(tag.getCompound(POS_TAG)));
        return null;
    }


    record AtBlock(BlockPos pos) implements SpeakerAddress {
        @Override
        public @Nullable Speaker resolve(final Level level) {
            return (level.isLoaded(pos) && (level.getBlockEntity(pos) instanceof SpeakerBlockEntity sbe)) ? sbe.speaker() : null;
        }
        @Override
        public boolean isLoaded(final Level level) {
            return level.isLoaded(pos);
        }
        @Override
        public boolean mayStillExist(final Level level, final UUID speakerId) {
            return true;
        }
        @Override
        public void write(final CompoundTag tag) {
            tag.put(POS_TAG, NbtUtils.writeBlockPos(pos));
        }
    }

    /**
     * @param entityId The UUID of the boat or minecart carrying the speaker
     */
    record OnEntity(UUID entityId) implements SpeakerAddress {
        @Override
        public @Nullable Speaker resolve(final Level level) {
            return ((level instanceof ServerLevel serverLevel) && (serverLevel.getEntity(entityId) instanceof SpeakerVehicle vehicle))
                ? vehicle.speaker() : null;
        }
        @Override
        public boolean isLoaded(final Level level) {
            return resolve(level) != null;
        }
        /**
         * a vehicle that isn't loaded may have been destroyed, which only the list of existing vehicles can tell
         */
        @Override
        public boolean mayStillExist(final Level level, final UUID speakerId) {
            return (level.getServer() == null) || SpeakerVehicles.get(level.getServer()).contains(speakerId);
        }
        @Override
        public void write(final CompoundTag tag) {
            tag.putUUID(ENTITY_TAG, entityId);
        }
    }
}
