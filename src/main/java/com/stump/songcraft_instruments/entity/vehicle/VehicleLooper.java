package com.stump.songcraft_instruments.entity.vehicle;

import com.stump.songcraft_instruments.block.LooperBlock;
import com.stump.songcraft_instruments.block.ModBlocks;
import com.stump.songcraft_instruments.block.blockentity.looper.LooperOwners;
import com.stump.songcraft_instruments.block.blockentity.looper.LooperPlayback;
import com.stump.songcraft_instruments.block.blockentity.looper.LooperVolumeControls;
import com.stump.songcraft_instruments.block.blockentity.speaker.Speaker;
import com.stump.songcraft_instruments.item.record.WritableRecordItem;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.recording.Recording;
import com.stump.songcraft_instruments.recording.RecordingStore;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import net.minecraft.ChatFormatting;
import com.stump.songcraft_instruments.networking.packet.instrument.s2c.S2CLooperParticlePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A looper carried by a boat or minecart. It only plays back the inserted record:
 * instruments can't connect to it, so nothing is recorded on it.
 * Its record slot is a single slot container, shown through {@link LooperVehicleMenu}.
 */
public class VehicleLooper implements Container, LooperPlayback.Host {
    private static final String RECORD_TAG = "Record", TICKS_TAG = "Ticks", PLAYING_TAG = "Playing", LOOPING_TAG = "Looping",
        VOLUME_TAG = "Volume";

    // The looks of the carried looper, synced to clients as flags
    public static final byte FLAG_RECORD_IN = 1, FLAG_PLAYING = 2, FLAG_LOOPING = 4;

    // What the record slot holds, as shown by the menu
    public static final int STATUS_NO_RECORD = 0, STATUS_EMPTY = 1, STATUS_MISSING = 2, STATUS_READY = 3;
    public static final int
        DATA_PLAYING = 0,
        DATA_LOOPING = 1,
        DATA_STATUS = 2,
        // In seconds, as container data is synced as shorts
        DATA_ELAPSED = 3,
        DATA_LENGTH = 4,
        // In percent
        DATA_VOLUME = 5,
        DATA_COUNT = 6;

    private final Entity vehicle;
    private final LooperPlayback playback;
    /**
     * The volume, set through the menu. On the client, animates the buttons and indicator of the carried looper.
     */
    private final LooperVolumeControls volumeControls = new LooperVolumeControls();

    private ItemStack recordIn = ItemStack.EMPTY;
    /**
     * The recording of the inserted burned record, loaded from the world's {@link RecordingStore} on first use
     */
    private @Nullable Recording recording;
    private boolean recordingResolved = false;

    private int ticks = 0;
    private boolean playing = false;
    private boolean looping = true;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(final int index) {
            return switch (index) {
                case DATA_PLAYING -> playing ? 1 : 0;
                case DATA_LOOPING -> looping ? 1 : 0;
                case DATA_STATUS -> getStatus();
                case DATA_ELAPSED -> ticks / 20;
                case DATA_LENGTH -> {
                    final Recording recording = getRecording();
                    yield (recording == null) ? 0 : recording.length() / 20;
                }
                case DATA_VOLUME -> volumeControls.getVolume();
                default -> 0;
            };
        }
        @Override
        public void set(final int index, final int value) {}
        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public VehicleLooper(final Entity vehicle) {
        this.vehicle = vehicle;
        this.playback = new LooperPlayback(this, new InitiatorID("vehicle", String.valueOf(vehicle.getId())));
    }

    public ContainerData data() {
        return data;
    }
    public LooperVolumeControls volumeControls() {
        return volumeControls;
    }


    /**
     * @return The recording of the inserted burned record, or null if there is none
     * (or its recording is missing from this world). Server only.
     */
    public @Nullable Recording getRecording() {
        final Level level = vehicle.level();
        if (!recordingResolved && !level.isClientSide && level.getServer() != null) {
            recordingResolved = true;

            final String id = WritableRecordItem.getRecordingId(recordIn);
            if (id != null) {
                final RecordingStore store = RecordingStore.get(level.getServer());
                recording = store.get(id).orElse(null);
                store.markSeen(id);
            }
        }

        return recording;
    }

    public boolean hasFootage() {
        final Recording recording = getRecording();
        return (recording != null) && (recording.noteCount() > 0);
    }

