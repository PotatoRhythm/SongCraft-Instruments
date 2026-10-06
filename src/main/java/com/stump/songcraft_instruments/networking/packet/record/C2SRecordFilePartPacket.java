package com.stump.songcraft_instruments.networking.packet.record;

import com.stump.songcraft_instruments.networking.IModPacket;
import com.stump.songcraft_instruments.recording.RecordTransfers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * A part of a record file the player is importing. 0 parts cancels the import.
 */
public class C2SRecordFilePartPacket implements IModPacket {
    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_SERVER;

    private final String fileName;
    private final int part, parts;
    private final byte[] data;

    public C2SRecordFilePartPacket(final String fileName, final int part, final int parts, final byte[] data) {
        this.fileName = fileName;
        this.part = part;
        this.parts = parts;
        this.data = data;
    }
    public C2SRecordFilePartPacket(final FriendlyByteBuf buf) {
        fileName = buf.readUtf(RecordTransfers.MAX_FILE_NAME_LENGTH);
        part = buf.readVarInt();
        parts = buf.readVarInt();
        data = buf.readByteArray(RecordTransfers.PART_SIZE);
    }

    public static C2SRecordFilePartPacket cancel(final String fileName) {
        return new C2SRecordFilePartPacket(fileName, 0, 0, new byte[0]);
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
        final ServerPlayer player = context.getSender();
        if (player != null)
            RecordTransfers.receiveImportPart(player, fileName, part, parts, data);
    }
}
