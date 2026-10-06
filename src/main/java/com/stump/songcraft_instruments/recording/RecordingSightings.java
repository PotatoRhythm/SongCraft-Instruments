package com.stump.songcraft_instruments.recording;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.item.record.WritableRecordItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Notes when records are seen, so that {@code /screcord cleanup} only deletes recordings nobody has come across in a while.
 * <p>
 * A record is seen when it is in a loaded looper, in an online player's inventory or ender chest (including inside
 * shulker boxes and backpacks), in a block entity of a chunk loaded from disk, or held by an entity as it is loaded.
 * Records left untouched in chunks nobody visits are not seen, nor are those of players who stay offline
 * (though cleanup checks offline players' saved inventories itself).
 */
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public final class RecordingSightings {
    private static final int PLAYER_SCAN_INTERVAL = 5 * 60 * 20;
    private static final int MAX_DEPTH = 64;

    /**
     * Recordings seen off the server thread, as chunks may be read elsewhere
     */
    private static final Queue<String> PENDING = new ConcurrentLinkedQueue<>();
    private static int ticks = 0;

    private RecordingSightings() {}


    /**
     * Passes the recording ID of every record in the tag, however deeply nested
     */
    public static void findRecordings(final Tag tag, final Consumer<String> found) {
        findRecordings(tag, found, 0);
    }
    private static void findRecordings(final Tag tag, final Consumer<String> found, final int depth) {
        if (depth > MAX_DEPTH)
            return;

        if (tag instanceof CompoundTag compound) {
            for (final String key : compound.getAllKeys()) {
                final Tag child = compound.get(key);

                if (key.equals(WritableRecordItem.RECORDING_ID_TAG) && child.getId() == Tag.TAG_STRING) {
                    final String id = child.getAsString();
                    if (RecordingCodec.isValidId(id))
                        found.accept(id);
                } else if (child.getId() == Tag.TAG_COMPOUND || child.getId() == Tag.TAG_LIST) {
                    findRecordings(child, found, depth + 1);
                }
            }
        } else if (tag instanceof ListTag list
                && (list.getElementType() == Tag.TAG_COMPOUND || list.getElementType() == Tag.TAG_LIST)) {
            for (final Tag child : list)
                findRecordings(child, found, depth + 1);
        }
    }

    public static void findRecordings(final ItemStack stack, final Consumer<String> found) {
        if (stack.hasTag())
            findRecordings(stack.getTag(), found);
    }

    public static void scanPlayer(final ServerPlayer player, final Consumer<String> found) {
        findRecordings(player.saveWithoutId(new CompoundTag()), found);
    }


    @SubscribeEvent
    public static void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            scanPlayer(player, RecordingStore.get(player.server)::markSeen);
    }
    @SubscribeEvent
    public static void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            scanPlayer(player, RecordingStore.get(player.server)::markSeen);
    }

    @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        final MinecraftServer server = event.getServer();
        final RecordingStore store = RecordingStore.get(server);

        String id;
        while ((id = PENDING.poll()) != null)
            store.markSeen(id);

        if (++ticks % PLAYER_SCAN_INTERVAL == 0) {
            for (final ServerPlayer player : server.getPlayerList().getPlayers())
                scanPlayer(player, store::markSeen);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(final ChunkDataEvent.Load event) {
        if (event.getLevel() == null || event.getLevel().isClientSide())
            return;

        findRecordings(event.getData().getList("block_entities", Tag.TAG_COMPOUND), PENDING::add);
    }

    @SubscribeEvent
    public static void onEntityJoin(final EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide())
            return;

        final Entity entity = event.getEntity();
        if (entity instanceof ItemEntity item) {
            findRecordings(item.getItem(), PENDING::add);
        } else if (entity instanceof ItemFrame frame) {
            findRecordings(frame.getItem(), PENDING::add);
        } else if (entity instanceof ContainerEntity container) {
            container.getItemStacks().forEach((stack) -> findRecordings(stack, PENDING::add));
        } else if (entity instanceof LivingEntity living && !(entity instanceof ServerPlayer)) {
            living.getAllSlots().forEach((stack) -> findRecordings(stack, PENDING::add));
        }
    }
}
