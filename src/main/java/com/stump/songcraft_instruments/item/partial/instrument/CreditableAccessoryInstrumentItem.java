package com.stump.songcraft_instruments.item.partial.instrument;

import com.stump.songcraft_instruments.item.AccessoryInstrumentItem;
import com.stump.songcraft_instruments.item.InstrumentAccessoryItem;
import com.stump.songcraft_instruments.networking.OpenInstrumentPacketSender;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CreditableAccessoryInstrumentItem extends AccessoryInstrumentItem implements CreditableInstrument {

    private final String credit;

    public CreditableAccessoryInstrumentItem(OpenInstrumentPacketSender onOpenRequest, InstrumentAccessoryItem accessory,
                                             String credit) {
        super(onOpenRequest, accessory);
        this.credit = credit;
    }
    public CreditableAccessoryInstrumentItem(OpenInstrumentPacketSender onOpenRequest, Properties properties,
                                             InstrumentAccessoryItem accessory, String credit) {
        super(onOpenRequest, properties, accessory);
        this.credit = credit;
    }

    @Override
    public @Nullable String getCredit() {
        return credit;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        creditHoverText(pTooltipComponents);
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

}
