package com.stump.songcraft_instruments.networking.packet.instrument.util;

import com.stump.songcraft_instruments.event.NoteSoundPlayedEvent;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.networking.packet.instrument.s2c.S2CNoteSoundPacket;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.sound.NoteSound;
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
 * A helper class for dealing with {@link NoteSound} packets.
 */
public class NoteSoundPacketUtil {
    private static final S2CNotePacketDelegate<NoteSound, S2CNoteSoundPacket> INIT = S2CNoteSoundPacket::new;

    /**
     * Sends {@link S2CNoteSoundPacket}s in the specified {@link InstrumentPacketUtil#PLAY_DISTANCE}.
     * @param initiator The entity producing the sounds
     * @param sound The sound to initiate
     * @param instrumentId The ID of the instrument initiating the sound
     * @param pitch The pitch of the sound to initiate
     * @param volume The volume of the sound to initiate
     */
    public static void sendPlayerPlayNotePackets(Entity initiator,
                                           NoteSound sound, ResourceLocation instrumentId, int pitch, int volume, int particleColor) {
        firePlayerEvent(initiator,
            InstrumentPacketUtil.sendPlayerPlayNotePackets(
                initiator, sound, instrumentId, pitch, volume, particleColor, INIT
            )
        );
    }
    /**
     * Sends {@link S2CNoteSoundPacket}s in the specified {@link InstrumentPacketUtil#PLAY_DISTANCE}.
     * @param initiator The entity producing the sounds
     * @param sound The sound to initiate
     * @param soundMeta Additional metadata of the used sound
     */
    public static void sendPlayerPlayNotePackets(Entity initiator, NoteSound sound, NoteSoundMetadata soundMeta) {
        firePlayerEvent(initiator, InstrumentPacketUtil.sendPlayerPlayNotePackets(
                initiator, sound, soundMeta, INIT
                )
        );
    }

    public static void sendPlayNotePackets(Level level, NoteSound sound,
            NoteSoundMetadata soundMeta, InitiatorID initiatorID) {
        fireGenericEvent(level, InstrumentPacketUtil.sendPlayNotePackets(
                    level, sound, soundMeta, toReg(initiatorID)
                )
        );
    }

    /**
     * Sends a speaker's relay of a note to everyone in {@link InstrumentPacketUtil#PLAY_DISTANCE}
     * except whoever played it, who already hears their own copy where they are
     * @param source Who played the note (a player or looper)
     * @param pedalChain The effects pedals plugged into the speaker
     */
    public static void sendRelayedNotePackets(Level level, NoteSound sound,
            NoteSoundMetadata soundMeta, InitiatorID source, PedalChain pedalChain) {
        final List<Player> listeners = InstrumentPacketUtil.noteListeners(level, soundMeta.pos());
        listeners.removeIf((player) -> InitiatorID.fromEntity(player).equals(source));

        fireGenericEvent(level, InstrumentPacketUtil.sendPlayNotePackets(
                    level, sound, soundMeta, toReg(source, pedalChain), listeners
                )
        );
    }

    /**
     * Sends {@link S2CNoteSoundPacket}s in the specified {@link InstrumentPacketUtil#PLAY_DISTANCE}.
     * This method treats the sound as it was NOT produced by a player.
     * @param level The world that the sound should initiate in
     * @param pos The position of the sound to initiate
     * @param sound The sound to initiate
     * @param instrumentId The ID of the instrument initiating the sound
     * @param pitch The pitch of the sound to initiate
     */
    public static void sendPlayNotePackets(Level level, BlockPos pos, NoteSound sound, ResourceLocation instrumentId,
                                           int pitch, int volume, int particleColor) {
        fireGenericEvent(level,
            InstrumentPacketUtil.sendPlayNotePackets(
                level, pos, sound, instrumentId, pitch, volume, particleColor, INIT
            )
        );
    }
    /**
     * Sends {@link S2CNoteSoundPacket}s in the specified {@link InstrumentPacketUtil#PLAY_DISTANCE}.
     * This method treats the sound as it was NOT produced by a player.
     * @param level The world that the sound should initiate in
     * @param sound The sound to initiate
     * @param soundMeta Additional metadata of the used sound
     */
    public static void sendPlayNotePackets(Level level, NoteSound sound, NoteSoundMetadata soundMeta) {
        fireGenericEvent(level,
            InstrumentPacketUtil.sendPlayNotePackets(
                level, sound, soundMeta, INIT
            )
        );
    }


    private static void firePlayerEvent(Entity initiator, S2CNoteSoundPacket packet) {
        MinecraftForge.EVENT_BUS.post(
            new NoteSoundPlayedEvent(initiator, packet.sound, packet.meta)
        );
    }

    private static void fireGenericEvent(Level level, S2CNoteSoundPacket packet) {
        MinecraftForge.EVENT_BUS.post(
            new NoteSoundPlayedEvent(level, packet.sound, packet.meta)
        );
    }

    private static S2CNotePacketDelegate<NoteSound, S2CNoteSoundPacket> toReg(
            InitiatorID oInitiatorID) {

        return toReg(oInitiatorID, PedalChain.NONE);
    }
    private static S2CNotePacketDelegate<NoteSound, S2CNoteSoundPacket> toReg(
            InitiatorID oInitiatorID, PedalChain pedalChain) {

        return (initiatorID, sound, meta) ->
                new S2CNoteSoundPacket(initiatorID, Optional.of(oInitiatorID), sound, meta, pedalChain);
    }
}
