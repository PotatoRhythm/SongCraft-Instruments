package com.stump.songcraft_instruments.item;

import com.stump.songcraft_instruments.client.ModArmPose;
import com.stump.songcraft_instruments.networking.OpenInstrumentPacketSender;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A saxophone, whose players' arms move along with the notes they play
 */
public class SaxophoneItem extends WindInstrumentItem {

    public SaxophoneItem(OpenInstrumentPacketSender onOpenRequest) {
        super(onOpenRequest);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            @Override
            public @Nullable HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                return (getPlayingArmPose(entityLiving) != null) ? ModArmPose.PLAYING_SAXOPHONE : null;
            }

        });
    }
}
