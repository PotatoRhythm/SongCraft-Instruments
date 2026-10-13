package com.stump.songcraft_instruments.mixins.required;

import com.stump.songcraft_instruments.sound.ChannelTwinSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Lists the missing Mono/Stereo twins of the mod's sound files, so sounds.json can use them (see {@link ChannelTwinSounds}).
 */
@Mixin(targets = "net.minecraft.client.sounds.SoundManager$Preparations")
public class SoundManagerPreparationsMixin {
    @Shadow
    private Map<ResourceLocation, Resource> soundCache;

    @Inject(method = "listResources", at = @At("TAIL"))
    private void songcraft_instruments$listMissingTwins(ResourceManager resourceManager, CallbackInfo ci) {
        soundCache = ChannelTwinSounds.withMissingTwins(soundCache);
    }
}
