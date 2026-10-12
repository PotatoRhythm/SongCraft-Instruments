package com.stump.songcraft_instruments.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.MicrophoneStandBlock;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.client.gui.instrument.microphone.MicrophoneScreen;
import com.stump.songcraft_instruments.client.gui.instrument.microphone.MicrophoneStandScreen;
import com.stump.songcraft_instruments.event.HeldNoteSoundPlayedEvent;
import com.stump.songcraft_instruments.event.InstrumentPlayedEvent;
import com.stump.songcraft_instruments.event.NoteSoundPlayedEvent;
import com.stump.songcraft_instruments.item.ModItems;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sways players gently from side to side while they're singing into a microphone, be it held or on a stand.
 * The sway settles back to standing still once they've stopped singing for a moment.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public class MicrophoneSway {
    /** How far players lean to each side, in degrees */
    private static final float SWAY_ANGLE = 4.5f;
    /** How long a full sway to one side and back takes, in seconds */
    private static final float SWAY_PERIOD = 2.4f;
    /** How far players bob up as they lean out to each side, in blocks */
    private static final float BOB_HEIGHT = 0.03f;
    /** How long the sway takes to fade in as players start singing, in seconds */
    private static final double FADE_IN_TIME = 0.45;
    /** How long the sway takes to settle back to standing still after players stop singing, in seconds */
    private static final double FADE_OUT_TIME = 1.2;
    /** How long players keep swaying after their last note ends, in seconds */
    private static final double KEEP_SWAYING_TIME = 0.75;

    /** How much each player is swaying: 0 standing still and 1 swaying fully */
    private static final Map<UUID, Fade> FADES = new HashMap<>();
    /** The notes each player is singing into a microphone */
    private static final Map<UUID, Singing> SINGING = new HashMap<>();

    /** Whether the player being rendered was swayed, for its pose to be restored after */
    private static boolean swayed;

    private static final class Fade {
        double amount, updatedAt = now();
    }

    private static final class Singing {
        /** How many held notes are playing */
        int heldNotes;
        /** When the last note was sung or let go of */
        double lastNoteAt = now();
    }


    @SubscribeEvent
    public static void onNoteSoundPlayed(final NoteSoundPlayedEvent event) {
        getSinging(event).ifPresent((singing) -> singing.lastNoteAt = now());
    }

    @SubscribeEvent
    public static void onHeldNoteSoundPlayed(final HeldNoteSoundPlayedEvent event) {
        getSinging(event).ifPresent((singing) -> {
            if (event.phase == HeldSoundPhase.ATTACK)
                singing.heldNotes++;
            else if (singing.heldNotes > 0)
                singing.heldNotes--;

            singing.lastNoteAt = now();
        });
    }

    /**
     * @return The notes of the player that sang the event's note into a microphone, if one did
     */
    private static Optional<Singing> getSinging(final InstrumentPlayedEvent<?> event) {
        if (!event.level().isClientSide || !event.isByPlayer())
            return Optional.empty();

        final ResourceLocation instrumentId = event.soundMeta().instrumentId();
        if (!MicrophoneScreen.INSTRUMENT_ID.equals(instrumentId) && !MicrophoneStandScreen.INSTRUMENT_ID.equals(instrumentId))
            return Optional.empty();

        return Optional.of(SINGING.computeIfAbsent(event.entityInfo().get().entity.getUUID(), (uuid) -> new Singing()));
    }


    // Lowest priority, so that no other listener cancels the render after it was swayed
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPreRenderPlayer(final RenderPlayerEvent.Pre event) {
        final Player player = event.getEntity();
        final double fade = updateFade(player);
        if (fade <= 0)
            return;

        final float time = (player.tickCount + event.getPartialTick()) / 20f;
        final float phase = time / SWAY_PERIOD * Mth.TWO_PI;
        final float sway = Mth.sin(phase);
        // Ease the fade so the sway starts and settles softly
        final float amount = (float) (fade * fade * (3 - 2 * fade));

        final PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        swayed = true;

        // Bob up at either end of the sway, then lean sideways about the player's feet
        poseStack.translate(0, sway * sway * BOB_HEIGHT * amount, 0);

        final float bodyRot = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sway * SWAY_ANGLE * amount));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRot));
    }

    @SubscribeEvent
    public static void onPostRenderPlayer(final RenderPlayerEvent.Post event) {
        if (!swayed)
            return;

        event.getPoseStack().popPose();
        swayed = false;
    }

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        FADES.clear();
        SINGING.clear();
    }


    /**
     * Moves the given player's sway towards whether they're singing
     * @return How much the player is swaying now
     */
    private static double updateFade(final Player player) {
        final boolean singing = isSinging(player);
        Fade fade = FADES.get(player.getUUID());

        if (fade == null) {
            if (!singing)
                return 0;

            FADES.put(player.getUUID(), fade = new Fade());
        }

        final double now = now();
        final double step = (now - fade.updatedAt) / (singing ? FADE_IN_TIME : FADE_OUT_TIME);
        fade.updatedAt = now;
        fade.amount = Mth.clamp(fade.amount + (singing ? step : -step), 0, 1);

        if (fade.amount <= 0)
            FADES.remove(player.getUUID());

        return fade.amount;
    }

    /**
     * @return Whether the given player is singing into an open microphone, or only just stopped
     */
    private static boolean isSinging(final Player player) {
        final Singing singing = SINGING.get(player.getUUID());
        if (singing == null)
            return false;

        if (!hasMicrophoneOpen(player)) {
            // Forget notes cut off by closing the microphone
            SINGING.remove(player.getUUID());
            return false;
        }

        return (singing.heldNotes > 0) || (now() - singing.lastNoteAt < KEEP_SWAYING_TIME);
    }

    /**
     * @return Whether the given player has a microphone open, either held or on a stand
     */
    private static boolean hasMicrophoneOpen(final Player player) {
        if (!InstrumentOpenProvider.isOpen(player))
            return false;

        if (InstrumentOpenProvider.isItem(player)) {
            return (InstrumentOpenProvider.getHand(player) != null)
                && player.getItemInHand(InstrumentOpenProvider.getHand(player)).is(ModItems.MICROPHONE.get());
        }

        return (InstrumentOpenProvider.getBlockPos(player) != null)
            && player.level().getBlockState(InstrumentOpenProvider.getBlockPos(player)).getBlock() instanceof MicrophoneStandBlock;
    }

    private static double now() {
        return System.nanoTime() / 1e9;
    }
}