    private int getStatus() {
        if (recordIn.isEmpty())
            return STATUS_NO_RECORD;
        if (WritableRecordItem.getRecordingId(recordIn) == null)
            return STATUS_EMPTY;
        if (getRecording() == null)
            return STATUS_MISSING;
        return STATUS_READY;
    }

    public byte getFlags() {
        byte flags = 0;
        if (!recordIn.isEmpty())
            flags |= FLAG_RECORD_IN;
        if (playing)
            flags |= FLAG_PLAYING;
        if (looping)
            flags |= FLAG_LOOPING;
        return flags;
    }
    /**
     * @return How the carried looper looks, by its synced {@link #getFlags() flags}
     */
    public static BlockState displayState(final byte flags) {
        return ModBlocks.LOOPER.get().defaultBlockState()
            .setValue(LooperBlock.RECORD_IN, (flags & FLAG_RECORD_IN) != 0)
            .setValue(LooperBlock.PLAYING, (flags & FLAG_PLAYING) != 0)
            .setValue(LooperBlock.LOOPING, (flags & FLAG_LOOPING) != 0);
    }


    //#region Interaction

    /**
     * Inserts the held record, giving back the one inserted before (if any)
     * @return {@link InteractionResult#PASS} if the player isn't holding a record
     */
    public InteractionResult insertHeldRecord(final Player player, final InteractionHand hand) {
        final ItemStack heldStack = player.getItemInHand(hand);
        if (!(heldStack.getItem() instanceof WritableRecordItem))
            return InteractionResult.PASS;
        if (vehicle.level().isClientSide)
            return InteractionResult.SUCCESS;

        final ItemStack prev = recordIn;
        setItem(0, heldStack.copyWithCount(1));
        if (!player.getAbilities().instabuild)
            heldStack.shrink(1);
        if (!prev.isEmpty())
            player.getInventory().placeItemBackInInventory(prev);

        if (getStatus() == STATUS_MISSING) {
            player.displayClientMessage(
                Component.translatable("songcraft_instruments.record.missing").withStyle(ChatFormatting.RED)
            , true);
        } else if (!hasFootage()) {
            player.displayClientMessage(
                Component.translatable("songcraft_instruments.looper_vehicle.no_footage").withStyle(ChatFormatting.YELLOW)
            , true);
        }

        return InteractionResult.SUCCESS;
    }

