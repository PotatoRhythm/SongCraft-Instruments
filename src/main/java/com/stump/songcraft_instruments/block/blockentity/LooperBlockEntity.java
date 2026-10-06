package com.stump.songcraft_instruments.block.blockentity;

import com.stump.songcraft_instruments.block.LooperBlock;
import com.stump.songcraft_instruments.block.blockentity.looper.LooperConnections;
import com.stump.songcraft_instruments.block.blockentity.looper.LooperPlayback;
import com.stump.songcraft_instruments.block.blockentity.looper.LooperRecordWriter;
import com.stump.songcraft_instruments.block.blockentity.looper.RecordingSession;
import com.mojang.logging.LogUtils;
import com.stump.songcraft_instruments.item.record.WritableRecordItem;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.LooperPlayStatePacket;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.recording.Recording;
import com.stump.songcraft_instruments.recording.RecordingBuilder;
import com.stump.songcraft_instruments.recording.RecordingCodec;
import com.stump.songcraft_instruments.recording.RecordingStore;
import com.stump.songcraft_instruments.util.LooperUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * The looper's record slot, block state and timeline. Recording, connections, note writing and playback
 * are each handled by their own part: {@link #session()}, {@link #connections()}, {@link #writer()} and {@link #playback()}.
 */
public class LooperBlockEntity extends BlockEntity implements ContainerSingleItem {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String
        RECORD_TAG = "Record",
        TICKS_TAG = "Ticks",
        // The notes recorded so far onto an empty record, in the recording file format
        DRAFT_TAG = "Draft"
    ;

    private ItemStack recordIn = ItemStack.EMPTY;
    /**
     * The notes being recorded while an empty record is inserted, otherwise null
     */
    private @Nullable RecordingBuilder draft;
    /**
     * The recording of the inserted burned record, loaded from the world's {@link RecordingStore} on first use
     */
    private @Nullable Recording recording;
    private boolean recordingResolved = false;

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
     * @return The notes being recorded onto the inserted empty record, or null if no empty record is inserted
     */
    public @Nullable RecordingBuilder getDraft() {
        return draft;
    }

    /**
     * @return The recording of the inserted burned record, or null if there is none
     * (or its recording is missing from this world). Server only.
     */
    public @Nullable Recording getRecording() {
        if (!recordingResolved && level != null && !level.isClientSide && level.getServer() != null) {
            recordingResolved = true;

            final String id = WritableRecordItem.getRecordingId(recordIn);
            if (id != null) {
                final RecordingStore store = RecordingStore.get(level.getServer());
                recording = store.get(id).orElse(null);
                store.markSeen(id);

                if (recording == null)
                    LOGGER.warn("The recording {} of the record in the looper at {} is missing", id, getBlockPos());
            }
        }

        return recording;
    }

    /**
     * Reads the inserted record: an empty record starts a new draft, a burned one is looked up when first needed
     * @param savedDraft The draft saved with this looper, if any
     */
    private void updateRecording(final @Nullable byte[] savedDraft) {
        recording = null;
        recordingResolved = false;
        draft = null;

        if (!(recordIn.getItem() instanceof WritableRecordItem) || WritableRecordItem.getRecordingId(recordIn) != null)
            return;

        draft = new RecordingBuilder();
        if (savedDraft != null) {
            try {
                draft = new RecordingBuilder(RecordingCodec.decode(savedDraft));
            } catch (IOException e) {
                LOGGER.error("Could not read the unfinished recording of the looper at {}", getBlockPos(), e);
            }
        }
    }

    private void updateRecordNBT() {
        getPersistentData().put(RECORD_TAG, recordIn.save(new CompoundTag()));
    }

    public boolean hasFootage() {
        if (draft != null)
            return !draft.isEmpty();

        final Recording recording = getRecording();
        return (recording != null) && (recording.noteCount() > 0);
    }

    /**
     * @return Whether an empty record is inserted, which may still be recorded onto
     */
    public boolean isWritable() {
        return draft != null;
    }

    /**
     * @return Whether the inserted record is burned, but its recording is not in this world
     */
    public boolean isRecordingMissing() {
        return (WritableRecordItem.getRecordingId(recordIn) != null) && (getRecording() == null);
    }

    public boolean isRecordIn() {
        return !recordIn.isEmpty();
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        recordIn = ItemStack.of(getPersistentData().getCompound(RECORD_TAG));
        updateRecording(pTag.contains(DRAFT_TAG, Tag.TAG_BYTE_ARRAY) ? pTag.getByteArray(DRAFT_TAG) : null);
        connections.load();
        session.load();
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        // Saved with the looper rather than the world's recordings, as it may still be discarded.
        // Never sent to clients.
        if (draft != null && !draft.isEmpty())
            pTag.putByteArray(DRAFT_TAG, RecordingCodec.encode(draft.build(-1)));
    }

    //#region ContainerSingleItem implementation

    // Assuming for single container, slots irrelevant:

    @Override
    public ItemStack getItem(int pSlot) {
        return recordIn;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        if (!(pStack.getItem() instanceof WritableRecordItem))
            return;

        recordIn = pStack.copyWithCount(1);
        updateRecording(null);

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
        updateRecording(null);
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
        return (pStack.getItem() instanceof WritableRecordItem) && !isRecordIn();
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

    /**
     * Overrides when the inserted record loops back, as when syncing loopers
     */
    public void setRepeatTick(final int tick) {
        if (!isRecordIn())
            return;

        WritableRecordItem.setRepeatTickOverride(recordIn, tick);
        updateRecordNBT();
        setChanged();
    }

    /**
     * Used for stopping the Looper's recording
     */
    public void lock() {
        session.onFinalized();

        playback.stopAndClearHeldSounds();

        burnDraft();
        session.setRecording(false);

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
        final int override = WritableRecordItem.getRepeatTickOverride(recordIn);
        if (override != -1)
            return override;

        final Recording recording = getRecording();
        return (recording != null) ? recording.length() : -1;
    }

    /**
     * Stores the notes recorded so far as a recording of the world, and burns it into the inserted record
     */
    private void burnDraft() {
        if (draft == null || draft.isEmpty() || level == null || level.getServer() == null)
            return;

        final Recording finished = draft.build(Math.min(getTicks(), RecordingCodec.MAX_TICK));
        final String id;
        try {
            id = RecordingStore.get(level.getServer()).save(finished);
        } catch (IOException e) {
            LOGGER.error("Could not save the recording of the looper at {}", getBlockPos(), e);
            return;
        }

        WritableRecordItem.burn(recordIn, id, finished);
        draft = null;
        recording = finished;
        recordingResolved = true;
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

        final Recording recording = lbe.getRecording();
        if (recording == null)
            return;

        playback.emitHeldParticles();

        final int ticks = getTicks();
        // Keeps the recording from being cleaned up while it's played
        if (ticks == 0)
            RecordingStore.get(pLevel.getServer()).markSeen(WritableRecordItem.getRecordingId(recordIn));

        recording.forEachNoteAt(ticks, lbe.playback()::playNote);

        lbe.incrementTick();
    }

    public void popRecord() {
        // Ejected while recording; the notes recorded so far are discarded, and the record comes out empty
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
