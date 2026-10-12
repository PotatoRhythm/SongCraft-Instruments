package com.stump.songcraft_instruments.client.saxophone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.client.config.enumType.SaxophoneSoundType;
import com.stump.songcraft_instruments.client.gui.instrument.saxophone.SaxophoneScreen;
import com.stump.songcraft_instruments.event.HeldNoteSoundPlayedEvent;
import com.stump.songcraft_instruments.event.InstrumentPlayedEvent;
import com.stump.songcraft_instruments.event.NoteSoundPlayedEvent;
import com.stump.songcraft_instruments.item.ModItems;
import com.stump.songcraft_instruments.networking.packet.instrument.util.HeldSoundPhase;
import com.stump.songcraft_instruments.sound.held.HeldNoteSound;
import net.minecraft.client.model.HumanoidModel;
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
import java.util.UUID;

/**
 * <p>Moves players like they're really playing the saxophone.</p>
 * While playing, they rock gently into a groove. They lean back to reach for high notes, all the more the higher
 * they go, and further still into long held ones,
 * and hunch over the horn for low ones. Every note they start dips the horn a touch, as if tonguing it,
 * and their hands shift a little between notes, as if fingering them.
 * Everything settles back to standing still once they've stopped playing for a moment.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public class SaxophonePlaying {
    // All angles are in degrees for the body and radians for the arms, and all timings are in seconds

    /** How far players sway to each side, and how long a full sway to one side and back takes */
    private static final float SWAY_ANGLE = 2f, SWAY_PERIOD = 2.6f;
    /** How far players rock forward and back, twice every sway */
    private static final float ROCK_ANGLE = 1.2f;

    /**
     * How far players lean back for their highest notes, when just tapped and when held out,
     * and forward (negative) for their lowest ones.
     * The lean builds up towards the top of the range, so middle notes stand about upright.
     */
    private static final float TAPPED_HIGH_NOTE_LEAN = 3.5f, HELD_HIGH_NOTE_LEAN = 13f, LOW_NOTE_LEAN = -2f;
    /** How long players take to follow their notes' heights */
    private static final double HEIGHT_FOLLOW_TIME = 0.4;
    /**
     * How far players lean back into held notes of any height, and how long they take to lean into and out of them.
     * Taps are let go of long before players get to lean into them.
     */
    private static final float HOLD_LEAN = 2f;
    private static final double HOLD_LEAN_TIME = 0.8;

    /** How far players tip forward as they start a note, and how fast the tip comes and goes */
    private static final float ACCENT_LEAN = 1.8f;
    private static final double ACCENT_ATTACK_TIME = 0.07, ACCENT_DECAY_TIME = 0.22;
    /**
     * How long after a note the next one gets its full accent. Notes played quicker than this get softer accents,
     * so that fast runs flow instead of jittering.
     */
    private static final double ACCENT_RECOVERY_TIME = 0.35;
    /** How far the horn dips as players start a note */
    private static final float ACCENT_ARM_DIP = 0.07f;
    /** How far players raise the horn for their highest notes, and lower it for their lowest */
    private static final float HEIGHT_ARM_LIFT = 0.06f;
    /** How far players raise the horn into held notes */
    private static final float HOLD_ARM_LIFT = 0.05f;

    /** How far each hand shifts between notes, and how quickly */
    private static final float UPPER_HAND_SHIFT = 0.035f, LOWER_HAND_SHIFT = 0.018f;
    private static final double HAND_SHIFT_TIME = 0.12;

    /** How long the motion takes to fade in as players start playing, and to settle once they stop */
    private static final double FADE_IN_TIME = 0.3, FADE_OUT_TIME = 1.0;
    /** How long players keep moving after their last note ends */
    private static final double KEEP_MOVING_TIME = 0.75;

    /** The semitones of the 7 natural notes of an octave, as laid out on a note grid row */
    private static final int[] NATURAL_NOTE_PITCHES = {0, 2, 4, 5, 7, 9, 11};
    /** The semitones a saxophone's note grid spans when its voice can't be told */
    private static final int DEFAULT_GRID_SEMITONES = 3 * 12;

    private static final Map<UUID, Playing> PLAYING = new HashMap<>();

    /** Whether the player being rendered was moved, for its pose to be restored after */
    private static boolean moved;

    private static final class Playing {
        int heldNotes;
        double lastNoteAt = Double.NEGATIVE_INFINITY;

        /** How high the latest note is, from 0 for the lowest to 1 for the highest, and how high players lean for it */
        double noteHeight = 0.5, height = 0.5;
        /** How far players are leaning into held notes, from 0 to 1 */
        double holding;
        /** How much players are moving, from 0 standing still to 1 playing */
        double activity;
        double updatedAt = now();

        double accentAt = Double.NEGATIVE_INFINITY;
        float accentStrength, accentFrom;

        /** Where each hand is shifting from and to, as fractions of its full shift */
        float upperHandFrom, upperHandTo, lowerHandFrom, lowerHandTo;
        double handsShiftedAt = Double.NEGATIVE_INFINITY;

        /** How far the horn is raised, worked out as the player is rendered for their arms to follow */
        float armLift;
        float upperHand, lowerHand;
    }


    @SubscribeEvent
    public static void onNoteSoundPlayed(final NoteSoundPlayedEvent event) {
        getPlaying(event).ifPresent((playing) ->
            startNote(playing, event, event.sound().index, DEFAULT_GRID_SEMITONES)
        );
    }

    @SubscribeEvent
    public static void onHeldNoteSoundPlayed(final HeldNoteSoundPlayedEvent event) {
        getPlaying(event).ifPresent((playing) -> {
            if (event.phase == HeldSoundPhase.ATTACK) {
                startNote(playing, event, event.sound().index(), gridSemitones(event.sound()));
                playing.heldNotes++;
            } else if (playing.heldNotes > 0) {
                playing.heldNotes--;
            }

            playing.lastNoteAt = now();
        });
    }

    /**
     * @return How many semitones the note grid of the saxophone voice the given sound belongs to spans,
     * so that each voice's highest notes count as high
     */
    private static int gridSemitones(final HeldNoteSound sound) {
        for (final SaxophoneSoundType type : SaxophoneSoundType.values()) {
            final HeldNoteSound[] sounds = type.getSoundArr().get().getHeldSounds();

            if ((sounds != null) && (sounds.length > 0) && sounds[0].baseSoundLocation().equals(sound.baseSoundLocation()))
                return sounds.length / NATURAL_NOTE_PITCHES.length * 12;
        }

        return DEFAULT_GRID_SEMITONES;
    }

    private static void startNote(final Playing playing, final InstrumentPlayedEvent<?> event, final int soundIndex,
                                  final int gridSemitones) {
        final double now = now();
        final int row = soundIndex / NATURAL_NOTE_PITCHES.length;
        final int semitone = row * 12 + NATURAL_NOTE_PITCHES[soundIndex % NATURAL_NOTE_PITCHES.length]
            + event.soundMeta().pitch();

        playing.lastNoteAt = now;
        playing.noteHeight = Mth.clamp(semitone / (gridSemitones - 1f), 0, 1);

        // Pick up from wherever the last accent is, rather than snapping back up from nothing
        final double sinceLastAccent = now - playing.accentAt;
        playing.accentFrom = accent(playing, now);
        playing.accentStrength = Math.max(playing.accentFrom,
            event.volume() * (float) Mth.clamp(sinceLastAccent / ACCENT_RECOVERY_TIME, 0.2, 1));
        playing.accentAt = now;

        // Every note has its own fingering, so each hand settles somewhere of its own for it
        playing.upperHandFrom = playing.upperHand;
        playing.lowerHandFrom = playing.lowerHand;
        playing.upperHandTo = Math.floorMod(semitone * 5, 7) / 3f - 1;
        playing.lowerHandTo = Math.floorMod(semitone * 3 + 2, 5) / 2f - 1;
        playing.handsShiftedAt = now;
    }

    /**
     * @return How the player that played the event's note on a saxophone is playing, if one did
     */
    private static Optional<Playing> getPlaying(final InstrumentPlayedEvent<?> event) {
        if (!event.level().isClientSide || !event.isByPlayer()
            || !SaxophoneScreen.INSTRUMENT_ID.equals(event.soundMeta().instrumentId()))
            return Optional.empty();

        return Optional.of(PLAYING.computeIfAbsent(event.entityInfo().get().entity.getUUID(), (uuid) -> new Playing()));
    }


    // Lowest priority, so that no other listener cancels the render after the player was moved
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPreRenderPlayer(final RenderPlayerEvent.Pre event) {
        final Player player = event.getEntity();
        final Playing playing = PLAYING.get(player.getUUID());
        if (playing == null)
            return;

        if (!hasSaxophoneOpen(player)) {
            // Forget notes cut off by putting the saxophone away
            PLAYING.remove(player.getUUID());
            return;
        }

        final double now = now();
        update(playing, now);

        final float activity = ease(playing.activity);
        if (activity <= 0) {
            playing.armLift = playing.upperHand = playing.lowerHand = 0;
            return;
        }

        final float phase = (player.tickCount + event.getPartialTick()) / 20f / SWAY_PERIOD * Mth.TWO_PI;
        final float accent = accent(playing, now);

        final float leanBack = activity * (
            Mth.lerp((float) (playing.height * playing.height), LOW_NOTE_LEAN,
                Mth.lerp(ease(playing.holding), TAPPED_HIGH_NOTE_LEAN, HELD_HIGH_NOTE_LEAN))
            + HOLD_LEAN * ease(playing.holding)
            + ROCK_ANGLE * Mth.sin(phase * 2)
        ) - ACCENT_LEAN * accent;
        final float sway = activity * SWAY_ANGLE * Mth.sin(phase);

        playing.armLift = activity * (
            HEIGHT_ARM_LIFT * ((float) playing.height * 2 - 1)
            + HOLD_ARM_LIFT * ease(playing.holding)
        ) - ACCENT_ARM_DIP * accent;

        final float handShift = easeOut(Math.min((float) ((now - playing.handsShiftedAt) / HAND_SHIFT_TIME), 1));
        playing.upperHand = activity * UPPER_HAND_SHIFT * Mth.lerp(handShift, playing.upperHandFrom, playing.upperHandTo);
        playing.lowerHand = activity * LOWER_HAND_SHIFT * Mth.lerp(handShift, playing.lowerHandFrom, playing.lowerHandTo);

        final PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        moved = true;

        // Lean about the player's feet, facing the way their body does
        final float bodyRot = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(-leanBack));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sway));
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyRot));
    }

    @SubscribeEvent
    public static void onPostRenderPlayer(final RenderPlayerEvent.Post event) {
        if (!moved)
            return;

        event.getPoseStack().popPose();
        moved = false;
    }

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        PLAYING.clear();
    }


    /**
     * Raises and dips the horn and shifts the hands of the given saxophone player along with their playing.
     * Called once their arms have been put in the wind instrument pose.
     */
    public static void moveArms(final HumanoidModel<?> model, final LivingEntity entity) {
        final Playing playing = PLAYING.get(entity.getUUID());
        if (playing == null)
            return;

        // Both arms move together so the hands stay on the horn
        model.rightArm.xRot -= playing.armLift + playing.lowerHand;
        model.leftArm.xRot -= playing.armLift + playing.upperHand;
        model.leftArm.zRot += playing.upperHand * 0.5f;
    }


    /**
     * Moves the given player's motion along to now
     */
    private static void update(final Playing playing, final double now) {
        final double delta = Math.max(0, now - playing.updatedAt);
        playing.updatedAt = now;

        final boolean active = (playing.heldNotes > 0) || (now - playing.lastNoteAt < KEEP_MOVING_TIME);
        final double fadeStep = delta / (active ? FADE_IN_TIME : FADE_OUT_TIME);
        playing.activity = Mth.clamp(playing.activity + (active ? fadeStep : -fadeStep), 0, 1);

        playing.height = follow(playing.height, playing.noteHeight, delta, HEIGHT_FOLLOW_TIME);
        playing.holding = follow(playing.holding, (playing.heldNotes > 0) ? 1 : 0, delta, HOLD_LEAN_TIME);
    }

    /**
     * @return How strongly the latest note's start is tipping the player, from 0 to its volume
     */
    private static float accent(final Playing playing, final double now) {
        final double time = now - playing.accentAt;
        if (time < ACCENT_ATTACK_TIME) {
            final double t = time / ACCENT_ATTACK_TIME;
            return (float) Mth.lerp(t * t * (3 - 2 * t), playing.accentFrom, playing.accentStrength);
        }

        return (float) (playing.accentStrength * Math.exp(-(time - ACCENT_ATTACK_TIME) / ACCENT_DECAY_TIME));
    }

    /**
     * @return Whether the given player has a saxophone open in their hand
     */
    private static boolean hasSaxophoneOpen(final Player player) {
        return InstrumentOpenProvider.isOpen(player) && InstrumentOpenProvider.isItem(player)
            && (InstrumentOpenProvider.getHand(player) != null)
            && player.getItemInHand(InstrumentOpenProvider.getHand(player)).is(ModItems.SAXOPHONE.get());
    }


    /**
     * Eases a value towards a target, getting about two thirds of the way there every given time
     */
    private static double follow(final double value, final double target, final double delta, final double time) {
        return target + (value - target) * Math.exp(-delta / time);
    }

    /**
     * Eases a fade so that what it fades starts and settles softly
     */
    private static float ease(final double fade) {
        return (float) (fade * fade * (3 - 2 * fade));
    }
    private static float easeOut(final float t) {
        return 1 - (1 - t) * (1 - t);
    }

    private static double now() {
        return System.nanoTime() / 1e9;
    }
}
