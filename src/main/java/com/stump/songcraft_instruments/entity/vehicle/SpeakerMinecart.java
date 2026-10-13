package com.stump.songcraft_instruments.entity.vehicle;

import com.stump.songcraft_instruments.block.blockentity.speaker.Speaker;
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
 * A minecart carrying a speaker. Relays its paired instruments' notes as it goes.
 * Instruments and the speaker cable are used on it like on a speaker block.
 */
public class SpeakerMinecart extends AbstractMinecart implements CarriedBlockVehicle, SpeakerVehicle {
    private static final EntityDataAccessor<Boolean> DATA_CONNECTED = SynchedEntityData.defineId(SpeakerMinecart.class, EntityDataSerializers.BOOLEAN);

    private final VehicleSpeaker speaker = new VehicleSpeaker(this);

    public SpeakerMinecart(final EntityType<?> type, final Level level) {
        super(type, level);
    }
    public SpeakerMinecart(final Level level, final double x, final double y, final double z) {
        super(ModEntities.SPEAKER_MINECART.get(), level, x, y, z);
    }

    @Override
    public Speaker speaker() {
        return speaker.speaker();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_CONNECTED, false);
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
            speaker.tick();
            entityData.set(DATA_CONNECTED, speaker.isConnected());
        }
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        return speaker.interact(player, hand);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return VehicleSpeaker.displayState(entityData.get(DATA_CONNECTED));
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
        return ModItems.SPEAKER_MINECART.get();
    }
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.SPEAKER_MINECART.get());
    }

    @Override
    public void remove(final RemovalReason reason) {
        if (!level().isClientSide)
            speaker.onRemoved(reason.shouldDestroy());
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        speaker.save(tag);
    }
    @Override
    protected void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        speaker.load(tag);
    }
}
