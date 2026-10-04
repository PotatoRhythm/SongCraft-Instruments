package com.stump.songcraft_instruments.item;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.event.InstrumentPlayedEvent;
import com.stump.songcraft_instruments.networking.packet.instrument.util.InstrumentPacketUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

class ViolinItem extends AccessoryInstrumentItem {
    public ViolinItem() {
        super((player) -> InstrumentPacketUtil.sendOpenPacket(
                player, new ResourceLocation(SCInstrumentMod.MODID, "violin")
            ),
            (InstrumentAccessoryItem) ModItems.VIOLIN_BOW.get()
        );
    }

    @Override
    public int hurtAccessoryBy(final InstrumentPlayedEvent<?> event, final ItemStack accessory) {
        return super.hurtAccessoryBy(event, accessory);
    }
}
