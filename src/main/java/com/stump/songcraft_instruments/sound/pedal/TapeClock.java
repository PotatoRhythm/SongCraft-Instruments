package com.stump.songcraft_instruments.sound.pedal;

import com.stump.songcraft_instruments.SCInstrumentMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * A clock every player shares: the world's game time, smoothed between ticks.
 * <p>Live effects whose randomness every player should hear alike (a tape's wobble and dropouts, see
 * {@code LofiParams}) run off it, so the whole audience hears them at the same moment, give or take a tick.
 * Read from any thread.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public final class TapeClock {
    private static final double TICK_NANOS = 50_000_000;

    private static volatile long gameTime = 0;
    private static volatile long tickNanos = System.nanoTime();

    private TapeClock() {}

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused())
            return;

        final long time = minecraft.level.getGameTime();
        if (time != gameTime) {
            gameTime = time;
            tickNanos = System.nanoTime();
        }
    }

    /**
     * @return The game time in seconds, moving smoothly between ticks (but never past the next one)
     */
    public static double seconds() {
        final double sinceTick = Math.min(1, (System.nanoTime() - tickNanos) / TICK_NANOS);
        return (gameTime + sinceTick) / 20.0;
    }
}