    public InteractionResult openMenu(final Player player) {
        if (!vehicle.level().isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new LooperVehicleMenu(containerId, inventory, this, data),
                vehicle.getDisplayName()
            ));
        }
        return InteractionResult.sidedSuccess(vehicle.level().isClientSide);
    }

    //#endregion


    //#region Playback

    public boolean isPlaying() {
        return playing;
    }
    /**
     * Ignores {@code playing} to be true on the condition no footage is inserted
     */
    public void setPlaying(final boolean playing) {
        final boolean isPlaying = playing && hasFootage();
        if (this.playing == isPlaying)
            return;

        this.playing = isPlaying;
        playback.notifyHeldNotesPhase(isPlaying ? HeldSoundPhase.ATTACK : HeldSoundPhase.RELEASE);
    }
    public void cyclePlaying() {
        setPlaying(!playing);
    }
    public void cycleLooping() {
        looping = !looping;
    }

    /**
     * Turns the volume up or down by 10%, clicking like the looper block's buttons. Server only.
     */
    public void stepVolume(final Player player, final boolean up) {
        volumeControls.stepVolume(up);

        vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
            SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3f, up ? 0.7f : 0.6f);
        player.displayClientMessage(
            Component.translatable("songcraft_instruments.looper.volume", volumeControls.getVolume()),
            true
        );
    }

    /**
     * Shows a volume synced from the server on the carried looper, pressing the button that changed it. Client only.
     */
    public void onVolumeSynced(final int volume) {
        final int prevVolume = volumeControls.getVolume();
        volumeControls.syncVolume(volume);

        if (volumeControls.getVolume() != prevVolume)
            volumeControls.onButtonPressed(volumeControls.getVolume() > prevVolume);
    }

    /**
     * Server only
     */
    public void tick() {
        if (!playing)
            return;

        final Recording recording = getRecording();
        if (recording == null) {
            setPlaying(false);
            return;
        }

        playback.emitHeldParticles();

        // Keeps the recording from being cleaned up while it's played
        if (ticks == 0)
            RecordingStore.get(vehicle.level().getServer()).markSeen(WritableRecordItem.getRecordingId(recordIn));

        recording.forEachNoteAt(ticks, (note) -> playback.playNote(recording, note));

        // Finished playing?
        if (ticks > recording.length()) {
            ticks = 0;
            playback.stopAndClearHeldSounds();
            if (!looping)
                setPlaying(false);
        } else {
            ticks++;
        }
    }

    /**
     * Stops the notes being played, as the vehicle is removed or unloaded.
     * @param destroyed Whether the vehicle was destroyed, dropping the inserted record
     */
    public void onRemoved(final boolean destroyed) {
        playback.stopAndClearHeldSounds();

        if (destroyed && !recordIn.isEmpty()) {
            Containers.dropItemStack(vehicle.level(), vehicle.getX(), vehicle.getY(), vehicle.getZ(), recordIn);
            recordIn = ItemStack.EMPTY;
        }

        if (destroyed && vehicle.level().getServer() != null)
            LooperOwners.get(vehicle.level().getServer()).removeVehicle(vehicle.getUUID());
    }

    //#endregion


    //#region LooperPlayback.Host implementation

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
        return soundPos(vehicle);
    }
    @Override
    public int getVolume() {
        return volumeControls.getVolume();
    }
    /**
     * Loopers carried by vehicles can't be paired to speakers
     */
    @Override
    public List<Speaker> getPairedSpeakers() {
        return List.of();
    }

    /**
     * @return Where a vehicle carrying a looper or speaker plays its notes from;
     * particles are spawned above it
     */
    public static BlockPos soundPos(final Entity vehicle) {
        return BlockPos.containing(vehicle.getX(), vehicle.getY() + 0.5, vehicle.getZ());
    }

    //#endregion


    //#region Container implementation

    @Override
    public int getContainerSize() {
        return 1;
    }
    @Override
    public boolean isEmpty() {
        return recordIn.isEmpty();
    }
    @Override
    public ItemStack getItem(final int slot) {
        return recordIn;
    }

    @Override
    public void setItem(final int slot, final ItemStack stack) {
        playback.stopAndClearHeldSounds();

        recordIn = stack;
        recording = null;
        recordingResolved = false;
        ticks = 0;
        playing = false;

        // Starts playing as soon as a record is inserted, like the looper block
        if (!recordIn.isEmpty())
            setPlaying(true);
    }
    @Override
    public ItemStack removeItem(final int slot, final int amount) {
        if (recordIn.isEmpty() || amount <= 0)
            return ItemStack.EMPTY;

        final ItemStack prev = recordIn;
        setItem(slot, ItemStack.EMPTY);
        return prev;
    }
    @Override
    public ItemStack removeItemNoUpdate(final int slot) {
        return removeItem(slot, 1);
    }
    @Override
    public void clearContent() {
        setItem(0, ItemStack.EMPTY);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return stack.getItem() instanceof WritableRecordItem;
    }

    @Override
    public void setChanged() {}
    @Override
    public boolean stillValid(final Player player) {
        return vehicle.isAlive() && player.distanceToSqr(vehicle) <= 64;
    }

    //#endregion


    public void save(final CompoundTag tag) {
        if (!recordIn.isEmpty())
            tag.put(RECORD_TAG, recordIn.save(new CompoundTag()));
        tag.putInt(TICKS_TAG, ticks);
        tag.putBoolean(PLAYING_TAG, playing);
        tag.putBoolean(LOOPING_TAG, looping);
        tag.putInt(VOLUME_TAG, volumeControls.getVolume());
    }
    public void load(final CompoundTag tag) {
        recordIn = ItemStack.of(tag.getCompound(RECORD_TAG));
        recording = null;
        recordingResolved = false;

        ticks = tag.getInt(TICKS_TAG);
        playing = tag.getBoolean(PLAYING_TAG);
        looping = !tag.contains(LOOPING_TAG) || tag.getBoolean(LOOPING_TAG);
        volumeControls.setVolume(tag.contains(VOLUME_TAG) ? tag.getInt(VOLUME_TAG) : LooperVolumeControls.MAX_VOLUME);
    }
}
