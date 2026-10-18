package com.stump.songcraft_instruments.networking.packet;

import com.stump.songcraft_instruments.client.pedal.TapeDeckPlayer;
import com.stump.songcraft_instruments.networking.IModPacket;
import com.stump.songcraft_instruments.pedal.PedalEffect;
import com.stump.songcraft_instruments.pedal.PedalEffectType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tells a player about a Lo-fi tape deck near them: an instrument (or looper) whose tape hiss rolls while it plays.
 * See {@code TapeDecks} on the server, and {@link TapeDeckPlayer} on the client.
 */
public class S2CTapeDeckPacket implements IModPacket {
    public static final NetworkDirection NETWORK_DIRECTION = NetworkDirection.PLAY_TO_CLIENT;

    /**
     * The most places a deck's hiss plays from, as many as an instrument and its speakers
     */
    private static final int MAX_EMITTERS = 16;

    public enum Action {
        /**
         * The deck started rolling: its play key clunks and its motor spins up
         */
        START,
        /**
         * The deck was already rolling when the player came within range: its hiss just fades in
         */
        JOIN,
        /**
         * The deck's settings, volume or places changed
         */
        UPDATE,
        /**
         * The deck stopped: its stop key clunks and its motor winds down
         */
        STOP,
        /**
         * The player went out of the deck's range: its hiss just fades out
         */
        LEAVE
    }

    /**
     * A place the deck's hiss plays from: an entity it follows (a player holding the instrument, or a vehicle
     * carrying a speaker or looper), or a block
     * @param entityId The entity's network ID, or -1 for a block
     */
    public record Emitter(int entityId, @Nullable BlockPos pos) {
        public static Emitter ofEntity(final int entityId) {
            return new Emitter(entityId, null);
        }
        public static Emitter ofBlock(final BlockPos pos) {
            return new Emitter(-1, pos.immutable());
        }
    }

    public final String deckId;
    public final Action action;
    /**
     * The deck's Lo-fi effect, whose Age sets how loud its hiss is
     */
    public final PedalEffect effect;
    /**
     * The volume the deck plays at, from 0 to 1: that of its last note
     */
    public final float volume;
    public final List<Emitter> emitters;

    public S2CTapeDeckPacket(final String deckId, final Action action, final PedalEffect effect, final float volume,
                             final List<Emitter> emitters) {
        this.deckId = deckId;
        this.action = action;
        this.effect = effect;
        this.volume = volume;
        this.emitters = List.copyOf(emitters);
    }
    public S2CTapeDeckPacket(final FriendlyByteBuf buf) {
        deckId = buf.readUtf(256);
        action = buf.readEnum(Action.class);
        effect = new PedalEffect(PedalEffectType.LOFI, buf.readInt());
        volume = buf.readFloat();

        final int count = buf.readVarInt();
        final List<Emitter> read = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final Emitter emitter = buf.readBoolean() ? Emitter.ofEntity(buf.readVarInt()) : Emitter.ofBlock(buf.readBlockPos());
            if (read.size() < MAX_EMITTERS)
                read.add(emitter);
        }
        emitters = read;
    }

    @Override
    public void write(final FriendlyByteBuf buf) {
        buf.writeUtf(deckId, 256);
        buf.writeEnum(action);
        buf.writeInt(effect.knobs());
        buf.writeFloat(volume);

        buf.writeVarInt(emitters.size());
        for (final Emitter emitter : emitters) {
            buf.writeBoolean(emitter.pos() == null);
            if (emitter.pos() == null)
                buf.writeVarInt(emitter.entityId());
            else
                buf.writeBlockPos(emitter.pos());
        }
    }

    @Override
    public void handle(final Context context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TapeDeckPlayer.handle(this));
    }
}
