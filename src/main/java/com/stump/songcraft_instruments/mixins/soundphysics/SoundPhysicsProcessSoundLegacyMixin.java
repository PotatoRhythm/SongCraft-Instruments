package com.stump.songcraft_instruments.mixins.soundphysics;

import com.sonicether.soundphysics.SoundPhysics;
import com.stump.songcraft_instruments.compat.soundphysics.SoundPhysicsCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@link SoundPhysicsProcessSoundMixin} for Sound Physics 1.4.x, whose sound identifier is a {@link String}.
 * Only applied when that overload exists (see {@link SoundPhysicsMixinPlugin}).
 */
@Mixin(value = SoundPhysics.class, remap = false)
public abstract class SoundPhysicsProcessSoundLegacyMixin {
    @Inject(method = SoundPhysicsMixinPlugin.PROCESS_SOUND_LEGACY, at = @At("HEAD"), cancellable = true)
    private static void songcraft_instruments$processAtWorldOrigin(int source, double x, double y, double z,
                                                                   SoundSource category, String sound,
                                                                   boolean auxOnly,
                                                                   CallbackInfoReturnable<Vec3> cir) {
        final ResourceLocation id = ResourceLocation.tryParse(sound);
        if (id != null && SoundPhysicsCompat.redirectProcessing(source, id, auxOnly)) {
            cir.setReturnValue(null);
        }
    }
}
