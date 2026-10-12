package com.stump.songcraft_instruments.block.blockentity;

import com.stump.songcraft_instruments.block.blockentity.koto.KotoStrings;
import com.stump.songcraft_instruments.block.partial.InstrumentBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class KotoBlockEntity extends InstrumentBlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);
    private final KotoStrings strings = new KotoStrings();

    public KotoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KOTO.get(), pos, state);
    }

    public KotoStrings getKotoStrings() {
        return strings;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }
}
