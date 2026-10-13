package com.stump.songcraft_instruments.sound;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.sound.held.InitiatorID;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Plays only the nearest copy of a note that arrives from several places at once.
 * <p>A looper's notes are also relayed by each of its paired speakers, and a player's by theirs, so the same note
 * arrives once per position. Playing every copy uses up the client's sound channels (about 247 in total),
 * after which new sounds go silent, and stacks the copies' volumes near several speakers.</p>
 * <p>Copies arriving together are held until the end of the frame, then only the one nearest the listener plays.
 * Packets are handled before the frame's ticks and rendering, so this adds no delay.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public final class NearestCopyFilter {
    private NearestCopyFilter() {}

    /**
     * @param play Plays this copy, given the initiators of the copies dropped in its favor
     */
    private record Copy(double distSqr, InitiatorID initiator, Consumer<Set<InitiatorID>> play) {}
    private static final class Copies {
        Copy nearest;
        final Set<InitiatorID> initiators = new HashSet<>();
    }

    private static final LinkedHashMap<Object, Copies> PENDING = new LinkedHashMap<>();

    /**
     * Offers a copy of a note to be played at the end of the frame, unless a nearer copy of it arrives first.
     * @param key Identifies the note regardless of where it is played from; equal for all copies of it
     * @param distSqr The listener's squared distance to this copy
     * @param initiator The initiator this copy is played with
     * @param play Plays this copy, given the initiators of the other copies, which were dropped in its favor
     */
    public static void offer(final Object key, final double distSqr, final InitiatorID initiator,
                             final Consumer<Set<InitiatorID>> play) {
        final Copies copies = PENDING.computeIfAbsent(key, (k) -> new Copies());
        copies.initiators.add(initiator);
        if ((copies.nearest == null) || (distSqr < copies.nearest.distSqr()))
            copies.nearest = new Copy(distSqr, initiator, play);
    }

    /**
     * Plays the nearest copy of every pending note. Called before releasing or dampening notes,
     * so a note arriving in the same frame as its release is still released.
     */
    public static void flush() {
        if (PENDING.isEmpty())
            return;

        final List<Copies> pending = new ArrayList<>(PENDING.values());
        PENDING.clear();

        final Minecraft minecraft = Minecraft.getInstance();
        if ((minecraft.player == null) || (minecraft.level == null))
            return;

        for (final Copies copies : pending) {
            final Set<InitiatorID> dropped = new HashSet<>(copies.initiators);
            dropped.remove(copies.nearest.initiator());
            copies.nearest.play().accept(dropped);
        }
    }

    public static void discard() {
        PENDING.clear();
    }


    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START)
            flush();
    }
    @SubscribeEvent
    public static void onRenderTick(final TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START)
            flush();
    }
    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        discard();
    }
}
