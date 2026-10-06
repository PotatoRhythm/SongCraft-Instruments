package com.stump.songcraft_instruments.networking.packet.record;

import com.stump.songcraft_instruments.networking.IModPacket;
import com.stump.songcraft_instruments.recording.RecordTransfers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Asks the player's client to upload one of their record files, as they ran {@code /screcord import}
 */
public class S2CRecordImportRequestPacket implements IModPacket {
    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_CLIENT;

    private final String fileName;
    private final int maxBytes;

    public S2CRecordImportRequestPacket(final String fileName, final int maxBytes) {
        this.fileName = fileName;
        this.maxBytes = maxBytes;
    }
    public S2CRecordImportRequestPacket(final FriendlyByteBuf buf) {
        fileName = buf.readUtf(RecordTransfers.MAX_FILE_NAME_LENGTH);
        maxBytes = buf.readVarInt();
    }

    @Override
    public void write(final FriendlyByteBuf buf) {
        buf.writeUtf(fileName, RecordTransfers.MAX_FILE_NAME_LENGTH);
        buf.writeVarInt(maxBytes);
    }

    @Override
    public void handle(final Context context) {
        RecordFilesClient.uploadFile(fileName, maxBytes);
    }
}
