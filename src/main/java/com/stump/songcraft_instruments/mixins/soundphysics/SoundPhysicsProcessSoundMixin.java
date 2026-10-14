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
 * Every sound Sound Physics evaluates (when played, and when updated) passes through {@code processSound}.
 * For instrument sounds, we evaluate their real position instead.
 * <p>For Sound Physics 1.5.0+, whose sound identifier is a {@link ResourceLocation}.
 * Only applied when that overload exists (see {@link SoundPhysicsMixinPlugin}).</p>
 */
@Mixin(value = SoundPhysics.class, remap = false)
public abstract class SoundPhysicsProcessSoundMixin {
    @Inject(method = SoundPhysicsMixinPlugin.PROCESS_SOUND, at = @At("HEAD"), cancellable = true)
    private static void songcraft_instruments$processAtWorldOrigin(int source, double x, double y, double z,
                                                                   SoundSource category, ResourceLocation sound,
                                                                   boolean auxOnly,
                                                                   CallbackInfoReturnable<Vec3> cir) {
        if (SoundPhysicsCompat.redirectProcessing(source, sound, auxOnly)) {
            cir.setReturnValue(null);
        }
    }
}
