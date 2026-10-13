package com.stump.songcraft_instruments.networking.packet;

import com.stump.songcraft_instruments.block.util.LooperSessionState;
import com.stump.songcraft_instruments.client.gui.instrument.LooperOverlayInjector;
import com.stump.songcraft_instruments.networking.IModPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Syncs the players connected to a looper, along with its session state,
 * to a connected player.
 */
public class LooperConnectionsPacket implements IModPacket {
    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_CLIENT;

    /**
     * @param present Whether the player is on their connected instrument's screen.
     *                Before a group recording starts, only present players take part in it.
     * @param volume The player's instrument volume in percent, or -1 if unknown
     */
    public record Entry(UUID playerId, String playerName, boolean online, boolean present, int volume) {}

    private final BlockPos looperPos;
    private final List<Entry> connections;
    private final LooperSessionState state;
    // Whether a recording session is in progress
    private final boolean groupSession;
    // The name of the synced looper's record that is (or would be) recorded over, if any
    private final @Nullable Component backingName;

    public LooperConnectionsPacket(final BlockPos looperPos, final List<Entry> connections,
                                   final LooperSessionState state, final boolean groupSession,
                                   final @Nullable Component backingName) {
        this.looperPos = looperPos;
        this.connections = connections;
        this.state = state;
        this.groupSession = groupSession;
        this.backingName = backingName;
    }
    public LooperConnectionsPacket(final FriendlyByteBuf buf) {
        looperPos = buf.readBlockPos();
        connections = buf.readList((fbb) -> new Entry(fbb.readUUID(), fbb.readUtf(), fbb.readBoolean(), fbb.readBoolean(), fbb.readVarInt()));
        state = buf.readEnum(LooperSessionState.class);
        groupSession = buf.readBoolean();
        backingName = buf.readNullable(FriendlyByteBuf::readComponent);
    }

    @Override
    public void write(final FriendlyByteBuf buf) {
        buf.writeBlockPos(looperPos);
        buf.writeCollection(connections, (fbb, entry) -> {
            fbb.writeUUID(entry.playerId());
            fbb.writeUtf(entry.playerName());
            fbb.writeBoolean(entry.online());
            fbb.writeBoolean(entry.present());
            fbb.writeVarInt(entry.volume());
        });
        buf.writeEnum(state);
        buf.writeBoolean(groupSession);
        buf.writeNullable(backingName, FriendlyByteBuf::writeComponent);
    }

    @Override
    public void handle(final Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            LooperOverlayInjector.handleConnectionsSync(looperPos, connections, state, groupSession, backingName)
        );
    }
}
