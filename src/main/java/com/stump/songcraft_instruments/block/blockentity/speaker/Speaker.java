package com.stump.songcraft_instruments.block.blockentity.speaker;

import com.stump.songcraft_instruments.networking.SCPacketHandler;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.networking.packet.instrument.s2c.S2CLooperParticlePacket;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldNoteSoundPacketUtil;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.networking.packet.instrument.util.NoteSoundPacketUtil;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import com.stump.songcraft_instruments.util.SpeakerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * immediately rebroadcasts the notes of its paired instruments from its host's position.
 * the host is a speaker block, or a boat or minecart carrying one.
 * notes are relayed through the effects pedals of the instrument that played them (see {@link PedalChain}).
 */
public class Speaker {
    /**
     * where a speaker lives: a speaker block, or a boat or minecart carrying one
     */
    public interface Host {
        @Nullable Level getHostLevel();
        /**
         * return the position notes are relayed from. may change over time for vehicles
         */
        BlockPos getSoundPos();
        /**
         * return how instruments find this speaker again
         */
        SpeakerAddress getAddress();
        void markHostChanged();

        /**
         * return whether the speaker shows as connected (the green front light)
         */
        boolean isConnected();
        void setConnected(boolean connected);

        /**
         * return the packet spawning a note particle above this speaker
         */
        default S2CLooperParticlePacket createParticlePacket(final int rgb, final double size) {
            return new S2CLooperParticlePacket(getSoundPos(), rgb, size);
        }
    }

    private static final double PARTICLE_SIZE = 0.2;
    private static final String SPEAKER_ID_TAG = "speaker_id", INSTRUMENTS_TAG = "instruments",
        INSTRUMENT_ID_TAG = "id", INSTRUMENT_POS_TAG = "pos", LOOPER_ID_TAG = "looper_id";
    /**
     * how often (in ticks) paired block instruments are checked for still being paired
     */
    private static final int VALIDATE_INTERVAL = 40;
    /**
     * how often (in ticks) held notes re-emit their particle while sustained, same as the looper
     */
    private static final int HELD_PARTICLE_INTERVAL = 10;
    private int heldParticleTimer = 0;

    private final Host host;
    private final InitiatorID speakerInitiatorID;

    /**
     * held notes currently being sustained through this speaker, mapped to the (relocated) metadata they started with.
     * keyed by sound, pitch and source only: a release's volume or particle color can differ from its attack's
     * (e.g. closing the instrument reports the fading volume), which must not stop the release from matching.
     * the source is who played it (a player or looper), so their notes can all be released at once, e.g. on dampen.
     */
    private record HeldNoteKey(HeldNoteSound sound, int pitch, InitiatorID source) {}
    private final HashMap<HeldNoteKey, NoteSoundMetadata> sustainedNotes = new HashMap<>();

    /**
     * identifies this specific speaker, so an instrument can tell it apart from
     * a new speaker later placed at the same position.
     */
    private UUID speakerId = UUID.randomUUID();

    /**
     * the instruments paired to this speaker, by instrument ID. block instruments map to their position
     * so they can be re-checked; held instruments map to null.
     * the speaker shows as connected exactly while this is non-empty.
     */
    private final HashMap<UUID, BlockPos> pairedInstruments = new HashMap<>();
    /**
     * the ID of the looper paired to this speaker, if any. a speaker accepts at most one looper.
     * the looper is also in pairedInstruments, so it counts towards the connected state.
     */
    @Nullable
    private UUID pairedLooperId = null;

    /**
     * @param initiatorID The initiator held notes relayed through this speaker are played with
     */
    public Speaker(final Host host, final InitiatorID initiatorID) {
        this.host = host;
        this.speakerInitiatorID = initiatorID;
    }

    /**
     * return The same metadata, but originating from this speaker's position.
     */
    private NoteSoundMetadata relocate(final NoteSoundMetadata meta) {
        return new NoteSoundMetadata(host.getSoundPos(), meta.pitch(), meta.volume(), meta.particleColor(), meta.instrumentId(), meta.noteIdentifier());
    }

    /**
     * @param pedalChain The effects pedals of the instrument that played the note
     */
    public void playNote(final NoteSound sound, final NoteSoundMetadata meta, final InitiatorID source,
                         final PedalChain pedalChain) {
        final NoteSoundMetadata relocated = relocate(meta);

        NoteSoundPacketUtil.sendRelayedNotePackets(host.getHostLevel(), sound, relocated, source, pedalChain);
        emitNoteParticle(relocated.particleColor());
    }

