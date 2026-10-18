package com.stump.songcraft_instruments.sound.held;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.pedal.PedalMonitor;
import com.stump.songcraft_instruments.event.HeldNoteSoundPlayedEvent;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.pedal.PedalChain;
import com.stump.songcraft_instruments.sound.InstrumentSoundSet;
import com.stump.songcraft_instruments.sound.NearestCopyFilter;
import com.stump.songcraft_instruments.sound.NoteSound;
import com.stump.songcraft_instruments.sound.registrar.HeldNoteSoundRegistrar;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

/**
 * A container for {@link NoteSound}s that act together
 * as a holdable note sound.
 * <p>The attack sound plays once at the beginning, and the hold
 * sound is held and repeated until released.</p>
 */
public record HeldNoteSound (
    ResourceLocation baseSoundLocation, int index,
    NoteSound attack,
    NoteSound hold,
    @Nullable NoteSound release,

    float holdDuration,
    float holdDelay,
    float chainedHoldDelay,
    float decay,
    float releaseFadeOut,
    float fullHoldFadeoutTime
) implements InstrumentSoundSet {

    public NoteSound getSound(final Phase phase) {
        return switch (phase) {
            case HOLD -> hold;
            case ATTACK -> attack;
            case RELEASE -> release;
        };
    }

    /**
     * @param pitch The pitch the note plays at
     * @return How many ticks apart its hold sounds start: each starts once the previous one has played
     * {@code holdDuration + chainedHoldDelay} seconds of its sound, which a higher pitch plays through faster
     */
    public int holdPeriodTicks(final float pitch) {
        final int threshold = (int) ((holdDuration + chainedHoldDelay) * 20);
        if (threshold <= 0 || pitch <= 0)
            return 0;

        // The first tick at which (ticks * pitch) reaches it, in the same float math as it's checked in
        int ticks = Math.max(0, (int) Math.ceil(threshold / pitch) - 2);
        while (ticks * pitch < threshold)
            ticks++;
        return ticks;
    }

    public static NoteSound[] getSounds(final HeldNoteSound[] sounds, final Phase phase) {
        return Arrays.stream(sounds)
            .map((sound) -> sound.getSound(phase))
            .toArray(NoteSound[]::new);
    }


    /**
     * A held note sound instance for 3rd party trigger
     */
    @OnlyIn(Dist.CLIENT)
    public void startPlaying(int notePitch, int particleColor, float volume, Entity initiator, BlockPos pos,
                             InitiatorID initiatorId, ResourceLocation instrumentId) {
        startPlaying(notePitch, particleColor, volume, initiator, pos, initiatorId, instrumentId, PedalChain.NONE);
    }
    /**
     * A held note sound instance for 3rd party trigger, played through the given pedals
     */
    @OnlyIn(Dist.CLIENT)
    public void startPlaying(int notePitch, int particleColor, float volume, Entity initiator, BlockPos pos,
                             InitiatorID initiatorId, ResourceLocation instrumentId, PedalChain pedalChain) {
        new HeldNoteSoundInstance(
            this, Phase.ATTACK,
            notePitch, particleColor, volume, volume,
            initiator, pos,
            initiatorId, instrumentId,
            pedalChain
        ).queueAndAddInstance();
    }
    /**
     * A held note sound instance for 3rd party trigger
     */
    @OnlyIn(Dist.CLIENT)
    public void startPlaying(int notePitch, int particleColor, float volume, Entity initiator,
                             InitiatorID initiatorId, ResourceLocation instrumentId) {
        startPlaying(notePitch, particleColor, volume, initiator, null, initiatorId, instrumentId);
    }
    /**
     * A held note sound instance for 3rd party trigger
     */
    @OnlyIn(Dist.CLIENT)
    public void startPlaying(int notePitch, int particleColor, float volume, Entity initiator, ResourceLocation instrumentId) {
        startPlaying(notePitch, particleColor, volume, initiator, InitiatorID.fromEntity(initiator), instrumentId);
    }
    /**
     * A held note sound instance for 3rd party trigger
     */
    @OnlyIn(Dist.CLIENT)
    public void startPlaying(int notePitch, int particleColor, float volume, BlockPos pos,
                             InitiatorID initiatorId, ResourceLocation instrumentId) {
        startPlaying(notePitch, particleColor, volume, null, pos, initiatorId, instrumentId);
    }
    /**
     * A held note sound instance for local playing, through the pedals the local player monitors
     */
    @OnlyIn(Dist.CLIENT)
    public void startPlaying(int notePitch, int particleColor, float volume, ResourceLocation instrumentId) {
        final Player player = Minecraft.getInstance().player;
        startPlaying(notePitch, particleColor, volume, player, null, InitiatorID.fromEntity(player), instrumentId,
            PedalMonitor.chain());
    }


    //#region Play From Server

        /**
         * A method for packets to use for playing this note on the client's end.
         * Will also stop the client's background music per preference.
         * @param initiatorId The ID of the entity who initiated the sound. Empty for when it wasn't an entity.
         * @param oInitiatorId The initiator ID of the non-player initiator.
         * @param meta Additional metadata of the Note Sound being played
         */
        @OnlyIn(Dist.CLIENT)
        public void playFromServer(Optional<Integer> initiatorId, Optional<InitiatorID> oInitiatorId,
                                   NoteSoundMetadata meta, HeldSoundPhase phase) {
            playFromServer(initiatorId, oInitiatorId, meta, phase, PedalChain.NONE);
        }
        /**
         * A method for packets to use for playing this note on the client's end.
         * @param pedalChain The pedals of the instrument that played this note, if any
         */
        @OnlyIn(Dist.CLIENT)
        public void playFromServer(Optional<Integer> initiatorId, Optional<InitiatorID> oInitiatorId,
                                   NoteSoundMetadata meta, HeldSoundPhase phase, PedalChain pedalChain) {
            final Player localPlayer = Minecraft.getInstance().player;
            final Level level = localPlayer.level();

            final InitiatorID _initiatorID = InitiatorID.getEither(initiatorId, oInitiatorId);

            if (initiatorId.isPresent()) {
                final Entity initiator = level.getEntity(initiatorId.get());

                MinecraftForge.EVENT_BUS.post(
                    new HeldNoteSoundPlayedEvent(initiator, this, meta, phase, _initiatorID)
                );

                final boolean serverAudioEnabled = ModClientConfigs.SERVER_AUDIO.get();
                if (!serverAudioEnabled) {
                    // Do not play for oneself.
                    if (localPlayer.equals(initiator))
                        return;
                }
            }

            MinecraftForge.EVENT_BUS.post(
                new HeldNoteSoundPlayedEvent(level, this, meta, phase, _initiatorID)
            );


            switch (phase) {
                case ATTACK -> attackFromServer(_initiatorID, meta, pedalChain);
                case RELEASE -> releaseFromServer(_initiatorID, meta);
            }
        }

    @OnlyIn(Dist.CLIENT)
    private void attackFromServer(InitiatorID initiatorID, NoteSoundMetadata meta, PedalChain pedalChain) {
        // Play as an entity, or as other
        final @Nullable Entity initiator = initiatorID.type().equals("entity")
            ? Minecraft.getInstance().level.getEntity(Integer.parseInt(initiatorID.identifier()))
            : null;
        final @Nullable BlockPos pos = (initiator == null) ? meta.pos() : null;
        if (initiator == null && initiatorID.type().equals("entity"))
            return;

        // Speakers relay held notes with their own initiator, so copies are matched by everything else
        NearestCopyFilter.offer(
            new RelayKey(this, meta.pitch(), meta.volume(), meta.particleColor(), meta.instrumentId()),
            HeldNoteSoundInstance.distFromSourceSqr(pos, initiator),
            initiatorID,
            (dropped) -> {
                final HeldNoteSoundInstance instance = new HeldNoteSoundInstance(
                    this, Phase.ATTACK,
                    meta.pitch(), meta.particleColor(), meta.volume() / 100f, meta.volume() / 100f,
                    initiator, pos,
                    initiatorID, meta.instrumentId(),
                    pedalChain
                );
                // Releasing or dampening any of the dropped copies does so to the one played
                instance.addMergedInitiators(dropped);
                instance.queueAndAddInstance();
            }
        );
    }
    private record RelayKey(HeldNoteSound sound, int pitch, int volume, int particleColor,
                            ResourceLocation instrumentId) {}

    @OnlyIn(Dist.CLIENT)
    private void releaseFromServer(InitiatorID initiatorID, NoteSoundMetadata meta) {
        HeldNoteSounds.release(initiatorID, this, meta.pitch());
    }

    //#endregion

    public void writeToNetwork(final FriendlyByteBuf buf) {
        buf.writeResourceLocation(baseSoundLocation);
        buf.writeInt(index);
    }
    public static HeldNoteSound readFromNetwork(final FriendlyByteBuf buf) {
        return HeldNoteSoundRegistrar.getSounds(buf.readResourceLocation())[buf.readInt()];
    }


    public enum Phase {
        ATTACK, HOLD, RELEASE
    }

}
