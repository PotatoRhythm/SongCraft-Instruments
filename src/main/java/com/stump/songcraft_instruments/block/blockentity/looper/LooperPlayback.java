package com.stump.songcraft_instruments.block.blockentity.looper;

import com.mojang.logging.LogUtils;
import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.SpeakerBlockEntity;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.networking.packet.instrument.s2c.S2CLooperDampenPacket;
import com.stump.songcraft_instruments.networking.packet.instrument.s2c.S2CLooperParticlePacket;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldNoteSoundPacketUtil;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.networking.packet.instrument.util.NoteSoundPacketUtil;
import com.stump.songcraft_instruments.recording.Recording;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import com.stump.songcraft_instruments.sound.registrar.HeldNoteSoundRegistrar;
import com.stump.songcraft_instruments.sound.registrar.NoteSoundRegistrar;
import com.stump.songcraft_instruments.util.SpeakerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Plays back the notes of the looper's inserted record, to nearby players and paired speakers.
 */
public class LooperPlayback {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final LooperBlockEntity looper;
    private final InitiatorID looperInitiatorID;
    private int heldParticleTimer = 0;

    /**
     * A set of cached notes as to use them
     * for pausing and resuming the looper.
     */
    private final HashSet<CachedHeldNote> cachedHeldNotes = new HashSet<>();
    private record CachedHeldNote(HeldNoteSound sound, NoteSoundMetadata meta, int particleRgb, InitiatorID initiator) {}

    public LooperPlayback(final LooperBlockEntity looper, final BlockPos pos) {
        this.looper = looper;
        this.looperInitiatorID = new InitiatorID("block",
            String.format("x%sy%sz%s", pos.getX(), pos.getY(), pos.getZ())
        );
    }


    public void stopAndClearHeldSounds() {
        notifyHeldNotesPhase(HeldSoundPhase.RELEASE);
        cachedHeldNotes.clear();
    }

    public void notifyHeldNotesPhase(final HeldSoundPhase phase) {
        final List<SpeakerBlockEntity> speakers = getPairedSpeakers();

        cachedHeldNotes.forEach((note) -> {
            HeldNoteSoundPacketUtil.sendPlayNotePackets(
                looper.getLevel(),
                note.sound(), note.meta(),
                phase,
                note.initiator()
            );
            speakers.forEach((speaker) -> speaker.playHeldNote(note.sound(), note.meta(), phase, note.initiator()));
        });
    }

    /**
     * return the speakers paired to this looper, which relay everything it plays
     */
    private List<SpeakerBlockEntity> getPairedSpeakers() {
        final Level level = looper.getLevel();
        if (level == null || level.isClientSide)
            return List.of();
        return SpeakerUtil.getFromBlock(level, looper);
    }

    /**
     * @return The initiator a performer's notes are played back with.
     * The first performer (and solo recordings) use the looper's own initiator.
     */
    private InitiatorID getPerformerInitiatorID(final int performerIndex) {
        return (performerIndex == 0)
            ? looperInitiatorID
            : new InitiatorID(looperInitiatorID.type(), looperInitiatorID.identifier() + "p" + performerIndex);
    }


