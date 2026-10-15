package com.stump.songcraft_instruments.block.blockentity.looper;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.config.ModServerConfigs;
import com.stump.songcraft_instruments.entity.vehicle.LooperBoat;
import com.stump.songcraft_instruments.entity.vehicle.LooperMinecart;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The player who placed each looper, block or boat/minecart, so each player can be held to
 * {@link ModServerConfigs#LOOPER_MAX_PER_PLAYER}.
 * Kept here rather than on the loopers so that loopers in unloaded chunks still count.
 */
public class LooperOwners extends SavedData {
    private static final String NAME = SCInstrumentMod.MODID + "_looper_owners",
        BLOCKS_TAG = "blocks", VEHICLES_TAG = "vehicles",
        DIMENSION_TAG = "dimension", POS_TAG = "pos", VEHICLE_TAG = "vehicle", OWNER_TAG = "owner";

    private final Map<GlobalPos, UUID> blockOwners = new HashMap<>();
    /** Owners by the vehicle's entity UUID, which it keeps across dimensions */
    private final Map<UUID, UUID> vehicleOwners = new HashMap<>();

    public static LooperOwners get(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(LooperOwners::load, LooperOwners::new, NAME);
    }

    public static boolean isLooperVehicle(final Entity entity) {
        return entity instanceof LooperBoat || entity instanceof LooperMinecart;
    }

    /**
     * Server only. Checks whether the player may place another looper, telling them if not. Operators have no limit.
     * @return True if the player has reached their limit, and the looper should not be placed
     */
    public static boolean refusePlacement(final Player player) {
        final int max = ModServerConfigs.LOOPER_MAX_PER_PLAYER.get();
        if (max < 0 || !(player instanceof ServerPlayer serverPlayer) || player.hasPermissions(2))
            return false;
        if (get(serverPlayer.server).countOwnedBy(player.getUUID()) < max)
            return false;

        player.displayClientMessage(
            Component.translatable("songcraft_instruments.looper.limit_reached").withStyle(ChatFormatting.RED),
            true
        );
        // The client may have already taken the looper from its stack, expecting it to be placed
        serverPlayer.containerMenu.sendAllDataToRemote();
        return true;
    }

    /**
     * @return How many loopers the given player has placed that still exist
     */
    public int countOwnedBy(final UUID owner) {
        return (int) (
            blockOwners.values().stream().filter(owner::equals).count()
            + vehicleOwners.values().stream().filter(owner::equals).count()
        );
    }

    public void addBlock(final GlobalPos pos, final UUID owner) {
        if (!owner.equals(blockOwners.put(pos, owner)))
            setDirty();
    }
    public void removeBlock(final GlobalPos pos) {
        if (blockOwners.remove(pos) != null)
            setDirty();
    }

    public void addVehicle(final UUID vehicleId, final UUID owner) {
        if (!owner.equals(vehicleOwners.put(vehicleId, owner)))
            setDirty();
    }
    public void removeVehicle(final UUID vehicleId) {
        if (vehicleOwners.remove(vehicleId) != null)
            setDirty();
    }

    private static LooperOwners load(final CompoundTag tag) {
        final LooperOwners loopers = new LooperOwners();

        tag.getList(BLOCKS_TAG, Tag.TAG_COMPOUND).forEach((entry) -> {
            final CompoundTag looperTag = (CompoundTag) entry;
            final GlobalPos pos = GlobalPos.of(
                ResourceKey.create(Registries.DIMENSION, new ResourceLocation(looperTag.getString(DIMENSION_TAG))),
                BlockPos.of(looperTag.getLong(POS_TAG))
            );
            loopers.blockOwners.put(pos, looperTag.getUUID(OWNER_TAG));
        });
        tag.getList(VEHICLES_TAG, Tag.TAG_COMPOUND).forEach((entry) -> {
            final CompoundTag looperTag = (CompoundTag) entry;
            loopers.vehicleOwners.put(looperTag.getUUID(VEHICLE_TAG), looperTag.getUUID(OWNER_TAG));
        });

        return loopers;
    }
    @Override
    public CompoundTag save(final CompoundTag tag) {
        final ListTag blocks = new ListTag();
        blockOwners.forEach((pos, owner) -> {
            final CompoundTag looperTag = new CompoundTag();
            looperTag.putString(DIMENSION_TAG, pos.dimension().location().toString());
            looperTag.putLong(POS_TAG, pos.pos().asLong());
            looperTag.putUUID(OWNER_TAG, owner);
            blocks.add(looperTag);
        });
        tag.put(BLOCKS_TAG, blocks);

        final ListTag vehicles = new ListTag();
        vehicleOwners.forEach((vehicleId, owner) -> {
            final CompoundTag looperTag = new CompoundTag();
            looperTag.putUUID(VEHICLE_TAG, vehicleId);
            looperTag.putUUID(OWNER_TAG, owner);
            vehicles.add(looperTag);
        });
        tag.put(VEHICLES_TAG, vehicles);

        return tag;
    }
}
