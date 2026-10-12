package com.stump.songcraft_instruments.client;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.keyboard.KeyboardKeys;
import com.stump.songcraft_instruments.block.blockentity.keyboard.KeyboardKeysHolder;
import com.stump.songcraft_instruments.client.gui.instrument.keyboard.KeyboardScreen;
import com.stump.songcraft_instruments.event.HeldNoteSoundPlayedEvent;
import com.stump.songcraft_instruments.event.InstrumentPlayedEvent;
import com.stump.songcraft_instruments.event.NoteSoundPlayedEvent;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.Optional;

/**
 * Presses the keys of the keyboard that notes are played on, for everyone that hears them
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public class KeyboardKeyListener {

    @SubscribeEvent
    public static void onNoteSoundPlayed(final NoteSoundPlayedEvent event) {
        getKeys(event).ifPresent((keys) -> keys.tap(getKey(event, event.sound().index)));
    }

    @SubscribeEvent
    public static void onHeldNoteSoundPlayed(final HeldNoteSoundPlayedEvent event) {
        getKeys(event).ifPresent((keys) -> {
            final int key = getKey(event, event.sound().index());

            if (event.phase == HeldSoundPhase.ATTACK)
                keys.hold(key);
            else
                keys.release(key);
        });
    }


    private static int getKey(final InstrumentPlayedEvent<?> event, final int soundIndex) {
        return KeyboardKeys.keyOf(soundIndex, event.soundMeta().pitch());
    }

    /**
     * @return The keys of the keyboard the event's note was played on, if it was played on one.
     * Notes relayed through speakers or loopers come from those blocks instead, so they press no keys.
     */
    private static Optional<KeyboardKeys> getKeys(final InstrumentPlayedEvent<?> event) {
        if (!event.level().isClientSide || !KeyboardScreen.INSTRUMENT_ID.equals(event.soundMeta().instrumentId()))
            return Optional.empty();

        return (event.level().getBlockEntity(event.soundMeta().pos()) instanceof KeyboardKeysHolder holder)
                && holder.hasKeyboard()
            ? Optional.of(holder.getKeyboardKeys())
            : Optional.empty();
    }
}
