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
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A boat carrying a speaker, in place of a chest boat's chest. Relays its paired instruments' notes as it goes.
 * Right-click to ride. Instruments and the speaker cable are used on it like on a speaker block.
 */
public class SpeakerBoat extends Boat implements CarriedBlockVehicle, SpeakerVehicle {
    private static final EntityDataAccessor<Boolean> DATA_CONNECTED = SynchedEntityData.defineId(SpeakerBoat.class, EntityDataSerializers.BOOLEAN);

    private final VehicleSpeaker speaker = new VehicleSpeaker(this);

    public SpeakerBoat(final EntityType<? extends Boat> type, final Level level) {
        super(type, level);
    }
    public SpeakerBoat(final Level level, final double x, final double y, final double z) {
        this(ModEntities.SPEAKER_BOAT.get(), level);
        setPos(x, y, z);
        xo = x;
        yo = y;
        zo = z;
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

    // The speaker takes the back seat, as a chest does
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
            speaker.tick();
            entityData.set(DATA_CONNECTED, speaker.isConnected());
        }
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        final InteractionResult used = speaker.interact(player, hand);
        return (used != InteractionResult.PASS) ? used : super.interact(player, hand);
    }

    @Override
    public BlockState getCarriedBlockState() {
        return VehicleSpeaker.displayState(entityData.get(DATA_CONNECTED));
    }

    @Override
    public Vec3 getNoteParticlePos() {
        return CarriedBlockVehicle.boatParticlePos(this);
    }

    @Override
    public Item getDropItem() {
        return ModItems.SPEAKER_BOATS.get(getVariant()).get();
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