    /**
     * @param note A note of the looper's {@link LooperBlockEntity#getRecording recording}
     */
    public void playNote(final int[] note) {
        try {
            switch (note[Recording.TYPE]) {
                case Recording.TYPE_REGULAR:
                    playNoteSound(note);
                    break;

                case Recording.TYPE_HELD:
                    playHeldSound(note);
                    break;

                case Recording.TYPE_DAMPEN:
                    dampenSounds(getPerformerInitiatorID(note[Recording.PERFORMER]));
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Attempted to play a looper note at {}, but met with an exception", looper.getBlockPos(), e);
        }
    }

    /**
     * @param sounds The registered sounds of the note's sound type, null if it is not (or no longer) registered
     * @return The note's sound, or null if its sound type or index no longer exists
     */
    private static <T> @Nullable T getRecordedSound(final @Nullable T[] sounds, final int[] note) {
        final int index = note[Recording.SOUND_INDEX];
        return ((sounds == null) || (index < 0) || (index >= sounds.length)) ? null : sounds[index];
    }

    private void playNoteSound(final int[] note) {
        final NoteSoundMetadata meta = metaFromNote(note);
        final ResourceLocation soundLocation = ResourceLocation.tryParse(looper.getRecording().soundType(note));

        final NoteSound sound = (soundLocation == null) ? null
            : getRecordedSound(NoteSoundRegistrar.getSounds(soundLocation), note);
        // Recorded with a sound type that has since been removed (or not in this mod)
        if (sound == null || meta == null)
            return;

        final InitiatorID initiator = getPerformerInitiatorID(note[Recording.PERFORMER]);

        NoteSoundPacketUtil.sendPlayNotePackets(
                looper.getLevel(),
                sound,
                meta,
                initiator
        );
        getPairedSpeakers().forEach((speaker) -> speaker.playNote(sound, meta, initiator));

        triggerEmitNoteParticle(note[Recording.PARTICLE_COLOR]);
    }

    private void playHeldSound(final int[] note) {
        final NoteSoundMetadata meta = metaFromNote(note);

        final ResourceLocation soundLocation = ResourceLocation.tryParse(looper.getRecording().soundType(note));
        final HeldNoteSound sound = (soundLocation == null) ? null
            : getRecordedSound(HeldNoteSoundRegistrar.getSounds(soundLocation), note);
        if (sound == null || meta == null)
            return;

        final HeldSoundPhase phase = (note[Recording.HELD_PHASE] == Recording.PHASE_RELEASE)
            ? HeldSoundPhase.RELEASE : HeldSoundPhase.ATTACK;
        final InitiatorID initiator = getPerformerInitiatorID(note[Recording.PERFORMER]);

        HeldNoteSoundPacketUtil.sendPlayNotePackets(
            looper.getLevel(), sound,
            meta, phase, initiator
        );
        getPairedSpeakers().forEach((speaker) -> speaker.playHeldNote(sound, meta, phase, initiator));

        if (phase == HeldSoundPhase.ATTACK) {
            final int rgb = note[Recording.PARTICLE_COLOR];

            cachedHeldNotes.add(new CachedHeldNote(sound, meta, rgb, initiator));
            triggerEmitNoteParticle(rgb);

        } else if (phase == HeldSoundPhase.RELEASE) {
            cachedHeldNotes.removeIf((cached) ->
                    cached.sound().equals(sound) &&
                            cached.meta().equals(meta) &&
                            cached.initiator().equals(initiator)
            );
        }
    }

    /**
     * Dampens the sounds of a single performer
     */
    private void dampenSounds(final InitiatorID initiator) {
        // Speakers don't get the dampen packet, so release their copies of the held notes
        getPairedSpeakers().forEach((speaker) -> speaker.releaseHeldNotesFrom(initiator));
        cachedHeldNotes.removeIf((note) -> note.initiator().equals(initiator));

        SCPacketHandler.sendToTracking(
                new S2CLooperDampenPacket(initiator), (ServerLevel) looper.getLevel(), looper.getBlockPos()
        );
    }

    /**
     * @return The note's metadata, or null if its instrument ID is malformed
     */
    private @Nullable NoteSoundMetadata metaFromNote(final int[] note) {
        final ResourceLocation instrumentId = ResourceLocation.tryParse(looper.getRecording().instrumentId(note));
        if (instrumentId == null)
            return null;

        return new NoteSoundMetadata(
            looper.getBlockPos(),
            note[Recording.PITCH],
            note[Recording.VOLUME],
            note[Recording.PARTICLE_COLOR],
            instrumentId, Optional.empty()
        );
    }

    public void triggerEmitNoteParticle(int rgb) {

        double size = 0.2;

        SCPacketHandler.sendToTracking(
                new S2CLooperParticlePacket(
                        looper.getBlockPos(),
                        rgb,
                        size
                ),
                (ServerLevel) looper.getLevel(),
                looper.getBlockPos()
        );
    }

    public void emitHeldParticles() {
        if (cachedHeldNotes.isEmpty())
            return;
        if (++heldParticleTimer < 10)
            return;

        heldParticleTimer = 0;

        for (CachedHeldNote heldNote : cachedHeldNotes)
        {
            triggerEmitNoteParticle(
                    heldNote.particleRgb()
            );
        }
    }
}
