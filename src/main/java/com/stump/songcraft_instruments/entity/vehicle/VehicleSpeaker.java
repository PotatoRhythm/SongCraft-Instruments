package com.stump.songcraft_instruments.entity.vehicle;

import com.stump.songcraft_instruments.block.ModBlocks;
import com.stump.songcraft_instruments.block.SpeakerBlock;
import com.stump.songcraft_instruments.block.blockentity.speaker.Speaker;
import com.stump.songcraft_instruments.block.blockentity.speaker.SpeakerAddress;
import com.stump.songcraft_instruments.item.InstrumentItem;
import com.stump.songcraft_instruments.item.SpeakerCableItem;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import com.stump.songcraft_instruments.util.SpeakerUtil;
import com.stump.songcraft_instruments.networking.packet.instrument.s2c.S2CLooperParticlePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A speaker carried by a boat or minecart. Instruments find it by the vehicle's UUID,
 * and it relays their notes from wherever the vehicle currently is.
 */
public class VehicleSpeaker implements Speaker.Host {
    private static final String SPEAKER_TAG = "Speaker";

    private final Entity vehicle;
    private final Speaker speaker;
    private boolean connected = false;
    // Whether this speaker was added to the existing vehicle speakers since it was loaded
    private boolean registered = false;

    public VehicleSpeaker(final Entity vehicle) {
        this.vehicle = vehicle;
        this.speaker = new Speaker(this, new InitiatorID("vehicle", String.valueOf(vehicle.getId())));
    }

    public Speaker speaker() {
        return speaker;
    }

    /**
     * @return How the carried speaker looks, by whether it is synced as connected
     */
    public static BlockState displayState(final boolean connected) {
        return ModBlocks.SPEAKER.get().defaultBlockState().setValue(SpeakerBlock.CONNECTED, connected);
    }

    /**
     * Pairs or unpairs the held instrument, or uses the held speaker cable on this speaker.
     * @return {@link InteractionResult#PASS} if the player is holding neither
     */
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        final ItemStack heldStack = player.getItemInHand(hand);
        final boolean isInstrument = heldStack.getItem() instanceof InstrumentItem;
        if (!isInstrument && !(heldStack.getItem() instanceof SpeakerCableItem))
            return InteractionResult.PASS;

        // Consume on the client too, so the instrument's screen doesn't open
        final Level level = vehicle.level();
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        if (isInstrument) {
            SpeakerUtil.useHeldInstrument(level, player, heldStack, speaker);
            return InteractionResult.SUCCESS;
        }
        return SpeakerCableItem.useOnSpeaker(heldStack, player, speaker);
    }

    /**
     * Server only
     */
    public void tick() {
        final Level level = vehicle.level();
        if (!registered && level.getServer() != null) {
            SpeakerVehicles.get(level.getServer()).add(speaker.getSpeakerId());
            registered = true;
        }

        speaker.tick(level);
    }

    /**
     * @param destroyed Whether the vehicle was destroyed, after which instruments drop their pairings to this speaker
     */
    public void onRemoved(final boolean destroyed) {
        speaker.releaseSustainedNotes();

        final Level level = vehicle.level();
        if (destroyed && !level.isClientSide && level.getServer() != null)
            SpeakerVehicles.get(level.getServer()).remove(speaker.getSpeakerId());
    }


    //#region Speaker.Host implementation

    @Override
    public Level getHostLevel() {
        return vehicle.level();
    }
    /**
     * The particle is placed by each client, on top of the carried block
     */
    @Override
    public S2CLooperParticlePacket createParticlePacket(final int rgb, final double size) {
        return new S2CLooperParticlePacket(vehicle, rgb, size);
    }
    @Override
    public BlockPos getSoundPos() {
        return VehicleLooper.soundPos(vehicle);
    }
    @Override
    public SpeakerAddress getAddress() {
        return new SpeakerAddress.OnEntity(vehicle.getUUID());
    }
    // Saved along with the vehicle
    @Override
    public void markHostChanged() {}

    @Override
    public boolean isConnected() {
        return connected;
    }
    @Override
    public void setConnected(final boolean connected) {
        this.connected = connected;
    }

    //#endregion


    public void save(final CompoundTag tag) {
        final CompoundTag speakerTag = new CompoundTag();
        speaker.save(speakerTag);
        tag.put(SPEAKER_TAG, speakerTag);
    }
    public void load(final CompoundTag tag) {
        speaker.load(tag.getCompound(SPEAKER_TAG));
        registered = false;
    }
}
