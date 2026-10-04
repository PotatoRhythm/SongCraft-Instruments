package com.stump.songcraft_instruments.block.blockentity.looper;

import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.block.util.LooperSessionState;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.capability.recording.RecordingCapabilityProvider;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/**
 * The recording state of a looper, for both solo and group recordings.
 * <p>
 * A solo recording is started by its player pressing record, and locks the looper to them once they play their first note.
 * A group recording is started with multiple players connected: it counts in with beeps, then records every participant
 * from the first note played after the final beep.
 */
public class RecordingSession {
    public static final String
        RECORDING_TAG = "Recording",
        GROUP_STATE_TAG = "GroupState"
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

    // Solo recording
    private boolean locked = false;
    private Player lockedBy = null;

    /**
     * The state of the current group recording session. {@link LooperSessionState#IDLE} when there is none,
     * in which case recording is done solo.
     */
    private LooperSessionState groupState = LooperSessionState.IDLE;
    private int countdownTicks = 0;
    /**
     * The ticks since the last participant left their instrument during a group session, or -1 while someone is present
     */
    private int absentTicks = -1;

    public RecordingSession(final LooperBlockEntity looper) {
        this.looper = looper;
    }

    public void load() {
        final String rawGroupState = looper.getPersistentData().getString(GROUP_STATE_TAG);
        groupState = rawGroupState.isEmpty() ? LooperSessionState.IDLE : LooperSessionState.valueOf(rawGroupState);
        // Restart an interrupted countdown from the top
        countdownTicks = 0;
        absentTicks = -1;
    }


    public LooperSessionState getState() {
        if (isGroupSession())
            return groupState;

        return isRecording() ? LooperSessionState.RECORDING : LooperSessionState.IDLE;
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
     * New connections are refused while a recording is in progress, or a group recording's countdown started.
     */
    public boolean canAcceptConnections() {
        return getState() == LooperSessionState.IDLE && !isSoloArmed();
    }

    public boolean isInRecordRange(final Player player) {
        return looper.getBlockPos().closerToCenterThan(player.position(), MAX_RECORD_DIST);
    }

    /**
     * Called when the recording is finalized, as the looper is {@link LooperBlockEntity#lock locked}.
     */
    public void onFinalized() {
        locked = true;
        lockedBy = null;
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
        lockedBy = null;

        if (isGroupSession()) {
            setRecording(false);
            setGroupState(LooperSessionState.IDLE);
        }
    }


    //#region Solo recording

    public void setLockedBy(final Player player) {
        lockedBy = player;
    }

    public boolean isLocked() {
        return lockedByAnyone() || locked;
    }

    public boolean isAllowedToRecord(final Player player) {
        return !lockedByAnyone() || isLockedBy(player);
    }
    public boolean lockedByAnyone() {
        return lockedBy != null;
    }
    public boolean isLockedBy(final Player player) {
        return player.equals(lockedBy);
    }

    /**
     * @return Whether a connected player pressed record for a solo recording, but has not yet played a note
     */
    private boolean isSoloArmed() {
        if (looper.getLevel() == null || looper.getLevel().getServer() == null)
            return false;

        return looper.connections().getPlayerIds().stream()
            .map(looper.getLevel().getServer().getPlayerList()::getPlayer)
            .anyMatch((player) -> player != null
                && RecordingCapabilityProvider.isRecording(player)
                && looper.getBlockPos().equals(RecordingCapabilityProvider.getLooperPos(player))
            );
    }

    /**
     * Discards everything recorded so far (if anything), going back to waiting for the recording player's first note.
     * A single beep signals the restart.
     * @param player A player who pressed record on this looper
     * @return Whether the recording was restarted
     */
    public boolean restartSoloRecording(final Player player) {
        if (isGroupSession() || (isRecording() && !isLockedBy(player)))
            return false;

        if (isRecording()) {
            looper.writer().clearRecordedNotes();
            // The next note played by the player locks the looper again
            lockedBy = null;
            locked = false;
            setRecording(false);
        }

        playCountdownBeep(true);
        return true;
    }

    //#endregion


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
        setGroupState(LooperSessionState.COUNTDOWN);
        return true;
    }

    /**
     * Stops the group session. If notes were recorded, the record is finalized and played back.
     * Otherwise, the session is cancelled and players remain connected.
     */
    public void stopGroupSession() {
        if (groupState == LooperSessionState.RECORDING) {
            looper.lock();
            looper.getLevel().setBlockAndUpdate(looper.getBlockPos(), looper.setPlaying(true, looper.getBlockState()));
        } else if (isGroupSession()) {
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
                setGroupState(LooperSessionState.ARMED);
                return;
            }
        }

        countdownTicks++;
    }

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
