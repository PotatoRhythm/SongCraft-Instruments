package com.stump.songcraft_instruments.compat.soundphysics;

import com.mojang.logging.LogUtils;
import com.sonicether.soundphysics.SoundPhysics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Bridges to Sound Physics Remastered's {@code processSound}, whose sound-identifier parameter changed type
 * between versions: a {@link String} in 1.4.x, a {@link ResourceLocation} in 1.5.0+. The matching overload is
 * resolved once at runtime so a single build works with either.
 * <p>The other Sound Physics members the integration uses ({@code setEnvironment} and the config entries)
 * are identical across those versions, so they are called directly.</p>
 */
@OnlyIn(Dist.CLIENT)
final class SoundPhysicsApi {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean resolved;
    private static MethodHandle processSound;
    private static boolean usesStringId;

    private SoundPhysicsApi() {
    }

    /**
     * @return Whether a usable {@code processSound} overload was found in the installed Sound Physics
     */
    static boolean isAvailable() {
        return handle() != null;
    }

    /**
     * Calls Sound Physics' {@code processSound} with whichever identifier type the installed version expects.
     * @return The possibly-redirected sound position, or null if it is unchanged or the call is unavailable
     */
    static Vec3 processSound(int source, double x, double y, double z,
                             SoundSource category, ResourceLocation sound, boolean auxOnly) {
        final MethodHandle handle = handle();
        if (handle == null)
            return null;

        try {
            final Object id = usesStringId ? sound.toString() : sound;
            return (Vec3) handle.invoke(source, x, y, z, category, id, auxOnly);
        } catch (Throwable t) {
            LOGGER.error("Sound Physics Remastered processSound call failed; instrument integration is now inert", t);
            processSound = null;
            return null;
        }
    }

    private static synchronized MethodHandle handle() {
        if (resolved)
            return processSound;
        resolved = true;

        final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        try {
            // 1.5.0+: the identifier is a ResourceLocation
            processSound = lookup.findStatic(SoundPhysics.class, "processSound", MethodType.methodType(
                    Vec3.class, int.class, double.class, double.class, double.class, SoundSource.class, ResourceLocation.class, boolean.class));
            usesStringId = false;
        } catch (ReflectiveOperationException resourceLocationVariant) {
            try {
                // 1.4.x: the identifier is a String
                processSound = lookup.findStatic(SoundPhysics.class, "processSound", MethodType.methodType(
                        Vec3.class, int.class, double.class, double.class, double.class, SoundSource.class, String.class, boolean.class));
                usesStringId = true;
            } catch (ReflectiveOperationException stringVariant) {
                LOGGER.warn("No known Sound Physics Remastered processSound(...) overload was found; instrument integration will be inert");
            }
        }
        return processSound;
    }
}
