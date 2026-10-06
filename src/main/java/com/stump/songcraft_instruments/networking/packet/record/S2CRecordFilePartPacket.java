package com.stump.songcraft_instruments.networking.packet.record;

import com.stump.songcraft_instruments.networking.IModPacket;
import com.stump.songcraft_instruments.recording.RecordTransfers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * A part of a record file the player exported, to be saved on their computer once every part arrived
 */
public class S2CRecordFilePartPacket implements IModPacket {
    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_CLIENT;

    private final String fileName;
    private final int part, parts;
    private final byte[] data;

    public S2CRecordFilePartPacket(final String fileName, final int part, final int parts, final byte[] data) {
        this.fileName = fileName;
        this.part = part;
        this.parts = parts;
        this.data = data;
    }
    public S2CRecordFilePartPacket(final FriendlyByteBuf buf) {
        fileName = buf.readUtf(RecordTransfers.MAX_FILE_NAME_LENGTH);
        part = buf.readVarInt();
        parts = buf.readVarInt();
        data = buf.readByteArray(RecordTransfers.PART_SIZE);
    }

    @Override
    public void write(final FriendlyByteBuf buf) {
        buf.writeUtf(fileName, RecordTransfers.MAX_FILE_NAME_LENGTH);
        buf.writeVarInt(part);
        buf.writeVarInt(parts);
        buf.writeByteArray(data);
    }

    @Override
    public void handle(final Context context) {
        RecordFilesClient.receiveExportPart(fileName, part, parts, data);
    }
}
