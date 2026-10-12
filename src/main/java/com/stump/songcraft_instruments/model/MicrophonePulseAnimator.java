package com.stump.songcraft_instruments.model;

import com.stump.songcraft_instruments.block.blockentity.microphone.MicrophonePulse;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * Pulses the {@code Mic} bone of the microphone stand model with its {@link MicrophonePulse}.
 * The mic scales and tilts about its clip on the stand.
 */
@OnlyIn(Dist.CLIENT)
public final class MicrophonePulseAnimator {
    private static final String MIC_BONE = "Mic";

    /** How much bigger the mic gets while notes are held */
    private static final float SWELL_SCALE = 0.015f;
    /** How much bigger the mic gets at the peak of a new note's pop, on top of its swell */
    private static final float POP_SCALE = 0.008f;

    /** How much the mic throbs while notes are held, like a singer's vibrato */
    private static final float THROB_SCALE = 0.002f;
    /** How many times a second the mic throbs */
    private static final double THROB_RATE = 5.5;

    /** How far the mic nods at the peak of a new note's pop, in radians */
    private static final float POP_NOD_ANGLE = (float) Math.toRadians(1.5);

    private MicrophonePulseAnimator() {}

    /**
     * Sets the mic bone of the given model to the given pulse.
     * The model's bones are shared between every microphone stand rendered, so the bone is set each time.
     * @param pulse The pulse to follow, or null to put the mic at rest
     */
    public static void animateMic(final GeoModel<?> model, final @Nullable MicrophonePulse pulse) {
        final CoreGeoBone bone = model.getAnimationProcessor().getBone(MIC_BONE);
        if (bone == null)
            return;

        final double swell = (pulse == null) ? 0 : pulse.swell();
        final double pop = (pulse == null) ? 0 : pulse.pop();
        final double throb = Math.sin(MicrophonePulse.now() * THROB_RATE * 2 * Math.PI);

        final float scale = (float) (1 + swell * (SWELL_SCALE + throb * THROB_SCALE) + pop * POP_SCALE);
        bone.setScaleX(bone.getInitialSnapshot().getScaleX() * scale);
        bone.setScaleY(bone.getInitialSnapshot().getScaleY() * scale);
        bone.setScaleZ(bone.getInitialSnapshot().getScaleZ() * scale);
        bone.markScaleAsChanged();

        bone.setRotX(bone.getInitialSnapshot().getRotX() + (float) pop * POP_NOD_ANGLE);
        bone.markRotationAsChanged();
    }
}
