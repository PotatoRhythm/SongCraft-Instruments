package com.stump.songcraft_instruments.mixins.soundphysics;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.SoundBuffer;
import com.stump.songcraft_instruments.compat.soundphysics.ChannelSource;
import com.stump.songcraft_instruments.compat.soundphysics.SoundPhysicsCompat;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tracks instrument sound channels for the Sound Physics integration, and mirrors everything done to them
 * onto their twin sources (see {@code SoundPair}). These hooks do nothing for other channels.
 */
@Mixin(Channel.class)
public abstract class ChannelMixin implements ChannelSource {
    @Shadow
    @Final
    private int source;

    @Unique
    @Override
    public int songcraft_instruments$getSource() {
        return source;
    }

    @ModifyVariable(method = "setSelfPosition", at = @At("HEAD"), argsOnly = true)
    private Vec3 songcraft_instruments$updateInstrumentSound(Vec3 pos) {
        return SoundPhysicsCompat.onPositionUpdate(source, pos);
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void songcraft_instruments$untrackSource(CallbackInfo ci) {
        SoundPhysicsCompat.onChannelDestroyed(source);
    }


    // Mirroring onto the twin

    @ModifyVariable(method = "setVolume", at = @At("HEAD"), argsOnly = true)
    private float songcraft_instruments$splitVolume(float volume) {
        return SoundPhysicsCompat.onSetVolume(source, volume);
    }

    @Inject(method = "setPitch", at = @At("TAIL"))
    private void songcraft_instruments$mirrorPitch(float pitch, CallbackInfo ci) {
        SoundPhysicsCompat.onSetPitch(source, pitch);
    }

    @Inject(method = "setLooping", at = @At("TAIL"))
    private void songcraft_instruments$mirrorLooping(boolean looping, CallbackInfo ci) {
        SoundPhysicsCompat.onSetLooping(source, looping);
    }

    @Inject(method = "setRelative", at = @At("TAIL"))
    private void songcraft_instruments$mirrorRelative(boolean relative, CallbackInfo ci) {
        SoundPhysicsCompat.onSetRelative(source, relative);
    }

    @Inject(method = {"disableAttenuation", "linearAttenuation"}, at = @At("TAIL"))
    private void songcraft_instruments$mirrorAttenuation(CallbackInfo ci) {
        SoundPhysicsCompat.onAttenuationSet(source);
    }

    @Inject(method = "attachStaticBuffer", at = @At("TAIL"))
    private void songcraft_instruments$mirrorBuffer(SoundBuffer buffer, CallbackInfo ci) {
        SoundPhysicsCompat.onAttachStaticBuffer(source);
    }

    @Inject(method = "attachBufferStream", at = @At("HEAD"))
    private void songcraft_instruments$dropTwinForStream(AudioStream stream, CallbackInfo ci) {
        SoundPhysicsCompat.onAttachBufferStream(source);
    }

    @Redirect(
        method = {"play", "unpause"},
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/AL10;alSourcePlay(I)V", remap = false)
    )
    private void songcraft_instruments$playWithTwin(int alSource) {
        SoundPhysicsCompat.play(alSource);
    }

    @Redirect(
        method = "pause",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/AL10;alSourcePause(I)V", remap = false)
    )
    private void songcraft_instruments$pauseWithTwin(int alSource) {
        SoundPhysicsCompat.pause(alSource);
    }

    @Inject(method = "stop", at = @At("TAIL"))
    private void songcraft_instruments$stopTwin(CallbackInfo ci) {
        SoundPhysicsCompat.onStop(source);
    }
}
