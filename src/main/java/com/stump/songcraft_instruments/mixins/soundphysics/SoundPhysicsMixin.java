package com.stump.songcraft_instruments.mixins.soundphysics;

import com.sonicether.soundphysics.SoundPhysics;
import com.stump.songcraft_instruments.compat.soundphysics.SoundPhysicsCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every sound Sound Physics evaluates (when played, and when updated) passes through {@code processSound}.
 * For instrument sounds, we evaluate their real position instead,
 * and the environment (muffling and reverb) it then applies via {@code setEnvironment} is kept above
 * a minimum clarity and glides into place.
 */
@Mixin(value = SoundPhysics.class, remap = false)
public abstract class SoundPhysicsMixin {
    @Inject(
            method = "processSound(IDDDLnet/minecraft/sounds/SoundSource;Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void songcraft_instruments$processAtWorldOrigin(int source, double x, double y, double z,
                                                                   SoundSource category, ResourceLocation sound,
                                                                   boolean auxOnly,
                                                                   CallbackInfoReturnable<Vec3> cir) {
        if (SoundPhysicsCompat.redirectProcessing(source, sound, auxOnly)) {
            cir.setReturnValue(null);
        }
    }

    /**
     * Instrument sounds glide to where Sound Physics evaluated them to be heard from, rather than jumping there
     */
    @Inject(method = "setSoundPos", at = @At("HEAD"), cancellable = true, require = 0)
    private static void songcraft_instruments$keepInstrumentPosition(int source, Vec3 pos, CallbackInfo ci) {
        if (SoundPhysicsCompat.isProcessing(source)) {
            ci.cancel();
        }
    }

    @Inject(method = "setEnvironment", at = @At("HEAD"), cancellable = true, require = 0)
    private static void songcraft_instruments$glideEnvironment(int source,
                                                               float sendGain0, float sendGain1, float sendGain2, float sendGain3,
                                                               float sendCutoff0, float sendCutoff1, float sendCutoff2, float sendCutoff3,
                                                               float directCutoff, float directGain,
                                                               CallbackInfo ci) {
        final float[] values = {
                sendGain0, sendGain1, sendGain2, sendGain3,
                sendCutoff0, sendCutoff1, sendCutoff2, sendCutoff3,
                directCutoff, directGain
        };

        if (SoundPhysicsCompat.onSetEnvironment(source, values)) {
            ci.cancel();
        }
    }
}
