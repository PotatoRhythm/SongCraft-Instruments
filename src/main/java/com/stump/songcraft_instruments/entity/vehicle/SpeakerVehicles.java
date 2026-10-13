package com.stump.songcraft_instruments.entity.vehicle;

import com.stump.songcraft_instruments.SCInstrumentMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The speakers carried by boats and minecarts that still exist, by speaker ID.
 * Unlike a speaker block's position, an unloaded vehicle can't be looked at to tell whether it was destroyed,
 * so instruments check here before keeping a pairing to a vehicle they can't find.
 */
public class SpeakerVehicles extends SavedData {
    private static final String NAME = SCInstrumentMod.MODID + "_speaker_vehicles", SPEAKERS_TAG = "speakers";

    private final Set<UUID> speakerIds = new HashSet<>();

    public static SpeakerVehicles get(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(SpeakerVehicles::load, SpeakerVehicles::new, NAME);
    }

    public boolean contains(final UUID speakerId) {
        return speakerIds.contains(speakerId);
    }
    public void add(final UUID speakerId) {
        if (speakerIds.add(speakerId))
            setDirty();
    }
    public void remove(final UUID speakerId) {
        if (speakerIds.remove(speakerId))
            setDirty();
    }

    private static SpeakerVehicles load(final CompoundTag tag) {
        final SpeakerVehicles vehicles = new SpeakerVehicles();
        tag.getList(SPEAKERS_TAG, Tag.TAG_INT_ARRAY).forEach((idTag) -> vehicles.speakerIds.add(NbtUtils.loadUUID(idTag)));
        return vehicles;
    }
    @Override
    public CompoundTag save(final CompoundTag tag) {
        final ListTag list = new ListTag();
        speakerIds.forEach((speakerId) -> list.add(NbtUtils.createUUID(speakerId)));
        tag.put(SPEAKERS_TAG, list);
        return tag;
    }
}
