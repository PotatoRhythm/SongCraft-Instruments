package com.stump.songcraft_instruments.client.trombone;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.client.gui.instrument.trombone.TromboneScreen;
import com.stump.songcraft_instruments.event.HeldNoteSoundPlayedEvent;
import com.stump.songcraft_instruments.event.InstrumentPlayedEvent;
import com.stump.songcraft_instruments.event.NoteSoundPlayedEvent;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Moves the trombone slides of the players playing them, for everyone that hears them
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public class TromboneSlides {
    private static final Map<UUID, TromboneSlide> SLIDES = new HashMap<>();

    /** The entity currently being rendered, for the trombone it's holding to know whose slide to show */
    private static @Nullable Entity renderedEntity;


    /**
     * @param context The context the trombone is being rendered in
     * @return How far out the slide of the trombone being rendered is: 0 in 1st position and 1 in 7th
     */
    public static double extension(final ItemDisplayContext context) {
        final Entity holder = context.firstPerson() ? Minecraft.getInstance().player
            : (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                ? renderedEntity
                : null;

        if (holder == null)
            return 0;

        final TromboneSlide slide = SLIDES.get(holder.getUUID());
        return (slide == null) ? 0 : slide.extension();
    }


    @SubscribeEvent
    public static void onNoteSoundPlayed(final NoteSoundPlayedEvent event) {
        getSlide(event).ifPresent((slide) ->
            slide.tap(TromboneSlide.extensionOf(event.sound().index, event.soundMeta().pitch()))
        );
    }

    @SubscribeEvent
    public static void onHeldNoteSoundPlayed(final HeldNoteSoundPlayedEvent event) {
        getSlide(event).ifPresent((slide) -> {
            if (event.phase == HeldSoundPhase.ATTACK)
                slide.hold(TromboneSlide.extensionOf(event.sound().index(), event.soundMeta().pitch()));
            else
                slide.release();
        });
    }

    /**
     * @return The slide of the player that played the event's note on a trombone, if one did
     */
    private static Optional<TromboneSlide> getSlide(final InstrumentPlayedEvent<?> event) {
        if (!event.level().isClientSide || !event.isByPlayer()
                || !TromboneScreen.INSTRUMENT_ID.equals(event.soundMeta().instrumentId()))
            return Optional.empty();

        final Player player = (Player) event.entityInfo().get().entity;
        return Optional.of(SLIDES.computeIfAbsent(player.getUUID(), (uuid) -> new TromboneSlide()));
    }


    @SubscribeEvent
    public static void onPreRenderLiving(final RenderLivingEvent.Pre<?, ?> event) {
        renderedEntity = event.getEntity();
    }

    @SubscribeEvent
    public static void onPostRenderLiving(final RenderLivingEvent.Post<?, ?> event) {
        renderedEntity = null;
    }

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        SLIDES.clear();
    }
}
