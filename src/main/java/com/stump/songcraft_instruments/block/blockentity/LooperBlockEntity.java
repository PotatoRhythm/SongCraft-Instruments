package com.stump.songcraft_instruments.block.blockentity;

import com.stump.songcraft_instruments.block.LooperBlock;
import com.stump.songcraft_instruments.block.blockentity.looper.LooperConnections;
import com.stump.songcraft_instruments.block.blockentity.looper.LooperPlayback;
import com.stump.songcraft_instruments.block.blockentity.looper.LooperRecordWriter;
import com.stump.songcraft_instruments.block.blockentity.looper.RecordingSession;
import com.stump.songcraft_instruments.item.ModItems;
import com.stump.songcraft_instruments.item.emirecord.EMIRecordItem;
import com.stump.songcraft_instruments.item.emirecord.RecordNotes;
import com.stump.songcraft_instruments.item.emirecord.RecordRepository;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.LooperPlayStatePacket;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.util.LooperUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;

import static com.stump.songcraft_instruments.item.emirecord.BurnedRecordItem.*;

/**
 * The looper's record slot, block state and timeline. Recording, connections, note writing and playback
 * are each handled by their own part: {@link #session()}, {@link #connections()}, {@link #writer()} and {@link #playback()}.
 */
public class LooperBlockEntity extends BlockEntity implements ContainerSingleItem {
    public static final String
        RECORD_TAG = "Record",
        TICKS_TAG = "Ticks"
    ;

    private ItemStack recordIn = ItemStack.EMPTY;
    private CompoundTag channel;

    private final RecordingSession session = new RecordingSession(this);
    private final LooperConnections connections = new LooperConnections(this);
    private final LooperRecordWriter writer = new LooperRecordWriter(this);
    private final LooperPlayback playback;

    public RecordingSession session() {
        return session;
    }
    public LooperConnections connections() {
        return connections;
    }
    public LooperRecordWriter writer() {
        return writer;
    }
    public LooperPlayback playback() {
        return playback;
    }


    /**
     * Retrieves the channel (footage) information from the inserted record
     */
    public CompoundTag getChannel() {
        return channel;
    }

    private void setChannel(final CompoundTag channel) {
        this.channel = channel;
    }

    /**
     * Retrieves the channel (footage) information from the inserted record
     */
    private void updateChannel() {
        final CompoundTag recordData = recordIn.getOrCreateTag();

        if (recordData.contains(CHANNEL_TAG, Tag.TAG_COMPOUND)) {
            channel = recordData.getCompound(CHANNEL_TAG);
        }
        else if (recordData.contains(BURNED_MEDIA_TAG, Tag.TAG_STRING)) {
            setChannel(RecordRepository.getRecord(getBurnedMediaLoc()).orElse(null));
        }

        // Burned media and migrated legacy loopers may still hold unpacked notes
        if ((channel != null) && RecordNotes.pack(channel))
            updateRecordNBT();
    }
    protected ResourceLocation getBurnedMediaLoc() {
        return new ResourceLocation(recordIn.getTag().getString(BURNED_MEDIA_TAG));
    }

    private void updateRecordNBT() {
        getPersistentData().put(RECORD_TAG, recordIn.save(new CompoundTag()));
    }

    public boolean hasFootage() {
        final CompoundTag channel = getChannel();
        return (channel != null) && !RecordNotes.getNotes(channel).isEmpty();
    }

    public boolean isWritable() {
        return (getChannel() != null) && getChannel().getBoolean(WRITABLE_TAG);
    }
    public void setWritable(final boolean writable) {
        getChannel().putBoolean(WRITABLE_TAG, writable);
    }

    public boolean isRecordIn() {
        return !recordIn.isEmpty();
    }
    protected CompoundTag getRecordData() {
        return recordIn.getOrCreateTag();
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        recordIn = ItemStack.of(getPersistentData().getCompound(RECORD_TAG));
        updateChannel();
        connections.load();
        session.load();
    }

    //#region ContainerSingleItem implementation

    // Assuming for single container, slots irrelevant:

    @Override
    public ItemStack getItem(int pSlot) {
        return recordIn;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        if (!(pStack.getItem() instanceof EMIRecordItem recordItem))
            return;

        recordIn = pStack.copyWithCount(1);
        recordItem.onInsert(recordIn, this);

        updateChannel();

        BlockState newState = getBlockState().setValue(LooperBlock.RECORD_IN, true);
        if (hasFootage())
            newState = setPlaying(true, newState);

        updateRecordNBT();

        getLevel().setBlock(getBlockPos(), newState, 3);
        setChanged();
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        if (!isRecordIn() || pAmount <= 0)
            return ItemStack.EMPTY;

        final ItemStack prev = recordIn;
        recordIn = ItemStack.EMPTY;

        getLevel().setBlock(getBlockPos(),
             setPlaying(false, getBlockState())
            .setValue(LooperBlock.RECORD_IN, false),
            3
        );

        getPersistentData().remove(RECORD_TAG);
        reset();

        return prev;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return Container.stillValidBlockEntity(this, pPlayer);
    }

    @Override
    public boolean canPlaceItem(int pIndex, ItemStack pStack) {
        return (pStack.getItem() instanceof EMIRecordItem) && !isRecordIn();
    }