    /**
     * @param source Who played the note (a player or looper), see {@link #releaseHeldNotesFrom}
     * @param pedalChain The effects pedals of the instrument that played the note
     */
    public void playHeldNote(final HeldNoteSound sound, final NoteSoundMetadata meta, final HeldSoundPhase phase,
                             final InitiatorID source, final PedalChain pedalChain) {
        final NoteSoundMetadata relocated = relocate(meta);
        final HeldNoteKey key = new HeldNoteKey(sound, relocated.pitch(), source);

        if (phase == HeldSoundPhase.ATTACK) {
            HeldNoteSoundPacketUtil.sendRelayedNotePackets(host.getHostLevel(), sound, relocated, phase, speakerInitiatorID, source,
                pedalChain);
            sustainedNotes.put(key, relocated);
            emitNoteParticle(relocated.particleColor());
        } else if (phase == HeldSoundPhase.RELEASE) {
            // Release with the metadata the note started with, so clients match it to the right sound
            final NoteSoundMetadata attackMeta = sustainedNotes.remove(key);
            HeldNoteSoundPacketUtil.sendRelayedNotePackets(host.getHostLevel(), sound,
                (attackMeta != null) ? attackMeta : relocated, phase, speakerInitiatorID, source, PedalChain.NONE);
        }
    }

    /**
     * releases every held note the given source (a player or looper) is sustaining through this speaker.
     * used when the source stops all its notes at once without releasing each one, e.g. on dampen.
     */
    public void releaseHeldNotesFrom(final InitiatorID source) {
        sustainedNotes.entrySet().removeIf((entry) -> {
            if (!entry.getKey().source().equals(source))
                return false;

            HeldNoteSoundPacketUtil.sendRelayedNotePackets(
                host.getHostLevel(), entry.getKey().sound(), entry.getValue(), HeldSoundPhase.RELEASE, speakerInitiatorID,
                source, PedalChain.NONE
            );
            return true;
        });
    }

    //#region Pairing

    public UUID getSpeakerId() {
        return speakerId;
    }
    public SpeakerAddress getAddress() {
        return host.getAddress();
    }

    public boolean hasInstrument(final UUID instrumentId) {
        return pairedInstruments.containsKey(instrumentId);
    }
    /**
     * return whether a looper other than the given one is paired to this speaker.
     * re-checks block pairings first, so a broken or unpaired looper doesn't block a new one.
     */
    public boolean hasOtherLooper(final Level level, final UUID looperId) {
        validateBlockInstruments(level);
        return (pairedLooperId != null) && !pairedLooperId.equals(looperId);
    }
    /**
     * instrumentPos - The block instrument's or looper's position, or null for a held instrument
     * isLooper - Whether the paired source is a looper
     */
    public void addInstrument(final UUID instrumentId, @Nullable final BlockPos instrumentPos, final boolean isLooper) {
        pairedInstruments.put(instrumentId, instrumentPos);
        if (isLooper)
            pairedLooperId = instrumentId;
        onPairingsChanged();
    }
    public void removeInstrument(final UUID instrumentId) {
        // containsKey, since held instruments map to a null position
        if (!pairedInstruments.containsKey(instrumentId))
            return;

        pairedInstruments.remove(instrumentId);
        if (instrumentId.equals(pairedLooperId))
            pairedLooperId = null;
        onPairingsChanged();
    }

    private void onPairingsChanged() {
        host.markHostChanged();
        updateConnectedState();
    }
    /**
     * swaps da front texture between speaker_front and speaker_front_connected,
     * according to whether any instrument or looper is properly paired
     */
    private void updateConnectedState() {
        final Level level = host.getHostLevel();
        if (level == null || level.isClientSide)
            return;

        final boolean connected = hasConfirmedPairing(level);
        if (host.isConnected() != connected)
            host.setConnected(connected);
    }
    /**
     * @return Whether the speaker currently shows as connected (the green front light)
     */
    public boolean isConnected() {
        return host.isConnected();
    }
    /**
     * @return A copy of this speaker's pairings: instrument/looper ID to its position (null for held instruments)
     */
    public Map<UUID, BlockPos> getPairedSources() {
        return new HashMap<>(pairedInstruments);
    }

    /**
     * block instruments and loopers are checked (and dropped if stale) by validateBlockInstruments,
     * so they count as-is. held instruments only count while the item is actually in an online player's
     * inventory, since a lost or deleted item can't tell the speaker it's gone.
     */
    private boolean hasConfirmedPairing(final Level level) {
        for (final var entry : pairedInstruments.entrySet()) {
            if (entry.getValue() != null || SpeakerUtil.isHeldInstrumentPresent(level, entry.getKey(), this))
                return true;
        }
        return false;
    }

