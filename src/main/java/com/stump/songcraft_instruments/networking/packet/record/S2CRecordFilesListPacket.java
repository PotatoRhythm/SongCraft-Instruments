package com.stump.songcraft_instruments.networking.packet.record;

import com.stump.songcraft_instruments.networking.IModPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Asks the player's client to list their record files, as they ran {@code /screcord files}
 */
public class S2CRecordFilesListPacket implements IModPacket {
    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_CLIENT;

    public S2CRecordFilesListPacket() {}
    public S2CRecordFilesListPacket(final FriendlyByteBuf buf) {}

    @Override
    public void handle(final Context context) {
        RecordFilesClient.listFiles();
    }
}
