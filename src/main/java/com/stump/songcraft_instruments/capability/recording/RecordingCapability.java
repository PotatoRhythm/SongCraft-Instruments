package com.stump.songcraft_instruments.capability.recording;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

import java.util.UUID;

@AutoRegisterCapability
public class RecordingCapability {
    public static final String
        CONNECTED_POS_TAG = "ConnectedLooperPos",
        CONNECTION_ID_TAG = "ConnectionId"
    ;

    // The player's single instrument connection to a looper
    private BlockPos connectedLooperPos = null;
    private UUID connectionId = null;

    // The colors of the player's active particle color set, as sent by their client. Not saved.
    private int[] particleColors = new int[0];
    // The player's instrument volume in percent, as sent by their client, or -1 if unknown. Not saved.
    private int volume = -1;

    public void setConnection(final BlockPos connectedLooperPos, final UUID connectionId) {
        this.connectedLooperPos = connectedLooperPos;
        this.connectionId = connectionId;
    }
    public BlockPos getConnectedLooperPos() {
        return connectedLooperPos;
    }
    public UUID getConnectionId() {
        return connectionId;
    }

    public int[] getParticleColors() {
        return particleColors;
    }
    public void setParticleColors(final int[] particleColors) {
        this.particleColors = particleColors;
    }

    public int getVolume() {
        return volume;
    }
    public void setVolume(final int volume) {
        this.volume = volume;
    }

    public void copyFrom(final RecordingCapability other) {
        connectedLooperPos = other.connectedLooperPos;
        connectionId = other.connectionId;
        particleColors = other.particleColors;
        volume = other.volume;
    }

    public void saveNBTData(final CompoundTag nbt) {
        if (connectedLooperPos != null && connectionId != null) {
            nbt.put(CONNECTED_POS_TAG, NbtUtils.writeBlockPos(connectedLooperPos));
            nbt.putUUID(CONNECTION_ID_TAG, connectionId);
        }
    }

    public void loadNBTData(final CompoundTag nbt) {
        if (nbt.contains(CONNECTED_POS_TAG) && nbt.hasUUID(CONNECTION_ID_TAG)) {
            connectedLooperPos = NbtUtils.readBlockPos(nbt.getCompound(CONNECTED_POS_TAG));
            connectionId = nbt.getUUID(CONNECTION_ID_TAG);
        }
    }
}
