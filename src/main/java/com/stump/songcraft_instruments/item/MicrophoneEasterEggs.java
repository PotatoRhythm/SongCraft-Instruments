package com.stump.songcraft_instruments.item;

import com.stump.songcraft_instruments.SCInstrumentMod;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.jetbrains.annotations.Nullable;

/**
 * The microphone's hidden voices:
 * using it on a cat or a wolf sings through them,
 * and holding cooked beef in the other hand brings out the Meatball Man.
 */
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID)
public class MicrophoneEasterEggs {

    public enum Voice {
        CAT, DOG, MEATBALL_MAN
    }

    /**
     * How long, in game ticks, the voice of a used-on animal waits for its microphone screen to open
     */
    private static final int TARGET_VOICE_TIMEOUT = 40;

    private static @Nullable Voice targetVoice;
    private static long targetVoiceSetAt;

    /**
     * @return The voice of the animal the local player last used the microphone on,
     * if the microphone was opened from that. It is only handed out once.
     */
    public static @Nullable Voice takeTargetVoice(final Player player) {
        final Voice voice = targetVoice;
        targetVoice = null;

        return ((voice != null) && (player.level().getGameTime() - targetVoiceSetAt <= TARGET_VOICE_TIMEOUT))
            ? voice
            : null;
    }

    /**
     * @return Whether the player has cooked beef in either hand, for the Meatball Man.
     * The microphone takes up the other one.
     */
    public static boolean hasMeatballManVoice(final Player player) {
        return player.getMainHandItem().is(Items.COOKED_BEEF) || player.getOffhandItem().is(Items.COOKED_BEEF);
    }

    private static @Nullable Voice getVoiceOf(final Entity target) {
        if (target.getType() == EntityType.CAT)
            return Voice.CAT;
        if (target.getType() == EntityType.WOLF)
            return Voice.DOG;
        return null;
    }


    /**
     * Opens the microphone on cats and wolves before they handle the interaction themselves,
     * so tamed ones don't just sit down
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(final PlayerInteractEvent.EntityInteract event) {
        final Player player = event.getEntity();
        if (!player.getItemInHand(event.getHand()).is(ModItems.MICROPHONE.get()))
            return;

        final Voice voice = getVoiceOf(event.getTarget());
        if (voice == null)
            return;

        if (event.getLevel().isClientSide) {
            targetVoice = voice;
            targetVoiceSetAt = event.getLevel().getGameTime();
        } else {
            player.getItemInHand(event.getHand()).use(event.getLevel(), player, event.getHand());
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        if (event.getLevel().isClientSide())
            targetVoice = null;
    }
}
