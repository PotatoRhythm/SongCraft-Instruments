package com.stump.songcraft_instruments.sound;

import com.stump.songcraft_instruments.client.config.ModClientConfigs;
import com.stump.songcraft_instruments.client.util.ClientUtil;
import com.stump.songcraft_instruments.event.NoteSoundPlayedEvent;
import com.stump.songcraft_instruments.networking.buttonidentifier.NoteButtonIdentifier;
import com.stump.songcraft_instruments.networking.packet.instrument.NoteSoundMetadata;
import com.stump.songcraft_instruments.particle.ModParticles;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import com.stump.songcraft_instruments.sound.registrar.NoteSoundRegistrar;
import com.stump.songcraft_instruments.util.LabelUtil;
import com.stump.songcraft_instruments.util.ParticleColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * A class holding sound information for an instrument's note
 */
public class NoteSound {
    public static final SoundSource INSTRUMENT_SOUND_SOURCE = SoundSource.RECORDS;

    /**
     * The range at which Stereo starts crossfading into Mono.
     */
    public static final double STEREO_FADE_START = 8;
    /**
     * The range at which players will only hear Mono instead of Stereo.
     * Between {@link #STEREO_FADE_START} and this range, both are heard as a crossfade.
    */
    public static final double STEREO_RANGE = 16;
    /**
     * The distance at which Mono sounds fully fade out (their sounds.json attenuation distance)
     */
    public static final int MONO_DISTANCE = 64;
    /**
     * A volume multiplier for Stereo sounds, for balancing them against their Mono counterparts
     */
    public static final float STEREO_VOLUME = 1f;
    /**
     * The range from which players will hear instruments from their local sound output rather than the level's
     */
    public static final double LOCAL_RANGE = STEREO_RANGE;

    public final int index;
    public final ResourceLocation baseSoundLocation;

    public SoundEvent mono;
    public SoundEvent stereo;

    /**
     * Constructor for assigning mono & stereo lazily
     * @apiNote Please use {@link NoteSoundRegistrar}!
     */
    @Internal
    public NoteSound(int index, ResourceLocation baseSoundLocation) {
        this.index = index;
        this.baseSoundLocation = baseSoundLocation;
    }

    public static int getMinPitch() {
        return -LabelUtil.NOTES_PER_SCALE * 2;
    }

    public static int getMaxPitch() {
        return LabelUtil.NOTES_PER_SCALE * 2;
    }

    public SoundEvent getMono() {
        return mono;
    }

    public boolean hasStereo() {
        return stereo != null;
    }
    @Nullable
    public SoundEvent getStereo() {
        return stereo;
    }

    public NoteSound[] getSoundsArr() {
        return NoteSoundRegistrar.getSounds(baseSoundLocation);
    }

    /**
     * @param playDistSqr The distance between this player and the position of the note's sound squared
     * @return Whether this note should play as Stereo (crossfading into Mono with distance)
     */
    @OnlyIn(Dist.CLIENT)
    public boolean usesStereo(final double playDistSqr) {
        return hasStereo() && (playDistSqr <= Mth.square(STEREO_RANGE));
    }

    /**
     * Determines which sound type should play based on this player's distance from the instrument player.
     * Stereo is heard up close, and Mono further away, since only Mono sounds fade out with distance.
     * <p>This method is fired from the server.</p>
     * @param playDistSqr The distance between this player and the position of the note's sound squared
     * @return Either the Mono or Stereo sound
     */
    @OnlyIn(Dist.CLIENT)
    public SoundEvent getByDistance(final double playDistSqr) {
        return usesStereo(playDistSqr) ? getStereo() : mono;
    }

    /**
     * @return How much of the Stereo sound should be heard at the given distance, from 0 to 1.
     * The Mono sound should be heard at the remainder.
     */
    public static float stereoMix(final double dist) {
        return 1 - (float) Mth.clamp((dist - STEREO_FADE_START) / (STEREO_RANGE - STEREO_FADE_START), 0, 1);
    }

