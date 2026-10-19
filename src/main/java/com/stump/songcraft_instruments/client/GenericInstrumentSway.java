package com.stump.songcraft_instruments.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.client.gui.instrument.animalcall.AnimalCallScreen;
import com.stump.songcraft_instruments.client.gui.instrument.flute.FluteScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_bass.Gw2BassScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_flute.Gw2FluteScreen;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_horn.Gw2HornScreen;
import com.stump.songcraft_instruments.client.gui.instrument.microphone.MicrophoneScreen;
import com.stump.songcraft_instruments.client.gui.instrument.microphone.MicrophoneStandScreen;
import com.stump.songcraft_instruments.client.gui.instrument.nightwind_horn.NightwindHornScreen;
import com.stump.songcraft_instruments.client.gui.instrument.ocarina.OcarinaScreen;
import com.stump.songcraft_instruments.client.gui.instrument.saxophone.SaxophoneScreen;
import com.stump.songcraft_instruments.client.gui.instrument.trombone.TromboneScreen;
import com.stump.songcraft_instruments.client.gui.instrument.violin.ViolinScreen;
import com.stump.songcraft_instruments.client.gui.instrument.xiao.XiaoScreen;
import com.stump.songcraft_instruments.event.HeldNoteSoundPlayedEvent;
import com.stump.songcraft_instruments.event.InstrumentPlayedEvent;
import com.stump.songcraft_instruments.event.NoteSoundPlayedEvent;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
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
import java.util.Set;
import java.util.UUID;