    @Override
    public boolean canTakeItem(Container pTarget, int pIndex, ItemStack pStack) {
        return !isRecordIn();
    }

    //#endregion


    public LooperBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.LOOPER.get(), pPos, pBlockState);
        this.playback = new LooperPlayback(this, pPos);

        final CompoundTag data = getPersistentData();

        if (!data.contains(TICKS_TAG, CompoundTag.TAG_INT))
            setTicks(0);
    }


    public void setTicks(final int ticks) {
        getPersistentData().putInt(TICKS_TAG, ticks);
    }

    /**
     * Increment the ticks of this looper by 1. Wrap back to the start
     * if the track finished playing.
     * @return The new tick value
     */
    public int incrementTick() {
        int ticks = getTicks();
        final int repTick = getRepeatTick();

        // Finished playing?
        if ((repTick != -1) && (ticks > repTick))
            ticks = onLooperEnd();
        else
            ticks++;

        setTicks(ticks);
        return ticks;
    }

    /**
     * Called after the Looper had finished playing a cycle.
     * Responsible for either replaying or ceasing,
     * depending on the {@link LooperBlock#LOOPING} state.
     *
     * @return The ticks to set the looper at
     */
    public int onLooperEnd() {
        // If we don't loop, disable playing
        if (!getBlockState().getValue(LooperBlock.LOOPING))
            getLevel().setBlockAndUpdate(getBlockPos(), setPlaying(false, getBlockState()));

        playback.stopAndClearHeldSounds();

        return 0;
    }

    public void setRepeatTick(final int tick) {
        getChannel().putInt(REPEAT_TICK_TAG, tick);
    }

    /**
     * Used for stopping the Looper's recording
     */
    public void lock() {
        session.onFinalized();

        playback.stopAndClearHeldSounds();

        setRepeatTick(getTicks());
        session.setRecording(false);
        setWritable(false);

        // The record is burned; nothing is left to connect to
        connections.clear();

        setTicks(0);

        updateRecordNBT();
        setChanged();
    }

    /**
     * This method resets the looper, assuming it is not recording.
     */
    public void reset() {
        session.reset();

        setTicks(0);

        setChanged();
    }

    public int getTicks() {
        return getPersistentData().getInt(TICKS_TAG);
    }
    public int getRepeatTick() {
        final CompoundTag channel = getChannel();

        if (channel.contains(REPEAT_TICK_TAG))
            return channel.getInt(REPEAT_TICK_TAG);
        else
            return -1;
    }


    /**
     * Updates the playing state of this looper.
     * Ignores {@code isPlaying} to be false on the condition this looper contains no footage.
     * @return The current block state with the set {@code playing} value
     */
    public BlockState setPlaying(final boolean playing, final BlockState state) {
        final boolean isPlaying = hasFootage() && playing;
        final BlockState newState = state.setValue(LooperBlock.PLAYING, isPlaying);

        // If it's the server;
        if (!getLevel().isClientSide) {
            // Update the clients
            getLevel().players().forEach((player) ->
                SCPacketHandler.sendToClient(new LooperPlayStatePacket(isPlaying, getBlockPos()), (ServerPlayer)player)
            );

            // Cycle held notes
            playback.notifyHeldNotesPhase(playing ? HeldSoundPhase.ATTACK : HeldSoundPhase.RELEASE);
        }

        return newState;
    }


    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        // idk why the state isn't updated but here we are
        final LooperBlockEntity lbe = LooperUtil.getFromPos(pLevel, pPos);
        final boolean isPlaying = lbe.getBlockState().getValue(LooperBlock.PLAYING);

        lbe.session().tick();

        if (!isPlaying && !lbe.session().isRecording())
            return;

        if (lbe.session().isRecording())
            lbe.incrementTick();

        if (!isPlaying)
            return;

        final CompoundTag channel = lbe.getChannel();
        if (channel == null)
            return;

        playback.emitHeldParticles();

        final int ticks = getTicks();

        RecordNotes.getNotes(channel).stream()
            .map((note) -> ((IntArrayTag) note).getAsIntArray())
            .filter((note) -> note[RecordNotes.TIMESTAMP] == ticks)
            .forEach(lbe.playback()::playNote);

        lbe.incrementTick();
    }

    public void popRecord() {
        final CompoundTag recordData = getRecordData();

        if (recordIn.is(ModItems.RECORD_WRITABLE.get())) {
            // Record ejected while player writing to the record; remove notes
            if (isWritable())
                RecordNotes.clear(getChannel());
            // Empty record; empty data.
            if (!hasFootage())
                recordData.remove(CHANNEL_TAG);
        }

        playback.stopAndClearHeldSounds();

        // Finally, pop it
        Vec3 popVec = Vec3.atLowerCornerWithOffset(getBlockPos(), 0.5D, 1.01D, 0.5D)
            .offsetRandom(getLevel().random, 0.7F);

        ItemEntity itementity = new ItemEntity(getLevel(), popVec.x(), popVec.y(), popVec.z(), recordIn);
        itementity.setDefaultPickUpDelay();
        getLevel().addFreshEntity(itementity);

        removeItem(0, 1);
    }


    @Override
    public void setRemoved() {
        super.setRemoved();
        playback.stopAndClearHeldSounds();
    }
}
