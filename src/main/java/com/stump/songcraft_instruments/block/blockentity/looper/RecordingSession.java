package com.stump.songcraft_instruments.block.blockentity.looper;

import com.stump.songcraft_instruments.block.LooperBlock;
import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.block.util.LooperSessionState;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.item.record.WritableRecordItem;
import com.stump.songcraft_instruments.recording.Recording;
import com.stump.songcraft_instruments.recording.RecordingStore;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * The recording state of a looper.
 * <p>
 * Recording always goes through a session, be it with one connected player or many: it counts in with beeps,
 * then records every participant from the first note played after the final beep.
 * <p>
 * A looper holding an empty record, synced to a looper holding a burned one, records over it.
 * On the final beep, the burned record plays from its start (once) and the recording begins.
 * The burned record's notes are burned into the new record along with the new part.
 */
public class RecordingSession {
    public static final String
        RECORDING_TAG = "Recording",
        GROUP_STATE_TAG = "GroupState",
        // The recording of the synced looper's record being recorded over, and the record's name
        BACKING_ID_TAG = "BackingId",
        BACKING_NAME_TAG = "BackingName"
    ;

    public static final int COUNTDOWN_BEEPS = 4, COUNTDOWN_BEEP_INTERVAL = 20;
    /**
     * How long (in ticks) a group session continues without any participant on their instrument before it ends
     */
    public static final int GROUP_ABSENCE_TIMEOUT = 5 * 20;
    /**
     * The maximum distance (in blocks) from the looper a player may record from
     */
    public static final int MAX_RECORD_DIST = 16;

    private final LooperBlockEntity looper;

    private boolean locked = false;

    /**
     * The state of the current recording session. {@link LooperSessionState#IDLE} when there is none.
     */
    private LooperSessionState groupState = LooperSessionState.IDLE;
    private int countdownTicks = 0;
    /**
     * The ticks since the last participant left their instrument during a group session, or -1 while someone is present
     */
    private int absentTicks = -1;

    /**
     * The recording being recorded over, held while in use so that it stays loaded
     */
    private @Nullable Recording backing;
    private boolean backingResolved = false;

    public RecordingSession(final LooperBlockEntity looper) {
        this.looper = looper;
    }

    public void load() {
        final String rawGroupState = looper.getPersistentData().getString(GROUP_STATE_TAG);
        groupState = rawGroupState.isEmpty() ? LooperSessionState.IDLE : LooperSessionState.valueOf(rawGroupState);
        // Restart an interrupted countdown from the top
        countdownTicks = 0;
        absentTicks = -1;
        backing = null;
        backingResolved = false;
    }


    public LooperSessionState getState() {
        return groupState;
    }

    public boolean isRecording() {
        return looper.getPersistentData().getBoolean(RECORDING_TAG);
    }
    public void setRecording(final boolean recording) {
        final boolean changed = isRecording() != recording;
        looper.getPersistentData().putBoolean(RECORDING_TAG, recording);

        if (changed)
            looper.connections().sync();
    }

    /**
     * New connections are refused while a recording session is in progress.
     */
    public boolean canAcceptConnections() {
        return getState() == LooperSessionState.IDLE;
    }

    public boolean isInRecordRange(final Player player) {
        return looper.getBlockPos().closerToCenterThan(player.position(), MAX_RECORD_DIST);
    }

    /**
     * Called when the recording is finalized, as the looper is {@link LooperBlockEntity#lock locked}.
     */
    public void onFinalized() {
        locked = true;
        // Silently end the group session; the connections' removal will sync it
        groupState = LooperSessionState.IDLE;
        absentTicks = -1;
        looper.getPersistentData().remove(GROUP_STATE_TAG);
    }

    /**
     * Called when the looper is {@link LooperBlockEntity#reset reset}.
     */
    public void reset() {
        locked = false;

        if (isOverdub()) {
            stopBacking();
            clearBacking();
        }

        if (isGroupSession()) {
            setRecording(false);
            setGroupState(LooperSessionState.IDLE);
        }
    }


    public boolean isLocked() {
        return locked;
    }


    //#region Group recording

    public boolean isGroupSession() {
        return groupState != LooperSessionState.IDLE;
    }

    private void setGroupState(final LooperSessionState state) {
        groupState = state;
        if (state == LooperSessionState.IDLE)
            absentTicks = -1;
        looper.getPersistentData().putString(GROUP_STATE_TAG, state.name());
        looper.setChanged();
        looper.connections().sync();
    }

