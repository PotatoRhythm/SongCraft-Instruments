package com.stump.songcraft_instruments.item;

import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.client.ModArmPose;
import com.stump.songcraft_instruments.networking.OpenInstrumentPacketSender;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class WindInstrumentItem extends InstrumentItem {

    public WindInstrumentItem(OpenInstrumentPacketSender onOpenRequest) {
        super(onOpenRequest);
    }
    public WindInstrumentItem(OpenInstrumentPacketSender onOpenRequest, Properties properties) {
        super(onOpenRequest, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            @Override
            public @Nullable HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                return getPlayingArmPose(entityLiving);
            }

        });
    }

    /**
     * @return The arm pose of the given entity playing a wind instrument, or null if it isn't playing one
     */
    protected static @Nullable HumanoidModel.ArmPose getPlayingArmPose(final LivingEntity entityLiving) {
        return (
            (entityLiving instanceof Player player)
            ? (InstrumentOpenProvider.isOpen(player) && InstrumentOpenProvider.isItem(player))
                ? ModArmPose.PLAYING_WIND_INSTRUMENT
                : null
            : null
        );
    }

}
