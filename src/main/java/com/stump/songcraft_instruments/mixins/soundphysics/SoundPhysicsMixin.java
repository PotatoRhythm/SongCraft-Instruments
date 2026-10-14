package com.stump.songcraft_instruments.mixins.soundphysics;

import com.sonicether.soundphysics.SoundPhysics;
import com.stump.songcraft_instruments.compat.soundphysics.SoundPhysicsCompat;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The environment (muffling and reverb) Sound Physics applies to instrument sounds via {@code setEnvironment}
 * is kept above a minimum clarity and glides into place, as does where they are heard from.
 * <p>Their evaluation is redirected by {@link SoundPhysicsProcessSoundMixin} (Sound Physics 1.5.0+)
 * or {@link SoundPhysicsProcessSoundLegacyMixin} (1.4.x).</p>
 */
@Mixin(value = SoundPhysics.class, remap = false)
public abstract class SoundPhysicsMixin {
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
