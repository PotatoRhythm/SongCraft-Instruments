package com.stump.songcraft_instruments.mixins.soundphysics;

import com.stump.songcraft_instruments.compat.soundphysics.SoundPhysicsCompat;
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

import java.util.Map;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;
    @Shadow
    @Final
    private SoundEngineExecutor executor;

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
    private Object songcraft_instruments$trackInstrumentSound(Object instance) {
        final SoundInstance sound = (SoundInstance) instance;
        SoundPhysicsCompat.onSoundPlay(sound, instanceToChannel.get(sound));
        return instance;
    }

    @Inject(method = "tickNonPaused", at = @At("TAIL"))
    private void songcraft_instruments$updateInstrumentSounds(CallbackInfo ci) {
        SoundPhysicsCompat.tick(instanceToChannel, executor);
    }
}
