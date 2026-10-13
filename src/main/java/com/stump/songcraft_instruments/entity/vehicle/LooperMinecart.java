package com.stump.songcraft_instruments.entity.vehicle;

import com.stump.songcraft_instruments.block.blockentity.looper.LooperVolumeControls;
import com.stump.songcraft_instruments.entity.ModEntities;
import com.stump.songcraft_instruments.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A minecart carrying a looper. Plays back the inserted record as it goes.
 * Right-click to open the looper, or right-click with a record to insert it.
 */
public class LooperMinecart extends AbstractMinecart implements CarriedBlockVehicle {
    private static final EntityDataAccessor<Byte> DATA_LOOPER_FLAGS = SynchedEntityData.defineId(LooperMinecart.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_LOOPER_VOLUME = SynchedEntityData.defineId(LooperMinecart.class, EntityDataSerializers.INT);

    private final VehicleLooper looper = new VehicleLooper(this);

    public LooperMinecart(final EntityType<?> type, final Level level) {
        super(type, level);
    }
    public LooperMinecart(final Level level, final double x, final double y, final double z) {
        super(ModEntities.LOOPER_MINECART.get(), level, x, y, z);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_LOOPER_FLAGS, (byte) 0);
        entityData.define(DATA_LOOPER_VOLUME, LooperVolumeControls.MAX_VOLUME);
    }

    @Override
    public void onSyncedDataUpdated(final EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        // Called before the looper is created, while the synced data is defined
        if (looper != null && level().isClientSide && DATA_LOOPER_VOLUME.equals(key))
            looper.onVolumeSynced(entityData.get(DATA_LOOPER_VOLUME));
    }

    public VehicleLooper looper() {
        return looper;
    }

    /**
     * Not {@link Type#RIDEABLE}, so it doesn't pick up mobs
     */
    @Override
    public Type getMinecartType() {
        return Type.CHEST;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            looper.tick();
            entityData.set(DATA_LOOPER_FLAGS, looper.getFlags());
            entityData.set(DATA_LOOPER_VOLUME, looper.getVolume());
        }
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        final InteractionResult inserted = looper.insertHeldRecord(player, hand);
        return inserted.consumesAction() ? inserted : looper.openMenu(player);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return VehicleLooper.displayState(entityData.get(DATA_LOOPER_FLAGS));
    }
    @Override
    public BlockState getCarriedBlockState() {
        return getDisplayBlockState();
    }
    @Override
    public Vec3 getNoteParticlePos() {
        return CarriedBlockVehicle.minecartParticlePos(this);
    }

    // Same as the chest minecart
    @Override
    public int getDefaultDisplayOffset() {
        return 8;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.LOOPER_MINECART.get();
    }
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.LOOPER_MINECART.get());
    }

    @Override
    public void remove(final RemovalReason reason) {
        if (!level().isClientSide)
            looper.onRemoved(reason.shouldDestroy());
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        looper.save(tag);
    }
    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        looper.load(tag);
    }
}