    /**
     * called when a speaker block is placed. a new speaker never starts paired, even if the item
     * carried a copied speaker's data (e.g. creative pick-block with ctrl).
     */
    public void resetPairings() {
        speakerId = UUID.randomUUID();
        pairedInstruments.clear();
        pairedLooperId = null;
        onPairingsChanged();
    }

    public void tick(final Level level) {
        emitHeldParticles();

        if (level.getGameTime() % VALIDATE_INTERVAL == 0)
            validateBlockInstruments(level);
    }
    /**
     * drops block instruments that were broken or no longer list this speaker.
     * also corrects the connected state, e.g. for speakers saved by an older version.
     */
    private void validateBlockInstruments(final Level level) {
        final boolean changed = pairedInstruments.entrySet().removeIf((entry) -> {
            final BlockPos instrumentPos = entry.getValue();
            return (instrumentPos != null)
                && level.isLoaded(instrumentPos)
                && !SpeakerUtil.isBlockInstrumentPaired(level, instrumentPos, entry.getKey(), this);
        });
        if ((pairedLooperId != null) && !pairedInstruments.containsKey(pairedLooperId))
            pairedLooperId = null;

        if (changed)
            host.markHostChanged();
        updateConnectedState();
    }

    public void save(final CompoundTag pTag) {
        pTag.putUUID(SPEAKER_ID_TAG, speakerId);

        final ListTag instruments = new ListTag();
        pairedInstruments.forEach((instrumentId, instrumentPos) -> {
            final CompoundTag instrumentTag = new CompoundTag();
            instrumentTag.putUUID(INSTRUMENT_ID_TAG, instrumentId);
            if (instrumentPos != null)
                instrumentTag.put(INSTRUMENT_POS_TAG, NbtUtils.writeBlockPos(instrumentPos));
            instruments.add(instrumentTag);
        });
        pTag.put(INSTRUMENTS_TAG, instruments);

        if (pairedLooperId != null)
            pTag.putUUID(LOOPER_ID_TAG, pairedLooperId);
    }
    public void load(final CompoundTag pTag) {
        if (pTag.hasUUID(SPEAKER_ID_TAG))
            speakerId = pTag.getUUID(SPEAKER_ID_TAG);

        pairedInstruments.clear();
        final ListTag instruments = pTag.getList(INSTRUMENTS_TAG, CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < instruments.size(); i++) {
            final CompoundTag instrumentTag = instruments.getCompound(i);
            if (!instrumentTag.hasUUID(INSTRUMENT_ID_TAG))
                continue;

            pairedInstruments.put(instrumentTag.getUUID(INSTRUMENT_ID_TAG),
                instrumentTag.contains(INSTRUMENT_POS_TAG, CompoundTag.TAG_COMPOUND)
                    ? NbtUtils.readBlockPos(instrumentTag.getCompound(INSTRUMENT_POS_TAG))
                    : null
            );
        }

        pairedLooperId = pTag.hasUUID(LOOPER_ID_TAG) ? pTag.getUUID(LOOPER_ID_TAG) : null;
        if ((pairedLooperId != null) && !pairedInstruments.containsKey(pairedLooperId))
            pairedLooperId = null;
    }

    //#endregion

    private void emitNoteParticle(final int rgb) {
        SCPacketHandler.sendToTracking(
            host.createParticlePacket(rgb, PARTICLE_SIZE),
            (ServerLevel) host.getHostLevel(),
            host.getSoundPos()
        );
    }

    /**
     * keeps emitting particles while held notes (e.g. the flute's) are sustained,
     * as the looper does in LooperBlockEntity
     */
    private void emitHeldParticles() {
        if (sustainedNotes.isEmpty()) {
            heldParticleTimer = 0;
            return;
        }
        if (++heldParticleTimer < HELD_PARTICLE_INTERVAL)
            return;

        heldParticleTimer = 0;
        sustainedNotes.values().forEach((meta) -> emitNoteParticle(meta.particleColor()));
    }

    /**
     * releases every held note sustained through this speaker, e.g. when it is removed or unloaded
     */
    public void releaseSustainedNotes() {
        final Level level = host.getHostLevel();
        if (level == null || level.isClientSide) {
            sustainedNotes.clear();
            return;
        }

        sustainedNotes.forEach((key, meta) ->
            HeldNoteSoundPacketUtil.sendPlayNotePackets(
                level, key.sound(), meta, HeldSoundPhase.RELEASE, speakerInitiatorID
            )
        );
        sustainedNotes.clear();
    }
}
