package com.stump.songcraft_instruments.mixins.soundphysics;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forgespi.language.IModFileInfo;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies the Sound Physics Remastered integration only when a compatible version of it is installed.
 * <p>The integration uses Sound Physics' {@code ResourceLocation}-based sound API, introduced in 1.5.0;
 * 1.4.x used {@code String} instead, so calling it there would throw at runtime and hang the sound thread.
 * On an older (or unreadable) version, none of the integration's mixins apply and Songcraft behaves as if
 * Sound Physics were not installed.</p>
 */
public class SoundPhysicsMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SOUND_PHYSICS_MOD_ID = "sound_physics_remastered";

    private boolean compatible;

    @Override
    public void onLoad(String mixinPackage) {
        compatible = isCompatibleSoundPhysicsPresent();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return compatible;
    }

    /**
     * Reads Sound Physics' version from its mod metadata (without loading any of its classes, which would be
     * unsafe this early in mixin setup) and checks it is at least 1.5.0.
     */
    private static boolean isCompatibleSoundPhysicsPresent() {
        final IModFileInfo modFile = FMLLoader.getLoadingModList().getModFileById(SOUND_PHYSICS_MOD_ID);
        if (modFile == null)
            return false;

        final String version;
        try {
            version = modFile.getMods().get(0).getVersion().toString();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not read the Sound Physics Remastered version; its integration will be disabled", e);
            return false;
        }

        final boolean ok = isAtLeast150(version);
        if (!ok)
            LOGGER.warn("Sound Physics Remastered {} is older than the required 1.5.0; its integration will be disabled", version);
        return ok;
    }

    /**
     * @param version a Sound Physics version such as {@code "1.20.1-1.5.1"} (its own version follows the last dash)
     */
    static boolean isAtLeast150(String version) {
        if (version == null)
            return false;

        final String own = version.substring(version.lastIndexOf('-') + 1);
        final String[] parts = own.split("\\.");
        try {
            final int major = Integer.parseInt(parts[0]);
            final int minor = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;
            return (major > 1) || (major == 1 && minor >= 5);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
