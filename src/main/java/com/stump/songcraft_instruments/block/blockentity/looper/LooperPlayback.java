package com.stump.songcraft_instruments.block.blockentity.looper;

import com.mojang.logging.LogUtils;
import com.stump.songcraft_instruments.block.blockentity.speaker.Speaker;
import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.networking.packet.instrument.s2c.S2CLooperDampenPacket;
import com.stump.songcraft_instruments.networking.packet.instrument.s2c.S2CLooperParticlePacket;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldNoteSoundPacketUtil;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.networking.packet.instrument.util.NoteSoundPacketUtil;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.pedal.TapeDecks;
import com.stump.songcraft_instruments.recording.Recording;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import com.stump.songcraft_instruments.sound.registrar.HeldNoteSoundRegistrar;
import com.stump.songcraft_instruments.sound.registrar.NoteSoundRegistrar;
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

    /**
     * What plays the notes: a looper block, or a boat or minecart carrying a looper
     */
    public interface Host {
        @Nullable Level getHostLevel();
        /**
         * @return The position notes are played from. May change over time for vehicles
         */
        BlockPos getSoundPos();
        /**
         * @return The speakers relaying everything played. Server only
         */
        List<Speaker> getPairedSpeakers();

        /**
         * @return The packet spawning a note particle above the looper
         */
        default S2CLooperParticlePacket createParticlePacket(final int rgb, final double size) {
            return new S2CLooperParticlePacket(getSoundPos(), rgb, size);
        }

        /**
         * @return The volume everything is played back at, in percent of the recorded volume
         */
        default int getVolume() {
            return LooperVolumeControls.MAX_VOLUME;
        }
    }

    private final Host looper;
    private final InitiatorID looperInitiatorID;
    private int heldParticleTimer = 0;

    /**
     * A set of cached notes as to use them
     * for pausing and resuming the looper.
     */
    private final HashSet<CachedHeldNote> cachedHeldNotes = new HashSet<>();
    private record CachedHeldNote(HeldNoteSound sound, NoteSoundMetadata meta, int particleRgb, InitiatorID initiator,
                                  PedalChain pedalChain) {}

    /**
     * @param initiatorID The initiator the first performer's notes are played back with
     */
    public LooperPlayback(final Host looper, final InitiatorID initiatorID) {
        this.looper = looper;
        this.looperInitiatorID = initiatorID;
    }


    /**
     * @return What tells this looper's tape deck apart from others', see {@link TapeDecks}
     */
    private String tapeDeckId() {
        return looperInitiatorID.type() + ":" + looperInitiatorID.identifier();
    }

    public void stopAndClearHeldSounds() {
        notifyHeldNotesPhase(HeldSoundPhase.RELEASE);
        cachedHeldNotes.clear();
        TapeDecks.onLooperStopped(tapeDeckId());
    }

    public void notifyHeldNotesPhase(final HeldSoundPhase phase) {
        final List<Speaker> speakers = getPairedSpeakers();

        cachedHeldNotes.forEach((note) -> {
            HeldNoteSoundPacketUtil.sendPlayNotePackets(
                looper.getHostLevel(),
                note.sound(), note.meta(),
                phase,
                note.initiator(),
                note.pedalChain()
            );
            speakers.forEach((speaker) ->
                speaker.playHeldNote(note.sound(), note.meta(), phase, note.initiator(), note.pedalChain())
            );
        });
    }

    /**
     * return the speakers paired to this looper, which relay everything it plays
     */
    private List<Speaker> getPairedSpeakers() {
        final Level level = looper.getHostLevel();
        if (level == null || level.isClientSide)
            return List.of();
        return looper.getPairedSpeakers();
    }

    /**
     * @return The initiator a performer's notes are played back with.
     * The first performer uses the looper's own initiator.
     */
    private InitiatorID getPerformerInitiatorID(final int performerIndex) {
        return (performerIndex == 0)
            ? looperInitiatorID
            : new InitiatorID(looperInitiatorID.type(), looperInitiatorID.identifier() + "p" + performerIndex);
    }


    /**
     * @param recording The recording the note is of; the looper's own, or a synced looper's being recorded over
     */
    public void playNote(final Recording recording, final int[] note) {
        try {
            switch (note[Recording.TYPE]) {
                case Recording.TYPE_REGULAR:
                    playNoteSound(recording, note);
                    break;

                case Recording.TYPE_HELD:
                    playHeldSound(recording, note);
                    break;

                case Recording.TYPE_DAMPEN:
                    dampenSounds(getPerformerInitiatorID(note[Recording.PERFORMER]));
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Attempted to play a looper note at {}, but met with an exception", looper.getSoundPos(), e);
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

    private void playNoteSound(final Recording recording, final int[] note) {
        final NoteSoundMetadata meta = metaFromNote(recording, note);
        final ResourceLocation soundLocation = ResourceLocation.tryParse(recording.soundType(note));

        final NoteSound sound = (soundLocation == null) ? null
            : getRecordedSound(NoteSoundRegistrar.getSounds(soundLocation), note);
        // Recorded with a sound type that has since been removed (or not in this mod)
        if (sound == null || meta == null)
            return;

        final InitiatorID initiator = getPerformerInitiatorID(note[Recording.PERFORMER]);
        // Played back through the pedals it was recorded with
        final PedalChain pedalChain = recording.pedalChain(note);

        NoteSoundPacketUtil.sendPlayNotePackets(
                looper.getHostLevel(),
                sound,
                meta,
                initiator,
                pedalChain
        );
        getPairedSpeakers().forEach((speaker) -> speaker.playNote(sound, meta, initiator, pedalChain));
        TapeDecks.onLooperNote(tapeDeckId(), recording.tape(note), looper.getHostLevel(), looper.getSoundPos(),
            getPairedSpeakers(), pedalChain, meta.volume(), null, null);

        triggerEmitNoteParticle(note[Recording.PARTICLE_COLOR]);
    }

    private void playHeldSound(final Recording recording, final int[] note) {
        final NoteSoundMetadata meta = metaFromNote(recording, note);

        final ResourceLocation soundLocation = ResourceLocation.tryParse(recording.soundType(note));
        final HeldNoteSound sound = (soundLocation == null) ? null
            : getRecordedSound(HeldNoteSoundRegistrar.getSounds(soundLocation), note);
        if (sound == null || meta == null)
            return;

        final HeldSoundPhase phase = (note[Recording.HELD_PHASE] == Recording.PHASE_RELEASE)
            ? HeldSoundPhase.RELEASE : HeldSoundPhase.ATTACK;
        final InitiatorID initiator = getPerformerInitiatorID(note[Recording.PERFORMER]);
        // Played back through the pedals it was recorded with
        final PedalChain pedalChain = recording.pedalChain(note);

        HeldNoteSoundPacketUtil.sendPlayNotePackets(
            looper.getHostLevel(), sound,
            meta, phase, initiator, pedalChain
        );
        getPairedSpeakers().forEach((speaker) -> speaker.playHeldNote(sound, meta, phase, initiator, pedalChain));
        // A held note keeps the looper's tape rolling until it's released
        TapeDecks.onLooperNote(tapeDeckId(), recording.tape(note), looper.getHostLevel(), looper.getSoundPos(),
            getPairedSpeakers(), pedalChain, meta.volume(), phase, List.of(sound, meta.pitch(), initiator));

        if (phase == HeldSoundPhase.ATTACK) {
            final int rgb = note[Recording.PARTICLE_COLOR];

            cachedHeldNotes.add(new CachedHeldNote(sound, meta, rgb, initiator, pedalChain));
            triggerEmitNoteParticle(rgb);

        } else if (phase == HeldSoundPhase.RELEASE) {
            // The looper may have moved since the note started (if carried by a vehicle), and its volume changed,
            // so ignore both
            cachedHeldNotes.removeIf((cached) ->
                    cached.sound().equals(sound) &&
                            cached.meta().equals(atPosAndVolume(meta, cached.meta().pos(), cached.meta().volume())) &&
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
                new S2CLooperDampenPacket(initiator), (ServerLevel) looper.getHostLevel(), looper.getSoundPos()
        );
    }

    /**
     * @return The note's metadata, or null if its instrument ID is malformed
     */
    private @Nullable NoteSoundMetadata metaFromNote(final Recording recording, final int[] note) {
        final ResourceLocation instrumentId = ResourceLocation.tryParse(recording.instrumentId(note));
        if (instrumentId == null)
            return null;

        return new NoteSoundMetadata(
            looper.getSoundPos(),
            note[Recording.PITCH],
            note[Recording.VOLUME] * looper.getVolume() / LooperVolumeControls.MAX_VOLUME,
            note[Recording.PARTICLE_COLOR],
            instrumentId, Optional.empty()
        );
    }
    private static NoteSoundMetadata atPosAndVolume(final NoteSoundMetadata meta, final BlockPos pos, final int volume) {
        return new NoteSoundMetadata(pos, meta.pitch(), volume, meta.particleColor(), meta.instrumentId(), meta.noteIdentifier());
    }

    public void triggerEmitNoteParticle(int rgb) {

        double size = 0.2;

        SCPacketHandler.sendToTracking(
                looper.createParticlePacket(rgb, size),
                (ServerLevel) looper.getHostLevel(),
                looper.getSoundPos()
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
