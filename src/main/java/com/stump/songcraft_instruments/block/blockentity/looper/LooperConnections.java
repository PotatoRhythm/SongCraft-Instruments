package com.stump.songcraft_instruments.block.blockentity.looper;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.ModBlockEntities;
import com.stump.songcraft_instruments.block.util.LooperConnection;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.capability.recording.RecordingCapabilityProvider;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.LooperConnectionsPacket;
import com.stump.songcraft_instruments.util.LooperUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The players connected to a looper through an instrument, and syncing them to their clients.
 */
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public class LooperConnections {
    public static final String CONNECTIONS_TAG = "Connections";

    private final LooperBlockEntity looper;

    /**
     * Players connected to this looper through an instrument, mapped by their UUID.
     * Each player may only have a single connection at a time.
     */
    private final LinkedHashMap<UUID, LooperConnection> connections = new LinkedHashMap<>();

    public LooperConnections(final LooperBlockEntity looper) {
        this.looper = looper;
    }


    public void load() {
        connections.clear();
        looper.getPersistentData().getList(CONNECTIONS_TAG, Tag.TAG_COMPOUND).forEach((tag) -> {
            final LooperConnection connection = LooperConnection.load((CompoundTag) tag);
            connections.put(connection.playerId(), connection);
        });
    }
    private void save() {
        final ListTag list = new ListTag();
        connections.values().forEach((connection) -> list.add(connection.save()));
        looper.getPersistentData().put(CONNECTIONS_TAG, list);
        looper.setChanged();
    }

    /**
     * Connects the player to this looper, replacing their previous connection to it (if any).
     */
    public void add(final Player player, final UUID connectionId) {
        connections.put(player.getUUID(), new LooperConnection(player.getUUID(), player.getGameProfile().getName(), connectionId));
        save();
        sync();
    }
    /**
     * Removes the player's connection, only if it matches the provided {@code connectionId}.
     */
    public void remove(final UUID playerId, final UUID connectionId) {
        final LooperConnection connection = connections.get(playerId);
        if (connection == null || !connection.connectionId().equals(connectionId))
            return;

        connections.remove(playerId);
        save();
        sync(null, List.of(playerId));

        // They may have been the last participant present
        looper.session().onParticipantLeft(playerId);
    }
    /**
     * Removes the connection with the provided {@code connectionId}, whichever player it belongs to.
     */
    public void removeByConnectionId(final UUID connectionId) {
        connections.values().stream()
            .filter((connection) -> connection.connectionId().equals(connectionId))
            .findFirst()
            .ifPresent((connection) -> remove(connection.playerId(), connectionId));
    }
    public void clear() {
        if (connections.isEmpty())
            return;

        final List<UUID> formerPlayers = List.copyOf(connections.keySet());
        connections.clear();
        save();
        sync(null, formerPlayers);
    }

    /**
     * @return Whether any player has an instrument connected with the provided {@code connectionId}
     */
    public boolean has(@Nullable final UUID connectionId) {
        return connectionId != null && connections.values().stream()
            .anyMatch((connection) -> connection.connectionId().equals(connectionId));
    }
    /**
     * @return Whether the provided {@code connectionId} is the player's current connection to this looper
     */
    public boolean isConnectedBy(final Player player, @Nullable final UUID connectionId) {
        final LooperConnection connection = connections.get(player.getUUID());
        return connection != null && connection.connectionId().equals(connectionId);
    }

    /**
     * @return All players currently connected to this looper, in connection order
     */
    public Collection<LooperConnection> getAll() {
        pruneStale();
        return Collections.unmodifiableCollection(connections.values());
    }
    /**
     * @return The UUIDs of all connected players, as currently stored (without pruning stale connections)
     */
    Set<UUID> getPlayerIds() {
        return connections.keySet();
    }

    /**
     * Removes connections of online players who have since connected elsewhere.
     * Occurs when they connected to another looper while this one was unloaded.
     * Offline players are kept, as they may come back.
     */
    private void pruneStale() {
        final Level level = looper.getLevel();
        if (level == null || level.isClientSide || level.getServer() == null)
            return;

        final boolean pruned = connections.values().removeIf((connection) -> {
            final ServerPlayer player = level.getServer().getPlayerList().getPlayer(connection.playerId());
            return player != null && !connection.connectionId().equals(RecordingCapabilityProvider.getConnectionId(player));
        });

        if (pruned)
            save();
    }

    /**
     * Disconnects every player who is not currently on their connected instrument's screen,
     * as they are not to participate in a group recording that is about to start.
     */
    public void disconnectAbsentPlayers() {
        final PlayerList playerList = looper.getLevel().getServer().getPlayerList();
        final List<UUID> absent = connections.keySet().stream()
            .filter((playerId) -> {
                final ServerPlayer player = playerList.getPlayer(playerId);
                return player == null || !isOnConnectedInstrument(player);
            })
            .toList();

        if (absent.isEmpty())
            return;

        absent.forEach(connections::remove);
        save();
        sync(null, absent);
    }

    private boolean isOnConnectedInstrument(final Player player) {
        if (!InstrumentOpenProvider.isOpen(player))
            return false;

        final CompoundTag looperTag;
        if (InstrumentOpenProvider.isItem(player)) {
            looperTag = LooperUtil.looperTag(player.getItemInHand(InstrumentOpenProvider.getHand(player)));
        } else {
            final BlockEntity instrumentBE = player.level().getBlockEntity(InstrumentOpenProvider.getBlockPos(player));
            if (instrumentBE == null)
                return false;

            looperTag = LooperUtil.looperTag(instrumentBE);
        }

        return LooperUtil.isConnectedBy(looper, looperTag, player);
    }


    //#region Syncing

    /**
     * Sends the connected players and session state to all online connected players.
     */
    public void sync() {
        sync(null, List.of());
    }
    /**
     * @param leavingPlayer A player in the process of logging out, to be treated as offline
     * @param formerPlayers Players that were just disconnected, to be notified as well
     */
    void sync(@Nullable final UUID leavingPlayer, final Collection<UUID> formerPlayers) {
        final Level level = looper.getLevel();
        if (level == null || level.isClientSide || level.getServer() == null)
            return;

        final PlayerList playerList = level.getServer().getPlayerList();
        final Predicate<UUID> isOnline = (playerId) ->
            !playerId.equals(leavingPlayer) && playerList.getPlayer(playerId) != null;

        final LooperConnectionsPacket packet = createPacket(isOnline);

        Stream.concat(connections.keySet().stream(), formerPlayers.stream())
            .filter(isOnline)
            .forEach((playerId) -> SCPacketHandler.sendToClient(packet, playerList.getPlayer(playerId)));
    }
    public void syncTo(final ServerPlayer player) {
        final PlayerList playerList = looper.getLevel().getServer().getPlayerList();
        SCPacketHandler.sendToClient(createPacket((playerId) -> playerList.getPlayer(playerId) != null), player);
    }

    private LooperConnectionsPacket createPacket(final Predicate<UUID> isOnline) {
        final PlayerList playerList = looper.getLevel().getServer().getPlayerList();

        final List<LooperConnectionsPacket.Entry> entries = getAll().stream()
            .map((connection) -> {
                final boolean online = isOnline.test(connection.playerId());
                final ServerPlayer player = online ? playerList.getPlayer(connection.playerId()) : null;

                return new LooperConnectionsPacket.Entry(
                    connection.playerId(), connection.playerName(), online,
                    player != null && isOnConnectedInstrument(player)
                );
            })
            .toList();

        final RecordingSession session = looper.session();
        return new LooperConnectionsPacket(looper.getBlockPos(), entries, session.getState(), session.isGroupSession());
    }

    //#endregion


    //#region Player events & lookups

    // If the player leaves the world, we shouldn't record anymore
    @SubscribeEvent
    public static void onPlayerLeave(final PlayerLoggedOutEvent event) {
        final Player player = event.getEntity();

        if (RecordingCapabilityProvider.isRecording(player)) {
            player.level()
                .getBlockEntity(RecordingCapabilityProvider.getLooperPos(player), ModBlockEntities.LOOPER.get())
                .filter((lbe) -> lbe.session().isLockedBy(player))
                .ifPresent((lbe) -> {
                    lbe.reset();
                    lbe.getPersistentData().putBoolean(RecordingSession.RECORDING_TAG, false);
                });

            LooperUtil.setNotRecording(player);
        }

        // Show the player as offline to the rest of their looper's players
        getConnectedLooper(player).ifPresent((lbe) -> {
            lbe.connections().sync(player.getUUID(), List.of());
            lbe.session().onParticipantLeft(player.getUUID());
        });
    }

    // Show the player as online again to the rest of their looper's players
    @SubscribeEvent
    public static void onPlayerJoin(final PlayerLoggedInEvent event) {
        getConnectedLooper(event.getEntity()).ifPresent((lbe) -> lbe.connections().sync());
    }

    /**
     * During a group session, participants are recorded on any instrument they play,
     * rather than only the one they connected with.
     * @return The loaded looper whose group session the player is participating in
     */
    public static Optional<LooperBlockEntity> getGroupSessionLooper(final Player player) {
        return getConnectedLooper(player).filter((lbe) -> lbe.session().isGroupSession());
    }

    /**
     * @return The loaded looper the player is currently connected to
     */
    public static Optional<LooperBlockEntity> getConnectedLooper(final Player player) {
        final BlockPos looperPos = RecordingCapabilityProvider.getConnectedLooperPos(player);
        if (looperPos == null || !player.level().isLoaded(looperPos))
            return Optional.empty();

        return player.level().getBlockEntity(looperPos, ModBlockEntities.LOOPER.get())
            .filter((lbe) -> lbe.connections().isConnectedBy(player, RecordingCapabilityProvider.getConnectionId(player)));
    }

    //#endregion
}
