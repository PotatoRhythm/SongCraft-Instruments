package com.stump.songcraft_instruments.block.blockentity.looper;

import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.capability.recording.RecordingCapabilityProvider;
import com.stump.songcraft_instruments.gamerule.ModGameRules;
import com.stump.songcraft_instruments.item.emirecord.RecordNotes;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import com.stump.songcraft_instruments.util.CommonUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Writes recorded notes into the looper's inserted record.
 */
public class LooperRecordWriter {
    public static final String PARTICLE_COLOR_TAG = "ParticleColor",
        PERFORMER_TAG = "Performer", PERFORMERS_TAG = "Performers",
        // The names and particle colors of the performers, in the same order as PERFORMERS_TAG
        PERFORMER_NAMES_TAG = "PerformerNames", PERFORMER_COLORS_TAG = "PerformerColors";

    private final LooperBlockEntity looper;

    public LooperRecordWriter(final LooperBlockEntity looper) {
        this.looper = looper;
    }


    /**
     * Writes a new note to the writable record.
     */
    public void writeNote(NoteSound sound, NoteSoundMetadata soundMeta, int timestamp, int particleRgb, UUID performer) {
        if (!looper.isWritable())
            return;

        RecordNotes.addNote(looper.getChannel(), timestamp, getPerformerIndex(performer),
            soundMeta.pitch(), soundMeta.volume(), particleRgb,
            soundMeta.instrumentId(), sound.baseSoundLocation, sound.index
        );
        looper.setChanged();
    }
    /**
     * Writes a new note to the writable record.
     */
    public void writeHeldNote(HeldNoteSound sound, HeldSoundPhase phase,
                              NoteSoundMetadata soundMeta, int timestamp,
                              int particleRgb, UUID performer) {
        if (!looper.isWritable())
            return;

        RecordNotes.addHeldNote(looper.getChannel(), timestamp, getPerformerIndex(performer),
            soundMeta.pitch(), soundMeta.volume(), particleRgb,
            soundMeta.instrumentId(), sound.baseSoundLocation(), sound.index(),
            phase
        );
        looper.setChanged();
    }

    public void writeDampen(int timestamp, UUID performer) {
        if (!looper.isWritable())
            return;

        RecordNotes.addDampen(looper.getChannel(), timestamp, getPerformerIndex(performer));
        looper.setChanged();
    }

    /**
     * Each player recording on a record is a performer, identified in its notes by their index in the
     * record's performer list. Performers are played back as separate initiators, so that dampening
     * and held notes of one do not affect the others.
     * @return The index of the performer, added to the record if not yet present
     */
    private int getPerformerIndex(final UUID performer) {
        final CompoundTag channel = looper.getChannel();
        if (!channel.contains(PERFORMERS_TAG, Tag.TAG_LIST))
            channel.put(PERFORMERS_TAG, new ListTag());

        final ListTag performers = channel.getList(PERFORMERS_TAG, Tag.TAG_INT_ARRAY);
        for (int i = 0; i < performers.size(); i++) {
            if (NbtUtils.loadUUID(performers.get(i)).equals(performer))
                return i;
        }

        performers.add(NbtUtils.createUUID(performer));
        CommonUtil.getOrCreateListTag(channel, PERFORMER_NAMES_TAG).add(StringTag.valueOf(getPlayerName(performer)));
        CommonUtil.getOrCreateListTag(channel, PERFORMER_COLORS_TAG)
            .add(new IntArrayTag(getPlayerParticleColors(performer)));
        return performers.size() - 1;
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
        RecordNotes.clear(looper.getChannel());
        looper.getChannel().remove(PERFORMERS_TAG);
        looper.getChannel().remove(PERFORMER_NAMES_TAG);
        looper.getChannel().remove(PERFORMER_COLORS_TAG);
        looper.setTicks(0);
        looper.setChanged();
    }

    /**
     * A capped looper is a looper that cannot have any more notes in it, as defined in {@link ModGameRules#RULE_LOOPER_MAX_NOTES}.
     * Any negative will make the looper uncappable.
     * @return Whether this looper is capped
     */
    public boolean isCapped(final Level level) {
        final int cap = level.getGameRules().getInt(ModGameRules.RULE_LOOPER_MAX_NOTES);
        return (cap >= 0) && (RecordNotes.getNotes(looper.getChannel()).size() >= cap);
    }
}