    /**
     * Stereo sounds are not attenuated by OpenAL, so we mimic the linear attenuation Mono sounds get.
     * @return The volume multiplier of a Stereo sound at the given distance
     */
    public static float stereoGain(final double dist) {
        final float attenuation = Math.max(0, 1 - (float) dist / MONO_DISTANCE);
        return stereoMix(dist) * attenuation * STEREO_VOLUME;
    }

    /**
     * @return The volume multiplier of the crossfading Mono sound at the given distance.
     * OpenAL attenuates it on its own.
     */
    public static float crossfadeMonoGain(final double dist) {
        return 1 - stereoMix(dist);
    }

    /**
     * @return The position sounds are heard from
     */
    @OnlyIn(Dist.CLIENT)
    public static Vec3 listenerPos() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }


    /**
     * A method for packets to use for playing this note on the client's end.
     * Will also stop the client's background music per preference.
     * @param initiatorId The ID of the player who initiated the sound. Empty for when it wasn't a player.
     * @param meta Additional metadata of the Note Sound being played
     */
    @OnlyIn(Dist.CLIENT)
    public void playFromServer(Optional<Integer> initiatorId, Optional<InitiatorID> oInitiatorId,
                               NoteSoundMetadata meta) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Player player = minecraft.player;

        final Level level = minecraft.level;
        final Entity initiator = initiatorId.map(level::getEntity).orElse(null);

        final double playDistSqr = meta.pos().getCenter().distanceToSqr(player.position());
        ClientUtil.stopMusicIfClose(playDistSqr);

        MinecraftForge.EVENT_BUS.post(initiator == null
                ? new NoteSoundPlayedEvent(level, this, meta)
                : new NoteSoundPlayedEvent(initiator, this, meta)
        );

        if (initiator != null) {

            double xOffset = (level.random.nextDouble() - 0.5) * 0.30;
            double yOffset = (level.random.nextDouble() - 0.5) * 0.30;
            double zOffset = (level.random.nextDouble() - 0.5) * 0.30;

            float bodyYaw = initiator.getYRot();
            double radians = Math.toRadians(bodyYaw);
            double forwardX = -Math.sin(radians);
            double forwardZ = Math.cos(radians);

            int rgb = meta.particleColor();

            level.addParticle(
                    ModParticles.CUSTOM_NOTE.get(),
                    initiator.getX() + forwardX * 0.6 + xOffset,
                    initiator.getY() + 1.3 + yOffset,
                    initiator.getZ() + forwardZ * 0.6 + zOffset,
                    rgb,
                    0.15,
                    0
            );
        }

        final boolean serverAudioEnabled = ModClientConfigs.SERVER_AUDIO.get();
        if (!serverAudioEnabled) {
            // Do not play for oneself.
            if (player.equals(initiator))
                return;
        }

        final float mcPitch = getPitchByNoteOffset(clampPitch(meta.pitch()));

        playLocally(
                mcPitch,
                meta,
                playDistSqr,
                initiatorId,
                oInitiatorId
        );
    }


    /**
     * Plays this sound locally. Treats the given {@code pitch} as a Minecraft pitch.
     */
    @OnlyIn(Dist.CLIENT)
    public void playLocally(float pitch, NoteSoundMetadata meta, double playDistSqr,
            Optional<Integer> initiatorId, Optional<InitiatorID> oInitiatorId) {
        final Minecraft minecraft = Minecraft.getInstance();
        final SoundEvent sound = getByDistance(playDistSqr);

        if (sound == null)
            return;

        final NoteSoundInstance instance = new NoteSoundInstance(
                this,
                pitch,
                meta,
                playDistSqr,
                initiatorId,
                oInitiatorId
        );

        minecraft.getSoundManager().play(instance);
        instance.getMonoCrossfade().ifPresent(minecraft.getSoundManager()::play);
        NoteSoundInstances.add(instance);
    }

    @OnlyIn(Dist.CLIENT)
    public void playLocally(int notePitch, float volume, BlockPos pos, double playDistSqr,
            ResourceLocation instrumentId, Optional<NoteButtonIdentifier> noteIdentifier) {
        final float mcPitch = getPitchByNoteOffset(clampPitch(notePitch));

        final NoteSoundMetadata meta = new NoteSoundMetadata(
                pos,
                notePitch,
                (int)(volume * 100),
                ParticleColorUtil.getNoteRGB(index, notePitch),
                instrumentId,
                noteIdentifier
        );

        playLocally(
                mcPitch,
                meta,
                playDistSqr,
                Optional.of(Minecraft.getInstance().player.getId()),
                Optional.empty()
        );
    }

    /**
     * Plays this sound locally. Treats the given {@code pitch} as a Minecraft pitch.
     */
    @OnlyIn(Dist.CLIENT)
    public void playLocally(float pitch, float volume, BlockPos pos) {
        final Minecraft minecraft = Minecraft.getInstance();

        final double playDistSqr =
                minecraft.player.position().distanceToSqr(pos.getCenter());

        final NoteSoundMetadata meta = new NoteSoundMetadata(
                pos,
                0,
                (int)(volume * 100),
                ParticleColorUtil.getNoteRGB(index, 0),
                baseSoundLocation,
                Optional.empty()
        );

        playLocally(
                pitch,
                meta,
                playDistSqr,
                Optional.of(minecraft.player.getId()),
                Optional.empty()
        );
    }

    /**
     * <p>Plays this note locally.</p>
     * Treats the given {@code pitch} as a note offset pitch,
     * thus performs a conversion from note offset pitch to Minecraft pitch.
     * @see NoteSound#getPitchByNoteOffset
     */
    @OnlyIn(Dist.CLIENT)
    public void playLocally(int pitch, float volume, BlockPos pos, double playDistSqr) {
        playLocally(
                pitch,
                volume,
                pos,
                playDistSqr,
                baseSoundLocation,
                Optional.empty()
        );
    }

    /**
     * <p>Plays this note locally.</p>
     * Treats the given {@code pitch} as a note offset pitch,
     * thus performs a conversion from note offset pitch to Minecraft pitch.
     * @see NoteSound#getPitchByNoteOffset
     */
    @OnlyIn(Dist.CLIENT)
    public void playLocally(int pitch, float volume, BlockPos pos) {
        playLocally(pitch,
                volume,
                pos,
                Minecraft.getInstance().player.position().distanceToSqr(pos.getCenter()),
                baseSoundLocation,
                Optional.empty()
        );
    }


    /**
     * Clams the given {@code pitch} between the set range
     */
    public static int clampPitch(final int pitch) {
        return Mth.clamp(pitch, getMinPitch(), getMaxPitch());
    }
    /**
     * Converts the given note offset to Minecraft pitch.
     * @apiNote Formula taken from
     * <a href="https://github.com/Specy/genshin-music/blob/bb8229a279c7e5885ad3ab270b7afbe41f00d1c2/src/lib/Utilities.ts#L207C4-L207C4">
     * Specy's Genshin music app
     * </a>
     */
    public static float getPitchByNoteOffset(final int pitch) {
        return (float)Math.pow(2, (double)pitch / LabelUtil.NOTES_PER_SCALE);
    }



    public void writeToNetwork(final FriendlyByteBuf buf) {
        buf.writeResourceLocation(baseSoundLocation);
        buf.writeInt(index);
    }
    public static NoteSound readFromNetwork(final FriendlyByteBuf buf) {
        return NoteSoundRegistrar.getSounds(buf.readResourceLocation())[buf.readInt()];
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof NoteSound other))
            return false;

        // Mono is enough to determine if the sounds are the same
        return baseSoundLocation.equals(other.baseSoundLocation) && (index == other.index);
    }
}