    /**
     * Starts the countdown for a group recording, locking the connected players in as its participants.
     * @return Whether the session started
     */
    public boolean startGroupSession() {
        if (getState() != LooperSessionState.IDLE || isLocked() || !looper.isWritable())
            return false;

        countdownTicks = 0;
        looper.setTicks(0);
        captureBacking();
        if (isOverdub())
            stopSyncedLooper();
        setGroupState(LooperSessionState.COUNTDOWN);
        return true;
    }

    /**
     * Stops the group session. If notes were recorded, the record is finalized and played back.
     * Otherwise, the session is cancelled and players remain connected.
     */
    public void stopGroupSession() {
        if (isOverdub())
            stopBacking();

        // Recording over a record starts on the final beep; cancel if nothing was played since
        if (groupState == LooperSessionState.RECORDING && !looper.hasFootage()) {
            looper.writer().clearRecordedNotes();
            clearBacking();
            setRecording(false);
            setGroupState(LooperSessionState.IDLE);
        } else if (groupState == LooperSessionState.RECORDING) {
            looper.lock();
            looper.getLevel().setBlockAndUpdate(looper.getBlockPos(), looper.setPlaying(true, looper.getBlockState()));
        } else if (isGroupSession()) {
            clearBacking();
            setGroupState(LooperSessionState.IDLE);
        }
    }

    /**
     * Discards everything recorded so far (if anything), and runs the countdown again with the same participants.
     * @return Whether the session was restarted
     */
    public boolean restartGroupSession() {
        if (!isGroupSession())
            return false;

        looper.writer().clearRecordedNotes();
        if (isOverdub())
            stopBacking();
        countdownTicks = 0;
        setGroupState(LooperSessionState.COUNTDOWN);
        setRecording(false);
        return true;
    }

