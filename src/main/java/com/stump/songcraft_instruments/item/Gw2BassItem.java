package com.stump.songcraft_instruments.item;

import com.stump.songcraft_instruments.client.ModArmPose;
import com.stump.songcraft_instruments.item.clientExtensions.InstrumentItemClientExt;
import com.stump.songcraft_instruments.networking.OpenInstrumentPacketSender;
import com.stump.songcraft_instruments.render.Gw2BassItemRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * The GW2 bass, rendered with a GeckoLib model so its gears, embers and smoke can keep animating.
 * Players play it in their arms, see {@link com.stump.songcraft_instruments.client.gw2bass.Gw2BassPlaying}.
 */
public class Gw2BassItem extends InstrumentItem implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.gw2_bass.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Gw2BassItem(OpenInstrumentPacketSender onOpenRequest) {
        super(onOpenRequest);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new InstrumentItemClientExt() {
            private Gw2BassItemRenderer renderer;

            @Override
            public @Nullable HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                return (super.getArmPose(entityLiving, hand, itemStack) != null) ? ModArmPose.PLAYING_GW2_BASS : null;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null)
                    renderer = new Gw2BassItemRenderer();

                return renderer;
            }
        });
    }
}