/**
 * <p>Sways players gently from side to side while they play any instrument that doesn't move them its own way,
 * with their head bobbing and tilting very slightly along.</p>
 * Instruments played with the mouth or chin keep the head still, so that it stays on the instrument.
 * Everything settles back to standing still once they've stopped playing for a moment.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public class GenericInstrumentSway {
    // Angles are in degrees for the whole body and radians for the head, and timings are in seconds

    /** How far players lean to each side, and how long a full sway to one side and back takes */
    private static final float SWAY_ANGLE = 2.2f, SWAY_PERIOD = 2.6f;
    /** How far players bob up as they lean out to each side, in blocks */
    private static final float BOB_HEIGHT = 0.012f;

    /**
     * How far players dip their head and tilt it along with the sway.
     * Both go round once every sway, so the head rolls slowly round in time with the body.
     */
    private static final float HEAD_NOD = 0.04f, HEAD_TILT = 0.05f;
    /** How far behind the sway the head follows, in radians of the sway, so it trails the body a little */
    private static final float HEAD_TILT_LAG = 0.6f;

    /** How long the motion takes to fade in as players start playing, and to settle once they stop */
    private static final double FADE_IN_TIME = 0.5, FADE_OUT_TIME = 1.2;
    /** How long players keep swaying after their last note ends */
    private static final double KEEP_SWAYING_TIME = 1.0;

    /** Instruments that move players their own way, which this leaves alone */
    private static final Set<ResourceLocation> OWN_ANIMATION = Set.of(
        MicrophoneScreen.INSTRUMENT_ID,
        MicrophoneStandScreen.INSTRUMENT_ID,
        SaxophoneScreen.INSTRUMENT_ID,
        Gw2BassScreen.INSTRUMENT_ID
    );
    /** Instruments played with the mouth or chin, whose players keep their head still */
    private static final Set<ResourceLocation> STILL_HEAD = Set.of(
        FluteScreen.INSTRUMENT_ID,
        XiaoScreen.INSTRUMENT_ID,
        OcarinaScreen.INSTRUMENT_ID,
        AnimalCallScreen.INSTRUMENT_ID,
        TromboneScreen.INSTRUMENT_ID,
        NightwindHornScreen.INSTRUMENT_ID,
        Gw2FluteScreen.INSTRUMENT_ID,
        Gw2HornScreen.INSTRUMENT_ID,
        ViolinScreen.INSTRUMENT_ID
    );

    private static final Map<UUID, Playing> PLAYING = new HashMap<>();

    /** Whether the player being rendered was swayed, for its pose to be restored after */
    private static boolean swayed;
    /** The player being rendered, and how far their head nods and tilts, for their model to be posed by */
    private static Player renderedPlayer;
    private static float renderedNod, renderedTilt;
    /**
     * The model whose head was tilted, for it to be straightened back up after.
     * Nothing else resets a head's roll, and the model is shared by all players.
     */
    private static HumanoidModel<?> tiltedModel;

    private static final class Playing {
        int heldNotes;
        double lastNoteAt = now();
        /** Whether the latest note's instrument moves the head */
        boolean moveHead;

        /** How much players are swaying, from 0 standing still to 1 swaying fully */
        double activity;
        double updatedAt = now();
    }


    @SubscribeEvent
    public static void onNoteSoundPlayed(final NoteSoundPlayedEvent event) {
        getPlaying(event).ifPresent((playing) -> playing.lastNoteAt = now());
    }

    @SubscribeEvent
    public static void onHeldNoteSoundPlayed(final HeldNoteSoundPlayedEvent event) {
        getPlaying(event).ifPresent((playing) -> {
            if (event.phase == HeldSoundPhase.ATTACK)
                playing.heldNotes++;
            else if (playing.heldNotes > 0)
                playing.heldNotes--;

            playing.lastNoteAt = now();
        });
    }

    /**
     * @return How the player that played the event's note is playing, if they played it on an instrument
     * without its own animation
     */
    private static Optional<Playing> getPlaying(final InstrumentPlayedEvent<?> event) {
        if (!event.level().isClientSide || !event.isByPlayer())
            return Optional.empty();

        final ResourceLocation instrumentId = event.soundMeta().instrumentId();
        if (OWN_ANIMATION.contains(instrumentId))
            return Optional.empty();

        final Playing playing = PLAYING.computeIfAbsent(event.entityInfo().get().entity.getUUID(), (uuid) -> new Playing());
        playing.moveHead = !STILL_HEAD.contains(instrumentId);
        return Optional.of(playing);
    }


    // Lowest priority, so that no other listener cancels the render after the player was swayed
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPreRenderPlayer(final RenderPlayerEvent.Pre event) {
        straightenHead();
        renderedPlayer = null;

        final Player player = event.getEntity();
        final Playing playing = PLAYING.get(player.getUUID());
        if (playing == null)
            return;

        if (!InstrumentOpenProvider.isOpen(player)) {
            // Forget notes cut off by closing the instrument
            PLAYING.remove(player.getUUID());
            return;
        }

        update(playing, now());

        final float activity = ease(playing.activity);
        if (activity <= 0) {
            if (playing.heldNotes == 0)
                PLAYING.remove(player.getUUID());
            return;
        }

        final float phase = (player.tickCount + event.getPartialTick()) / 20f / SWAY_PERIOD * Mth.TWO_PI;
        final float sway = Mth.sin(phase);

        if (playing.moveHead) {
            renderedPlayer = player;
            final float headPhase = phase - HEAD_TILT_LAG;
            renderedNod = activity * HEAD_NOD * (1 - Mth.cos(headPhase)) / 2;
            renderedTilt = activity * HEAD_TILT * Mth.sin(headPhase);
        }

        final PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        swayed = true;

        // Bob up at either end of the sway, then lean sideways about the player's feet, facing the way their body does
        poseStack.translate(0, sway * sway * BOB_HEIGHT * activity, 0);

        final float bodyRot = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sway * SWAY_ANGLE * activity));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRot));
    }

    @SubscribeEvent
    public static void onPostRenderPlayer(final RenderPlayerEvent.Post event) {
        straightenHead();
        renderedPlayer = null;

        if (!swayed)
            return;

        event.getPoseStack().popPose();
        swayed = false;
    }

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        PLAYING.clear();
        renderedPlayer = null;
        tiltedModel = null;
    }


    /**
     * Nods and tilts the head of the player being rendered along with their sway.
     * Called once their model has been posed for rendering.
     */
    public static void moveHead(final HumanoidModel<?> model, final LivingEntity entity) {
        if (entity != renderedPlayer)
            return;

        model.head.xRot += renderedNod;
        // The roll is set rather than added onto, as nothing resets it between frames
        model.head.zRot = renderedTilt;
        model.hat.copyFrom(model.head);
        tiltedModel = model;
    }

    /**
     * Straightens back up the head of whichever model was last tilted, so that no other player rendered with it
     * keeps the tilt
     */
    private static void straightenHead() {
        if (tiltedModel == null)
            return;

        tiltedModel.head.zRot = 0;
        tiltedModel.hat.zRot = 0;
        tiltedModel = null;
    }


    /**
     * Moves the given player's sway along to now
     */
    private static void update(final Playing playing, final double now) {
        final double delta = Math.max(0, now - playing.updatedAt);
        playing.updatedAt = now;

        final boolean active = (playing.heldNotes > 0) || (now - playing.lastNoteAt < KEEP_SWAYING_TIME);
        final double fadeStep = delta / (active ? FADE_IN_TIME : FADE_OUT_TIME);
        playing.activity = Mth.clamp(playing.activity + (active ? fadeStep : -fadeStep), 0, 1);
    }

    /**
     * Eases a fade so that what it fades starts and settles softly
     */
    private static float ease(final double fade) {
        return (float) (fade * fade * (3 - 2 * fade));
    }

    private static double now() {
        return System.nanoTime() / 1e9;
    }
}
