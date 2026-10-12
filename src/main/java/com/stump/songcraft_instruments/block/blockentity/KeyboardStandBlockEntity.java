package com.stump.songcraft_instruments.block.blockentity;

import com.stump.songcraft_instruments.block.KeyboardStandBlock;
import com.stump.songcraft_instruments.block.blockentity.keyboard.KeyboardKeys;
import com.stump.songcraft_instruments.block.blockentity.keyboard.KeyboardKeysHolder;
import com.stump.songcraft_instruments.block.partial.InstrumentBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class KeyboardStandBlockEntity
        extends InstrumentBlockEntity
        implements GeoBlockEntity, KeyboardKeysHolder {

    private final AnimatableInstanceCache cache =
            GeckoLibUtil.createInstanceCache(this);
    private final KeyboardKeys keys = new KeyboardKeys();

    public KeyboardStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KEYBOARD_STAND.get(), pos, state);
    }

    @Override
    public KeyboardKeys getKeyboardKeys() {
        return keys;
    }

    @Override
    public boolean hasKeyboard() {
        return getBlockState().getValue(KeyboardStandBlock.HAS_KEYBOARD);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(
            AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(
                worldPosition.getX() - 1,
                worldPosition.getY(),
                worldPosition.getZ() - 1,
                worldPosition.getX() + 2,
                worldPosition.getY() + 2,
                worldPosition.getZ() + 2
        );
    }
}