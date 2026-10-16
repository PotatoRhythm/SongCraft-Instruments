package com.stump.songcraft_instruments.mixins.required;

import com.stump.songcraft_instruments.sound.CrossfadeMonoSoundInstance;
import com.stump.songcraft_instruments.sound.NoteSoundInstance;
import com.stump.songcraft_instruments.sound.UnderwaterMuffling;
import com.stump.songcraft_instruments.sound.held.HeldNoteSoundInstance;
import com.stump.songcraft_instruments.sound.pedal.PedalLiveEffects;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;
    @Shadow
    @Final
    private SoundEngineExecutor executor;

    @Inject(
            method = "calculatePitch",
            at = @At("RETURN"),
            cancellable = true
    )
    private void songcraft_instruments$expandNoteSoundPitch(
            SoundInstance sound,
            CallbackInfoReturnable<Float> cir
    ) {
        // The Mono crossfade too, or it would be clamped (to 0.5-2) while its Stereo sound isn't,
        // playing notes over an octave from their sample at a different pitch than their Stereo sound
        if (sound instanceof NoteSoundInstance || sound instanceof HeldNoteSoundInstance
                || sound instanceof CrossfadeMonoSoundInstance) {
            // Through their pedals' live effects (like a vibrato)
            cir.setReturnValue(PedalLiveEffects.onPitch(sound, sound.getPitch()));
        }
    }

    /**
     * Fires once the sound's channel is created, but before it is set up and played
     * (on {@code instanceBySource.put(source, sound)}).
     */
    @ModifyArg(
            method = "play",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Multimap;put(Ljava/lang/Object;Ljava/lang/Object;)Z",
                    remap = false
            ),
            index = 1
    )
    private Object songcraft_instruments$trackMuffling(Object instance) {
        final SoundInstance sound = (SoundInstance) instance;
        UnderwaterMuffling.onSoundPlay(sound, instanceToChannel.get(sound));
        PedalLiveEffects.onSoundPlay(sound, instanceToChannel.get(sound));
        return instance;
    }

    @Inject(method = "tickNonPaused", at = @At("TAIL"))
    private void songcraft_instruments$updateMuffling(CallbackInfo ci) {
        UnderwaterMuffling.tick(instanceToChannel, executor);
        PedalLiveEffects.tick(executor);
    }

    /**
     * The volume vanilla gives playing sounds every tick, through their pedals' live effects (like a tremolo)
     */
    @Inject(
            method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("RETURN"),
            cancellable = true
    )
    private void songcraft_instruments$applyLivePedals(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(PedalLiveEffects.onVolume(sound, cir.getReturnValue()));
    }
}
