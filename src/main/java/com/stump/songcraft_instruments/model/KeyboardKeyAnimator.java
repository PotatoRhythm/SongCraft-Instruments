package com.stump.songcraft_instruments.model;

import com.stump.songcraft_instruments.block.blockentity.keyboard.KeyboardKeys;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * Tilts the key bones of the keyboard model ({@code Octave1_C} through {@code Octave3_B})
 * down to the depths of their {@link KeyboardKeys}. Each key hinges at its back edge.
 */
@OnlyIn(Dist.CLIENT)
public final class KeyboardKeyAnimator {
    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] KEY_BONES = new String[KeyboardKeys.KEY_COUNT];
    static {
        for (int i = 0; i < KEY_BONES.length; i++)
            KEY_BONES[i] = "Octave" + (i / KeyboardKeys.KEYS_PER_OCTAVE + 1) + "_" + NOTE_NAMES[i % KeyboardKeys.KEYS_PER_OCTAVE];
    }

    /**
     * How far down fully pressed keys tilt, in radians.
     * Black keys are shorter, so they tilt a bit further to sink about as deep.
     */
    private static final float WHITE_KEY_ANGLE = (float) Math.toRadians(6),
        BLACK_KEY_ANGLE = (float) Math.toRadians(7.5);

    private KeyboardKeyAnimator() {}

    /**
     * Sets every key bone of the given model to its key's depth.
     * The model's bones are shared between every keyboard rendered, so all keys are set each time.
     * @param keys The keys to follow, or null to put every key at rest
     */
    public static void animateKeys(final GeoModel<?> model, final @Nullable KeyboardKeys keys) {
        for (int i = 0; i < KEY_BONES.length; i++) {
            final CoreGeoBone bone = model.getAnimationProcessor().getBone(KEY_BONES[i]);
            if (bone == null)
                continue;

            final double depth = (keys == null) ? 0 : keys.depth(i);
            final float angle = KeyboardKeys.isBlackKey(i) ? BLACK_KEY_ANGLE : WHITE_KEY_ANGLE;

            // A negative X rotation takes the front of the key down
            bone.setRotX(bone.getInitialSnapshot().getRotX() - (float) depth * angle);
            bone.markRotationAsChanged();
        }
    }
}
