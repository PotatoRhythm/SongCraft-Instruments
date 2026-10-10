package com.stump.songcraft_instruments.mixins.soundphysics;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Only applies the Sound Physics Remastered integration mixins when it is installed
 */
public class SoundPhysicsMixinPlugin implements IMixinConfigPlugin {
    private static final String SOUND_PHYSICS_MOD_ID = "sound_physics_remastered";

    private boolean soundPhysicsLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        soundPhysicsLoaded = FMLLoader.getLoadingModList().getModFileById(SOUND_PHYSICS_MOD_ID) != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return soundPhysicsLoaded;
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