    /**
     * Called for every note a participant plays during a group session.
     * The first note after the countdown starts the recording.
     * @return Whether the note should be recorded
     */
    public boolean acceptGroupNote() {
        switch (groupState) {
            case ARMED:
                setRecording(true);
                setGroupState(LooperSessionState.RECORDING);
                return true;
            case RECORDING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Before the record button is pressed, only updates everyone on who is still present.
     * During a group session, the session ends once no participant has had any instrument open
     * for {@link #GROUP_ABSENCE_TIMEOUT} ticks.
     * @param leavingPlayer A participant closing their instrument or logging out
     */
    public void onParticipantLeft(final UUID leavingPlayer) {
        if (looper.getLevel() == null || looper.getLevel().getServer() == null)
            return;

        // Before the record button is pressed, only players on their connected instrument count.
        // Update everyone on who is still present.
        if (!isGroupSession()) {
            looper.connections().sync(leavingPlayer, List.of());
            return;
        }

        // Give the participants a moment to come back (e.g. switching instruments) before ending the session
        if (absentTicks < 0 && !isAnyParticipantPresent(leavingPlayer))
            absentTicks = 0;
    }

    /**
     * @param excludedPlayer A participant not to count, or null
     * @return Whether any participant has an instrument open
     */
    private boolean isAnyParticipantPresent(final UUID excludedPlayer) {
        final PlayerList playerList = looper.getLevel().getServer().getPlayerList();
        return looper.connections().getPlayerIds().stream()
            .filter((playerId) -> !playerId.equals(excludedPlayer))
            .map(playerList::getPlayer)
            .anyMatch((player) -> player != null && InstrumentOpenProvider.isOpen(player));
    }

    /**
     * Ends the group session once its participants have been away for {@link #GROUP_ABSENCE_TIMEOUT} ticks,
     * or stops counting if one of them came back.
     */
    private void tickAbsence() {
        if (absentTicks < 0)
            return;

        if (!isGroupSession() || looper.getLevel().getServer() == null || isAnyParticipantPresent(null)) {
            absentTicks = -1;
            return;
        }

        if (++absentTicks >= GROUP_ABSENCE_TIMEOUT) {
            absentTicks = -1;
            stopGroupSession();
        }
    }

    /**
     * Plays the countdown beeps, arming the looper on the final one.
     */
    public void tick() {
        tickAbsence();

        if (groupState != LooperSessionState.COUNTDOWN)
            return;

        if (countdownTicks % COUNTDOWN_BEEP_INTERVAL == 0) {
            final int beep = countdownTicks / COUNTDOWN_BEEP_INTERVAL;
            final boolean isFinalBeep = beep == COUNTDOWN_BEEPS - 1;

            playCountdownBeep(isFinalBeep);

            if (isFinalBeep) {
                if (isOverdub())
                    startOverdub();
                else
                    setGroupState(LooperSessionState.ARMED);
                return;
            }
        }

        countdownTicks++;
    }

    //#region Recording over a synced looper's record

    /**
     * @return Whether this session records over a synced looper's record
     */
    public boolean isOverdub() {
        return looper.getPersistentData().contains(BACKING_ID_TAG, Tag.TAG_STRING);
    }

    /**
     * Remembers the synced looper's burned record (if any) to record over,
     * so that swapping it out mid-session changes nothing.
     */
    private void captureBacking() {
        clearBacking();

        final LooperBlockEntity backingLooper = looper.getBackingLooper();
        if (backingLooper == null)
            return;

        final ItemStack record = backingLooper.getItem(0);
        looper.getPersistentData().putString(BACKING_ID_TAG, WritableRecordItem.getRecordingId(record));
        looper.getPersistentData().putString(BACKING_NAME_TAG, Component.Serializer.toJson(record.getHoverName()));
        backing = backingLooper.getRecording();
        backingResolved = true;
    }

    public void clearBacking() {
        looper.getPersistentData().remove(BACKING_ID_TAG);
        looper.getPersistentData().remove(BACKING_NAME_TAG);
        backing = null;
        backingResolved = false;
    }

    /**
     * @return The recording being recorded over, or null if there is none (or it is missing). Server only.
     */
    public @Nullable Recording getBacking() {
        if (!isOverdub())
            return null;

        if (!backingResolved && looper.getLevel() != null && looper.getLevel().getServer() != null) {
            backingResolved = true;
            backing = RecordingStore.get(looper.getLevel().getServer())
                .get(looper.getPersistentData().getString(BACKING_ID_TAG))
                .orElse(null);
        }

        return backing;
    }

    /**
     * @return The name of the record recorded over, or of the one that would be if a recording started now.
     * Null if there is none. Server only.
     */
    public @Nullable Component getBackingName() {
        if (isOverdub())
            return Component.Serializer.fromJson(looper.getPersistentData().getString(BACKING_NAME_TAG));

        final LooperBlockEntity backingLooper = isGroupSession() ? null : looper.getBackingLooper();
        return (backingLooper != null) ? backingLooper.getItem(0).getHoverName() : null;
    }

    /**
     * Starts recording on the final beep, along with the record being recorded over
     */
    private void startOverdub() {
        looper.setTicks(0);
        // In case it was started again during the countdown
        stopSyncedLooper();

        setRecording(true);
        setGroupState(LooperSessionState.RECORDING);
    }

    /**
     * Stops the synced looper if it is playing on its own, as its record is played by this session instead,
     * in time with the recording
     */
    private void stopSyncedLooper() {
        final LooperBlockEntity synced = looper.getSyncedLooper();
        if (synced == null || !synced.getBlockState().getValue(LooperBlock.PLAYING))
            return;

        synced.setTicks(0);
        synced.getLevel().setBlockAndUpdate(synced.getBlockPos(), synced.setPlaying(false, synced.getBlockState()));
    }

    /**
     * Plays the notes of the record being recorded over at the recording's current tick.
     * It plays only once; the recording may continue past its end.
     */
    public void playBacking(final int tick) {
        final Recording recording = getBacking();
        if (recording == null)
            return;

        final LooperPlayback playback = getBackingPlayback();
        if (tick > recording.length()) {
            if (tick == recording.length() + 1)
                playback.stopAndClearHeldSounds();
            return;
        }

        playback.emitHeldParticles();
        recording.forEachNoteAt(tick, (note) -> playback.playNote(recording, note));
    }

    /**
     * Releases the held notes of the record being recorded over
     */
    private void stopBacking() {
        getBackingPlayback().stopAndClearHeldSounds();
    }

    /**
     * @return The synced looper's playback, so that the record recorded over is heard from its own looper.
     * This looper's own if the synced looper is gone.
     */
    private LooperPlayback getBackingPlayback() {
        final LooperBlockEntity synced = looper.getSyncedLooper();
        return (synced != null) ? synced.playback() : looper.playback();
    }

    //#endregion


    /**
     * @param isFinalBeep The final beep signals that recording may begin, and is an octave higher
     */
    private void playCountdownBeep(final boolean isFinalBeep) {
        // A volume of 2 is heard up to 32 blocks away
        looper.getLevel().playSound(null, looper.getBlockPos(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.RECORDS,
            2, isFinalBeep ? 2 : 1
        );
    }

    //#endregion
}
