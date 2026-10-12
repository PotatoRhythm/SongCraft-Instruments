package com.stump.songcraft_instruments.model;

import com.stump.songcraft_instruments.block.blockentity.koto.KotoStrings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * Moves the string bones of the koto model ({@code String_1}, the lowest, through {@code String_5})
 * up and down with their {@link KotoStrings}. Only the plucked part of each string, past its bridge, moves.
 */
@OnlyIn(Dist.CLIENT)
public final class KotoStringAnimator {
    private static final String[] STRING_BONES = new String[KotoStrings.STRING_COUNT];
    static {
        for (int i = 0; i < STRING_BONES.length; i++)
            STRING_BONES[i] = "String_" + (i + 1);
    }

    private KotoStringAnimator() {}

    /**
     * Sets every string bone of the given model to its string's displacement.
     * The model's bones are shared between every koto rendered, so all strings are set each time.
     * @param strings The strings to follow, or null to put every string at rest
     */
    public static void animateStrings(final GeoModel<?> model, final @Nullable KotoStrings strings) {
        for (int i = 0; i < STRING_BONES.length; i++) {
            final CoreGeoBone bone = model.getAnimationProcessor().getBone(STRING_BONES[i]);
            if (bone == null)
                continue;

            final double displacement = (strings == null) ? 0 : strings.displacement(i);

            bone.setPosY(bone.getInitialSnapshot().getOffsetY() + (float) displacement);
            bone.markPositionAsChanged();
        }
    }
}
