package com.stump.songcraft_instruments.mixins.required;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.SoundBuffer;
import com.stump.songcraft_instruments.sound.ChannelSource;
import com.stump.songcraft_instruments.sound.UnderwaterMuffling;
import com.stump.songcraft_instruments.sound.pedal.PedalBufferCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class ChannelSourceMixin implements ChannelSource {
    @Shadow
    @Final
    private int source;

    @Unique
    @Override
    public int songcraft_instruments$getSource() {
        return source;
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void songcraft_instruments$untrackMuffling(CallbackInfo ci) {
        UnderwaterMuffling.onChannelDestroyed(source);
        PedalBufferCache.onDetach(source);
    }

    /**
     * Rendered pedal sounds are only let go of while no source plays them
     */
    @Inject(method = "attachStaticBuffer", at = @At("TAIL"))
    private void songcraft_instruments$trackPedalBuffer(SoundBuffer buffer, CallbackInfo ci) {
        PedalBufferCache.onAttach(source, buffer);
    }
}
