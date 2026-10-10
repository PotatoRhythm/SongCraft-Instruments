package com.stump.songcraft_instruments.networking.packet.instrument.util;

import com.stump.songcraft_instruments.event.HeldNoteSoundPlayedEvent;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.networking.packet.instrument.s2c.S2CHeldNoteSoundPacket;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Optional;

/**
 * A helper class for dealing with {@link HeldNoteSound} packets.
 */
public class HeldNoteSoundPacketUtil {

    /**
     * Sends {@link S2CHeldNoteSoundPacket}s in the specified {@link InstrumentPacketUtil#PLAY_DISTANCE}.
     * @param initiator The player producing the sounds
     * @param sound The sound to initiate
     * @param instrumentId The ID of the instrument initiating the sound
     * @param pitch The pitch of the sound to initiate
     * @param volume The volume of the sound to initiate
     * @param phase The phase for the packet to report
     */
    public static void sendPlayerPlayNotePackets(Entity initiator, HeldNoteSound sound,
                                                 ResourceLocation instrumentId, int pitch, int volume, int particleColor,
                                                 HeldSoundPhase phase) {
        sendPlayerPlayNotePackets(
            initiator, sound,
            new NoteSoundMetadata(
                initiator.blockPosition(),
                pitch, volume, particleColor,
                instrumentId,
                Optional.empty()
            ),
            phase
        );
    }
    /**
     * Sends {@link S2CHeldNoteSoundPacket}s in the specified {@link InstrumentPacketUtil#PLAY_DISTANCE}.
     * @param initiator The player producing the sounds
     * @param sound The sound to initiate
     * @param soundMeta Additional metadata of the used sound
     * @param phase The phase for the packet to report
     */
    public static void sendPlayerPlayNotePackets(Entity initiator, HeldNoteSound sound, NoteSoundMetadata soundMeta,
                                                 HeldSoundPhase phase) {
        fireEntityEvent(initiator,
            InstrumentPacketUtil.sendPlayerPlayNotePackets(
                initiator, sound, soundMeta, toReg(phase, initiator),
                listeners(initiator.level(), soundMeta.pos(), phase)
            )
        );
    }

    /**
     * Sends {@link S2CHeldNoteSoundPacket}s in the specified {@link InstrumentPacketUtil#PLAY_DISTANCE}.
     * This method treats the sound as it was NOT produced by a player.
     * @param level The world that the sound should initiate in
     * @param pos The position of the sound to initiate
     * @param sound The sound to initiate
     * @param instrumentId The ID of the instrument initiating the sound
     * @param pitch The pitch of the sound to initiate
     * @param phase The phase for the packet to report
     */
    public static void sendPlayNotePackets(Level level, BlockPos pos, HeldNoteSound sound, ResourceLocation instrumentId,
                                           int pitch, int volume, int particleColor, HeldSoundPhase phase,
                                           InitiatorID initiatorID) {
        sendPlayNotePackets(
            level, sound,
            new NoteSoundMetadata(
                pos,
                pitch, volume,
                particleColor,
                instrumentId,
                Optional.empty()
            ),
            phase, initiatorID
        );
    }
    /**
     * Sends {@link S2CHeldNoteSoundPacket}s in the specified {@link InstrumentPacketUtil#PLAY_DISTANCE}.
     * This method treats the sound as it was NOT produced by a player.
     * @param level The world that the sound should initiate in
     * @param sound The sound to initiate
     * @param soundMeta Additional metadata of the used sound
     * @param phase The phase for the packet to report
     */
    public static void sendPlayNotePackets(Level level, HeldNoteSound sound, NoteSoundMetadata soundMeta,
                                           HeldSoundPhase phase,
                                           InitiatorID initiatorID) {
        fireGenericEvent(level,
            InstrumentPacketUtil.sendPlayNotePackets(
                level, sound, soundMeta, toReg(initiatorID, phase),
                listeners(level, soundMeta.pos(), phase)
            )
        );
    }

    /**
     * Held notes are started within {@link InstrumentPacketUtil#PLAY_DISTANCE}, but released for everyone in the level:
     * a player who heard a note start may have since moved out of range, and would otherwise keep holding it.
     * Those who don't hold the note ignore its release.
     */
    private static List<? extends Player> listeners(Level level, BlockPos pos, HeldSoundPhase phase) {
        return (phase == HeldSoundPhase.RELEASE)
            ? level.players()
            : InstrumentPacketUtil.noteListeners(level, pos);
    }


    private static void fireEntityEvent(Entity initiator, S2CHeldNoteSoundPacket packet) {
        MinecraftForge.EVENT_BUS.post(
            new HeldNoteSoundPlayedEvent(
                initiator,
                packet.sound, packet.meta,
                packet.phase,
                InitiatorID.fromEntity(initiator)
            )
        );
    }
    private static void fireGenericEvent(Level level, S2CHeldNoteSoundPacket packet) {
        MinecraftForge.EVENT_BUS.post(
            new HeldNoteSoundPlayedEvent(
                level,
                packet.sound, packet.meta,
                packet.phase,
                InitiatorID.getEither(packet.initiatorID, packet.oInitiatorID)
            )
        );
    }


    // Converting to the base instrument packet lambda

    private static S2CNotePacketDelegate<HeldNoteSound, S2CHeldNoteSoundPacket> toReg(InitiatorID oInitiatorID, HeldSoundPhase phase) {
        return (initiatorID, sound, meta) -> new S2CHeldNoteSoundPacket(
            initiatorID, Optional.of(oInitiatorID),
            sound, meta, phase
        );
    }
    private static S2CNotePacketDelegate<HeldNoteSound, S2CHeldNoteSoundPacket> toReg(HeldSoundPhase phase, Entity initiator) {
        return (initiatorID, sound, meta) -> new S2CHeldNoteSoundPacket(
            initiatorID,
            initiatorID.isPresent() ? Optional.of(InitiatorID.fromEntity(initiator)) : Optional.empty(),
            sound, meta, phase
        );
    }

}
