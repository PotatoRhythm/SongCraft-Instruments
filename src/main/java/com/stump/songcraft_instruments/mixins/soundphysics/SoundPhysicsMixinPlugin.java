package com.stump.songcraft_instruments.mixins.soundphysics;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Applies the Sound Physics Remastered integration only when it is installed.
 * <p>Both the 1.4.x and 1.5.x sound APIs are supported: {@code SoundPhysicsApi} bridges the one call whose
 * signature differs between them. If a future version removes the expected method entirely, the bridge becomes
 * a no-op at runtime rather than crashing.</p>
 * <p>The {@code processSound} injector for each version is in its own mixin, applied only when its exact overload
 * exists: when it doesn't, Mixin falls back to another overload of the same name and fails the whole mixin,
 * despite {@code require = 0}.</p>
 */
public class SoundPhysicsMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SOUND_PHYSICS_MOD_ID = "sound_physics_remastered";
    private static final String SOUND_PHYSICS_CLASS = "com.sonicether.soundphysics.SoundPhysics";

    /**
     * {@code processSound} in Sound Physics 1.5.0+, whose sound identifier is a ResourceLocation
     */
    static final String PROCESS_SOUND =
        "processSound(IDDDLnet/minecraft/sounds/SoundSource;Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/world/phys/Vec3;";
    /**
     * {@code processSound} in Sound Physics 1.4.x, whose sound identifier is a String
     */
    static final String PROCESS_SOUND_LEGACY =
        "processSound(IDDDLnet/minecraft/sounds/SoundSource;Ljava/lang/String;Z)Lnet/minecraft/world/phys/Vec3;";

    private boolean soundPhysicsLoaded;
    /**
     * The {@code name(descriptor)} of every method of Sound Physics' main class, read once it is first needed
     */
    private Set<String> soundPhysicsMethods;

    @Override
    public void onLoad(String mixinPackage) {
        soundPhysicsLoaded = FMLLoader.getLoadingModList().getModFileById(SOUND_PHYSICS_MOD_ID) != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!soundPhysicsLoaded)
            return false;

        if (mixinClassName.endsWith(".SoundPhysicsProcessSoundMixin"))
            return hasMethod(PROCESS_SOUND);
        if (mixinClassName.endsWith(".SoundPhysicsProcessSoundLegacyMixin"))
            return hasMethod(PROCESS_SOUND_LEGACY);
        return true;
    }

    private boolean hasMethod(String method) {
        if (soundPhysicsMethods == null) {
            soundPhysicsMethods = new HashSet<>();
            try {
                final ClassNode node = MixinService.getService().getBytecodeProvider().getClassNode(SOUND_PHYSICS_CLASS);
                for (final MethodNode methodNode : node.methods)
                    soundPhysicsMethods.add(methodNode.name + methodNode.desc);
            } catch (Exception e) {
                LOGGER.error("Failed to read Sound Physics Remastered's methods; instruments won't be evaluated by it", e);
            }
        }

        return soundPhysicsMethods.contains(method);
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
