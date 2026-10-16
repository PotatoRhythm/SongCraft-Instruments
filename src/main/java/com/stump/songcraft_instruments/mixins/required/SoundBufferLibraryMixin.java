package com.stump.songcraft_instruments.mixins.required;

import com.mojang.blaze3d.audio.SoundBuffer;
import com.stump.songcraft_instruments.sound.ChannelTwinSounds;
import com.stump.songcraft_instruments.sound.pedal.PedalBufferCache;
import com.stump.songcraft_instruments.sound.pedal.PedalSounds;
import net.minecraft.Util;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Converts the missing Mono/Stereo twins of the mod's sound files from the file that exists (see {@link ChannelTwinSounds}),
 * and renders sounds played through effects pedals (see {@link PedalSounds}).
 */
@Mixin(SoundBufferLibrary.class)
public class SoundBufferLibraryMixin {
    @Shadow
    @Final
    private ResourceProvider resourceManager;
    @Shadow
    @Final
    private Map<ResourceLocation, CompletableFuture<SoundBuffer>> cache;

    /**
     * The provider is backed by the sound files found on every resource reload (twins included), so it stays current
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void songcraft_instruments$keepResources(ResourceProvider resources, CallbackInfo ci) {
        PedalBufferCache.setResources(resources);
    }

    @Inject(method = "getCompleteBuffer", at = @At("HEAD"), cancellable = true)
    private void songcraft_instruments$loadTwin(ResourceLocation soundId,
                                                CallbackInfoReturnable<CompletableFuture<SoundBuffer>> cir) {
        // Kept in a cache of their own, which lets go of them; vanilla's never does
        if (PedalSounds.isFxFile(soundId)) {
            cir.setReturnValue(PedalBufferCache.get(soundId));
            return;
        }

        if (!ChannelTwinSounds.isTwin(soundId))
            return;

        cir.setReturnValue(cache.computeIfAbsent(soundId, location -> CompletableFuture.supplyAsync(
                () -> ChannelTwinSounds.loadTwin(resourceManager, location),
                Util.backgroundExecutor()
        )));
    }
}
