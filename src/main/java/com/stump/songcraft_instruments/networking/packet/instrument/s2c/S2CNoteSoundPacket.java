package com.stump.songcraft_instruments.networking.packet.instrument.s2c;

import com.stump.songcraft_instruments.sound.held.InitiatorID;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.pedal.PedalChain;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.Optional;

/**
 * A S2C packet notifying the client to play
 * a specific {@link NoteSound}.
 */
public class S2CNoteSoundPacket extends S2CNotePacket<NoteSound> {

    public S2CNoteSoundPacket(
            Optional<Integer> initiatorID,
            NoteSound sound,
            NoteSoundMetadata meta
    ) {
        super(initiatorID, sound, meta);
    }

    public S2CNoteSoundPacket(
            Optional<Integer> initiatorID,
            Optional<InitiatorID> oInitiatorID,
            NoteSound sound,
            NoteSoundMetadata meta
    ) {
        super(initiatorID, oInitiatorID, sound, meta);
    }

    public S2CNoteSoundPacket(
            Optional<Integer> initiatorID,
            Optional<InitiatorID> oInitiatorID,
            NoteSound sound,
            NoteSoundMetadata meta,
            PedalChain pedalChain
    ) {
        super(initiatorID, oInitiatorID, sound, meta, pedalChain);
    }

    public S2CNoteSoundPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    protected void writeSound(FriendlyByteBuf buf) {
        sound.writeToNetwork(buf);
    }

    @Override
    protected NoteSound readSound(FriendlyByteBuf buf) {
        return NoteSound.readFromNetwork(buf);
    }

    @Override
    public void handle(final Context context) {
        sound.playFromServer(initiatorID, oInitiatorID, meta, pedalChain);
    }
}