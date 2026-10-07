package com.stump.songcraft_instruments.block.blockentity.looper;

import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.capability.recording.RecordingCapabilityProvider;
import com.stump.songcraft_instruments.config.ModServerConfigs;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.recording.Recording;
import com.stump.songcraft_instruments.recording.RecordingBuilder;
import com.stump.songcraft_instruments.recording.RecordingCodec;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Writes recorded notes into the looper's {@link LooperBlockEntity#getDraft draft},
 * which becomes the inserted record's recording once the recording is finalized.
 */
public class LooperRecordWriter {
    private final LooperBlockEntity looper;

    public LooperRecordWriter(final LooperBlockEntity looper) {
        this.looper = looper;
    }


    /**
     * Writes a new note to the writable record.
     */
    public void writeNote(NoteSound sound, NoteSoundMetadata soundMeta, int timestamp, int particleRgb, UUID performer) {
        final RecordingBuilder draft = looper.getDraft();
        if (draft == null)
            return;

        draft.addNote(timestamp, getPerformerIndex(draft, performer),
            soundMeta.pitch(), soundMeta.volume(), particleRgb,
            soundMeta.instrumentId().toString(), sound.baseSoundLocation.toString(), sound.index
        );
        looper.setChanged();
    }
    /**
     * Writes a new note to the writable record.
     */
    public void writeHeldNote(HeldNoteSound sound, HeldSoundPhase phase,
                              NoteSoundMetadata soundMeta, int timestamp,
                              int particleRgb, UUID performer) {
        final RecordingBuilder draft = looper.getDraft();
        if (draft == null)
            return;

        draft.addHeldNote(timestamp, getPerformerIndex(draft, performer),
            soundMeta.pitch(), soundMeta.volume(), particleRgb,
            soundMeta.instrumentId().toString(), sound.baseSoundLocation().toString(), sound.index(),
            (phase == HeldSoundPhase.RELEASE) ? Recording.PHASE_RELEASE : Recording.PHASE_ATTACK
        );
        looper.setChanged();
    }

    public void writeDampen(int timestamp, UUID performer) {
        final RecordingBuilder draft = looper.getDraft();
        if (draft == null)
            return;

        draft.addDampen(timestamp, getPerformerIndex(draft, performer));
        looper.setChanged();
    }

    /**
     * Each player recording on a record is a performer, identified in its notes by their index in the
     * recording's performer list. Performers are played back as separate initiators, so that dampening
     * and held notes of one do not affect the others.
     * @return The index of the performer, added to the recording if not yet present
     */
    private int getPerformerIndex(final RecordingBuilder draft, final UUID performer) {
        return draft.performerIndex(performer, () -> getPlayerName(performer), () -> getPlayerParticleColors(performer));
    }

    /**
     * @return The particle colors the performer's client sent when the recording started. Empty if unknown.
     */
    private int[] getPlayerParticleColors(final UUID playerId) {
        final Player player = looper.getLevel().getPlayerByUUID(playerId);
        return (player != null) ? RecordingCapabilityProvider.getParticleColors(player) : new int[0];
    }

    /**
     * @return The name of the (online) performer, as shown on the record's tooltip
     */
    private String getPlayerName(final UUID playerId) {
        final Player player = looper.getLevel().getPlayerByUUID(playerId);
        return (player != null) ? player.getGameProfile().getName() : playerId.toString();
    }

    /**
     * Discards everything recorded so far
     */
    public void clearRecordedNotes() {
        if (looper.getDraft() != null)
            looper.getDraft().clear();
        looper.setTicks(0);
        looper.setChanged();
    }

    /**
     * A capped looper is a looper that cannot have any more notes in it, as defined in {@link ModServerConfigs#RECORD_MAX_NOTES}.
     * A negative limit will make the looper uncappable by the config, though recordings always end at {@link RecordingCodec#MAX_TICK} ticks.
     * @return Whether this looper is capped
     */
    public boolean isCapped(final Level level) {
        final RecordingBuilder draft = looper.getDraft();
        if (draft == null)
            return true;

        final int cap = ModServerConfigs.RECORD_MAX_NOTES.get();
        return ((cap >= 0) && (draft.noteCount() >= cap))
            || (looper.getTicks() > RecordingCodec.MAX_TICK);
    }
}
