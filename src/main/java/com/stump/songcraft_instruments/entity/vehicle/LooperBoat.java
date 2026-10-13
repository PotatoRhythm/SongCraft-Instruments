package com.stump.songcraft_instruments.entity.vehicle;

import com.stump.songcraft_instruments.entity.ModEntities;
import com.stump.songcraft_instruments.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A boat carrying a looper, in place of a chest boat's chest. Plays back the inserted record as it goes.
 * Right-click to ride, shift + right-click to open the looper, or right-click with a record to insert it.
 */
public class LooperBoat extends Boat implements CarriedBlockVehicle, HasCustomInventoryScreen {
    private static final EntityDataAccessor<Byte> DATA_LOOPER_FLAGS = SynchedEntityData.defineId(LooperBoat.class, EntityDataSerializers.BYTE);

    private final VehicleLooper looper = new VehicleLooper(this);

    public LooperBoat(final EntityType<? extends Boat> type, final Level level) {
        super(type, level);
    }
    public LooperBoat(final Level level, final double x, final double y, final double z) {
        this(ModEntities.LOOPER_BOAT.get(), level);
        setPos(x, y, z);
        xo = x;
        yo = y;
        zo = z;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_LOOPER_FLAGS, (byte) 0);
    }

    // The looper takes the back seat, as a chest does
    @Override
    protected float getSinglePassengerXOffset() {
        return 0.15F;
    }
    @Override
    protected int getMaxPassengers() {
        return 1;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            looper.tick();
            entityData.set(DATA_LOOPER_FLAGS, looper.getFlags());
        }
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        final InteractionResult inserted = looper.insertHeldRecord(player, hand);
        if (inserted.consumesAction())
            return inserted;

        if (canAddPassenger(player) && !player.isSecondaryUseActive())
            return super.interact(player, hand);
        return looper.openMenu(player);
    }
    /**
     * Opened by the inventory key while riding
     */
    @Override
    public void openCustomInventoryScreen(final Player player) {
        looper.openMenu(player);
    }

    @Override
    public BlockState getCarriedBlockState() {
        return VehicleLooper.displayState(entityData.get(DATA_LOOPER_FLAGS));
    }

    @Override
    public Vec3 getNoteParticlePos() {
        return CarriedBlockVehicle.boatParticlePos(this);
    }

    @Override
    public Item getDropItem() {
        return ModItems.LOOPER_BOATS.get(getVariant()).get();
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
