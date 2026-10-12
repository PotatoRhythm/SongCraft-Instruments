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
import com.stump.songcraft_instruments.sound.SCSounds;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
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
 * <p>When singing with Sky's Aurora voice, players also raise their free arm out to the side, like Aurora does.</p>
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

    /** How high players raise their free arm out to the side when singing as Aurora, in radians from hanging down */
    private static final float ARM_RAISE_ANGLE = 2.2f;
    /** How far forward the raised arm reaches, in radians */
    private static final float ARM_FORWARD_ANGLE = 0.25f;
    /** How far the raised arm waves up and down along with the sway, in radians */
    private static final float ARM_WAVE_ANGLE = 0.12f;
    /** How long the arm takes to raise as players start singing as Aurora, in seconds */
    private static final double ARM_RAISE_TIME = 0.35;
    /** How long the arm takes to lower back down after players stop singing, in seconds */
    private static final double ARM_LOWER_TIME = 0.9;

    /** How much each player is swaying: 0 standing still and 1 swaying fully */
    private static final Map<UUID, Fade> FADES = new HashMap<>();
    /** How far each player has raised their arm: 0 lowered and 1 raised fully */
    private static final Map<UUID, Fade> ARM_FADES = new HashMap<>();
    /** The notes each player is singing into a microphone */
    private static final Map<UUID, Singing> SINGING = new HashMap<>();

    /** Whether the player being rendered was swayed, for its pose to be restored after */
    private static boolean swayed;
    /** How far the player being rendered has their arm raised, and their age in ticks, for their arm to be posed by */
    private static float renderedArmRaise, renderedAge;

    private static final class Fade {
        double amount, updatedAt = now();
    }

    private static final class Singing {
        /** How many held notes are playing */
        int heldNotes;
        /** When the last note was sung or let go of */
        double lastNoteAt = now();
        /** Whether the last held note was sung with Sky's Aurora voice */
        boolean aurora;
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
            singing.aurora = isAurora(event.sound());
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


    private static boolean isAurora(final HeldNoteSound sound) {
        return SCSounds.SKY_AURORA[0].baseSoundLocation().equals(sound.baseSoundLocation());
    }


    // Lowest priority, so that no other listener cancels the render after it was swayed
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPreRenderPlayer(final RenderPlayerEvent.Pre event) {
        final Player player = event.getEntity();
        final boolean singing = isSinging(player);

        renderedAge = player.tickCount + event.getPartialTick();
        renderedArmRaise = ease(updateFade(ARM_FADES, player,
            singing && SINGING.get(player.getUUID()).aurora, ARM_RAISE_TIME, ARM_LOWER_TIME));

        // Arm poses are set before this event and applied once the model is posed for rendering after it
        if (renderedArmRaise > 0) {
            final PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();

            if (getFreeArm(player) == HumanoidArm.RIGHT)
                model.rightArmPose = ModArmPose.AURORA_RAISED;
            else
                model.leftArmPose = ModArmPose.AURORA_RAISED;
        }

        final double fade = updateFade(FADES, player, singing, FADE_IN_TIME, FADE_OUT_TIME);
        if (fade <= 0)
            return;

        final float sway = Mth.sin(swayPhase(player.tickCount + event.getPartialTick()));
        final float amount = ease(fade);

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
        ARM_FADES.clear();
        SINGING.clear();
    }


    /**
     * Raises the free arm of the player being rendered while singing as Aurora out to the side,
     * waving gently along with their sway
     * @param side Which of the player's arms is raised
     */
    public static void raiseArm(final HumanoidModel<?> model, final HumanoidArm side) {
        final ModelPart arm = (side == HumanoidArm.RIGHT) ? model.rightArm : model.leftArm;
        final float outwards = (side == HumanoidArm.RIGHT) ? 1 : -1;

        final float wave = Mth.sin(swayPhase(renderedAge)) * ARM_WAVE_ANGLE;

        arm.xRot = Mth.lerp(renderedArmRaise, arm.xRot, -ARM_FORWARD_ANGLE);
        arm.yRot = Mth.lerp(renderedArmRaise, arm.yRot, 0);
        arm.zRot = Mth.lerp(renderedArmRaise, arm.zRot, (ARM_RAISE_ANGLE + wave) * outwards);
    }

    /**
     * @return The arm the given player isn't holding their microphone with.
     * On a stand, that's their off arm, leaving their main hand on the microphone.
     */
    private static HumanoidArm getFreeArm(final Player player) {
        final HumanoidArm mainArm = player.getMainArm();

        if (InstrumentOpenProvider.isItem(player) && InstrumentOpenProvider.getHand(player) == InteractionHand.OFF_HAND)
            return mainArm;

        return mainArm.getOpposite();
    }


    /**
     * Moves the given player's fade in the given map towards whether it's on
     * @return How far along the fade is now, from 0 to 1
     */
    private static double updateFade(final Map<UUID, Fade> fades, final Player player, final boolean on,
                                     final double fadeInTime, final double fadeOutTime) {
        Fade fade = fades.get(player.getUUID());

        if (fade == null) {
            if (!on)
                return 0;

            fades.put(player.getUUID(), fade = new Fade());
        }

        final double now = now();
        final double step = (now - fade.updatedAt) / (on ? fadeInTime : fadeOutTime);
        fade.updatedAt = now;
        fade.amount = Mth.clamp(fade.amount + (on ? step : -step), 0, 1);

        if (fade.amount <= 0)
            fades.remove(player.getUUID());

        return fade.amount;
    }

    /**
     * Eases a fade so that what it fades starts and settles softly
     */
    private static float ease(final double fade) {
        return (float) (fade * fade * (3 - 2 * fade));
    }

    /**
     * @return How far along their sway a player of the given age in ticks is, in radians
     */
    private static float swayPhase(final float ageInTicks) {
        return ageInTicks / 20f / SWAY_PERIOD * Mth.TWO_PI;
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
