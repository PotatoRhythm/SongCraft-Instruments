package com.stump.songcraft_instruments.networking.packet.instrument.s2c;

import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.Optional;

/**
 * A S2C packet notifying the client to play
 * a specific {@link HeldNoteSound}.
 */
public class S2CHeldNoteSoundPacket extends S2CNotePacket<HeldNoteSound> {

    public final HeldSoundPhase phase;
    /**
     * An initiator ID for when the initiator is not an entity
     */
    public final Optional<InitiatorID> oInitiatorID;

    /**
     * Constructs a new {@link S2CHeldNoteSoundPacket}.
     * @param initiatorID The UUID of the player initiating the sound.
     *                      May be empty for a non-player trigger.
     */
    public S2CHeldNoteSoundPacket(Optional<Integer> initiatorID, Optional<InitiatorID> oInitiatorID,
                                  HeldNoteSound sound, NoteSoundMetadata meta,
                                  HeldSoundPhase phase) {
        this(initiatorID, oInitiatorID, sound, meta, phase, PedalChain.NONE);
    }
    /**
     * Constructs a new {@link S2CHeldNoteSoundPacket} played through the given pedals.
     * @param initiatorID The UUID of the player initiating the sound.
     *                      May be empty for a non-player trigger.
     */
    public S2CHeldNoteSoundPacket(Optional<Integer> initiatorID, Optional<InitiatorID> oInitiatorID,
                                  HeldNoteSound sound, NoteSoundMetadata meta,
                                  HeldSoundPhase phase, PedalChain pedalChain) {
        super(initiatorID, Optional.empty(), sound, meta, pedalChain);
        this.phase = phase;
        this.oInitiatorID = oInitiatorID;
    }

    public S2CHeldNoteSoundPacket(FriendlyByteBuf buf) {
        super(buf);
        this.phase = buf.readEnum(HeldSoundPhase.class);
        this.oInitiatorID = buf.readOptional(InitiatorID::readFromNetwork);
    }
    @Override
    public void write(FriendlyByteBuf buf) {
        super.write(buf);
        buf.writeEnum(phase);
        buf.writeOptional(oInitiatorID, (fbb, initId) -> initId.writeToNetwork(fbb));
    }

    @Override
    protected void writeSound(FriendlyByteBuf buf) {
        sound.writeToNetwork(buf);
    }
    @Override
    protected HeldNoteSound readSound(FriendlyByteBuf buf) {
        return HeldNoteSound.readFromNetwork(buf);
    }


    @Override
    public void handle(final Context context) {
        sound.playFromServer(initiatorID, oInitiatorID, meta, phase, pedalChain);
    }
}