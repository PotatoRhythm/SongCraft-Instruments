package com.stump.songcraft_instruments.mixins.required;

import com.mojang.blaze3d.audio.SoundBuffer;
import com.stump.songcraft_instruments.sound.ChannelTwinSounds;
import net.minecraft.Util;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Converts the missing Mono/Stereo twins of the mod's sound files from the file that exists (see {@link ChannelTwinSounds}).
 */
@Mixin(SoundBufferLibrary.class)
public class SoundBufferLibraryMixin {
    @Shadow
    @Final
    private ResourceProvider resourceManager;
    @Shadow
    @Final
    private Map<ResourceLocation, CompletableFuture<SoundBuffer>> cache;

    @Inject(method = "getCompleteBuffer", at = @At("HEAD"), cancellable = true)
    private void songcraft_instruments$loadTwin(ResourceLocation soundId,
                                                CallbackInfoReturnable<CompletableFuture<SoundBuffer>> cir) {
        if (!ChannelTwinSounds.isTwin(soundId))
            return;

        cir.setReturnValue(cache.computeIfAbsent(soundId, location -> CompletableFuture.supplyAsync(
                () -> ChannelTwinSounds.loadTwin(resourceManager, location),
                Util.backgroundExecutor()
        )));
    }
}
