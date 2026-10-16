package com.stump.songcraft_instruments.networking.packet.instrument.s2c;

import com.stump.songcraft_instruments.networking.IModPacket;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;

import java.util.Optional;

/**
 * A generic S2C packet notifying the client to play
 * a specific note.
 * @param <T> The sound object type
 */
public abstract class S2CNotePacket<T> implements IModPacket {
    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_CLIENT;

    public final Optional<Integer> initiatorID;
    public final Optional<InitiatorID> oInitiatorID;
    public final T sound;
    public final NoteSoundMetadata meta;
    /**
     * The effects pedals the note goes through: those of the instrument that played it. Usually {@link PedalChain#NONE}.
     */
    public final PedalChain pedalChain;

    /**
     * Constructs a packet with an entity initiator.
     */
    public S2CNotePacket(Optional<Integer> initiatorID, T sound, NoteSoundMetadata meta) {
        this(initiatorID, Optional.empty(), sound, meta);
    }

    /**
     * Constructs a packet with an optional entity initiator and
     * an optional custom initiator.
     */
    public S2CNotePacket(Optional<Integer> initiatorID, Optional<InitiatorID> oInitiatorID,
            T sound, NoteSoundMetadata meta) {
        this(initiatorID, oInitiatorID, sound, meta, PedalChain.NONE);
    }
    /**
     * Constructs a packet with an optional entity initiator and
     * an optional custom initiator, played through the given pedals.
     */
    public S2CNotePacket(Optional<Integer> initiatorID, Optional<InitiatorID> oInitiatorID,
            T sound, NoteSoundMetadata meta, PedalChain pedalChain) {
        this.initiatorID = initiatorID;
        this.oInitiatorID = oInitiatorID;
        this.sound = sound;
        this.meta = meta;
        this.pedalChain = pedalChain;
    }

    public S2CNotePacket(FriendlyByteBuf buf) {
        initiatorID = buf.readOptional(FriendlyByteBuf::readInt);
        oInitiatorID = buf.readOptional(InitiatorID::readFromNetwork);
        sound = readSound(buf);
        meta = NoteSoundMetadata.read(buf);
        pedalChain = PedalChain.read(buf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeOptional(initiatorID, FriendlyByteBuf::writeInt);
        buf.writeOptional(oInitiatorID, (buffer, initiatorID) -> initiatorID.writeToNetwork(buffer));
        writeSound(buf);
        meta.write(buf);
        pedalChain.write(buf);
    }

    protected abstract T readSound(FriendlyByteBuf buf);
    protected abstract void writeSound(FriendlyByteBuf buf);
}