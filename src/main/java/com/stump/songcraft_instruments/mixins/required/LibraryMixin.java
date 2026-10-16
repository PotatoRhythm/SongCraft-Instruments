package com.stump.songcraft_instruments.mixins.required;

import com.mojang.blaze3d.audio.Library;
import com.stump.songcraft_instruments.sound.UnderwaterMuffling;
import com.stump.songcraft_instruments.sound.pedal.PedalBufferCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Library.class)
public abstract class LibraryMixin {
    @Shadow
    private long currentDevice;

    @Inject(method = "init", at = @At("TAIL"))
    private void songcraft_instruments$setUpUnderwaterMuffling(@Nullable String deviceSpecifier, boolean enableHrtf, CallbackInfo ci) {
        UnderwaterMuffling.onLibraryInit(currentDevice);
    }

    @Inject(method = "cleanup", at = @At("HEAD"))
    private void songcraft_instruments$clearUnderwaterMuffling(CallbackInfo ci) {
        UnderwaterMuffling.onLibraryCleanup();
        // While the context is still there to delete the buffers from
        PedalBufferCache.clear();
    }
}
