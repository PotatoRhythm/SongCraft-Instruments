package com.stump.songcraft_instruments.client.gw2bass;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import com.stump.songcraft_instruments.client.ModArmPose;
import com.stump.songcraft_instruments.client.gui.instrument.gw2_bass.Gw2BassScreen;
import com.stump.songcraft_instruments.event.NoteSoundPlayedEvent;
import com.stump.songcraft_instruments.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Quaternionf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <p>Moves players like they're really playing the GW2 bass.</p>
 * The bass hangs still across their body while their arms play it: the fretting hand holds the neck and slides
 * along it to each note, up towards the headstock for low notes and down towards the body for high ones,
 * and the plucking hand pulls through the strings with every note, alternating fingers.
 * The string the note's on rings out after, the lowest notes on the lowest string and the highest on the highest.
 * While playing, players settle into a groove, nodding their head and rocking to a beat that locks onto the
 * rhythm of their notes, swaying to each side over 2 beats, leaning back a little with their head cocked towards the neck.
 * Everything settles back to standing still once they've stopped playing for a moment.
 * <p>
 * The arm poses were worked out from the bass's model and display settings, so that the hands land on the
 * neck and strings. The bass is held in the plucking hand, so that hand's arm is
 * {@link #anchorHeldBass anchored} for the bass to stay put while the arm plays.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public class Gw2BassPlaying {
    // Angles are in radians for model parts and degrees for the whole body, and timings are in seconds

    /** Where the bass is held, as the rotation of the arm holding it, for it to hang across the body */
    private static final float[] RIGHT_ANCHOR = {-0.15f, 0.2f, 0}, LEFT_ANCHOR = {-0.15f, -0.2f, 0};
    /**
     * Where the plucking arm rests over the strings, when the bass is held in the right and left hand.
     * It plucks about a fifth of the way up the strings from the bridge, over the bass's body.
     */
    private static final float[] RIGHT_PLUCK = {-1.056f, 0, -0.393f}, LEFT_PLUCK = {-0.887f, 0, 0.304f};
    /**
     * The fretting arm's rotations along the neck, evenly spaced from the lowest note up near the headstock
     * to the highest down near the body, when the bass is held in the right and left hand.
     * The hand rests against the neck from behind, wrapping around it, rather than having the neck run through it.
     */
    private static final float[][]
        RIGHT_FRETS = {
            {-1.880f, -0.308f, 0.456f},
            {-1.725f, -0.261f, 0.388f},
            {-1.615f, -0.226f, 0.359f},
            {-1.510f, -0.180f, 0.384f},
            {-1.398f, -0.132f, 0.453f},
            {-1.297f, -0.106f, 0.546f},
            {-1.204f, -0.081f, 0.649f}
        },
        LEFT_FRETS = {
            {-1.629f, 0.416f, -0.232f},
            {-1.526f, 0.337f, -0.283f},
            {-1.425f, 0.280f, -0.339f},
            {-1.335f, 0.250f, -0.397f},
            {-1.244f, 0.221f, -0.459f},
            {-1.147f, 0.194f, -0.522f},
            {-1.046f, 0.173f, -0.581f}
        };

    /** Where along the neck the fretting hand rests before the first note, from 0 lowest to 1 highest */
    private static final float REST_HEIGHT = 0.3f;
    /** How long the fretting hand takes to slide to a note */
    private static final double FRET_SLIDE_TIME = 0.09;

    /** How far the plucking arm pulls through the strings on a note, and how far apart its two fingers pluck */
    private static final float PLUCK_PULL = 0.09f, FINGER_SPREAD = 0.035f;
    /** How fast a pluck comes and goes */
    private static final double PLUCK_ATTACK_TIME = 0.03, PLUCK_DECAY_TIME = 0.13;
    /** How long after a note the next one gets its full pluck, so that fast runs flow instead of jittering */
    private static final double PLUCK_RECOVERY_TIME = 0.25;
    /** How far the plucking hand bobs along with the groove */
    private static final float PLUCK_GROOVE = 0.025f;

    /** How many strings the bass has, which share its notes evenly between them from lowest to highest */
    public static final int STRINGS = 4;
    /**
     * How far each string swings across the fretboard as it's plucked, in pixels, how many times a second,
     * and how long its ringing takes to die down, from the lowest string to the highest.
     * Lower strings swing wider and slower, and ring for longer.
     * The strings are only about 0.2 pixels apart, so swinging any wider would have them blur into each other.
     */
    private static final float[] STRING_SWING = {0.1325f, 0.1175f, 0.1025f, 0.0875f}, STRING_RATE = {11, 13, 15.5f, 18};
    private static final double[] STRING_RING_TIME = {1.0, 0.9, 0.8, 0.7};

    /** The beat players groove to before they've played enough notes to tell, and the range beats are folded into */
    private static final double DEFAULT_BEAT = 0.6, SHORTEST_BEAT = 0.4, LONGEST_BEAT = 0.9;
    /** How much of the way each note's timing pulls the beat's length and timing to it */
    private static final double TEMPO_FOLLOW = 0.3, BEAT_LOCK = 0.5;
    /** How long the beat takes to shift onto a note's timing */
    private static final double BEAT_SHIFT_TIME = 0.15;
    /** Notes further apart than this start a new phrase, rather than telling the beat */
    private static final double PHRASE_GAP = 2;
    /** Notes closer together than this are played together, as a chord, and don't tell the beat */
    private static final double CHORD_GAP = 0.06;

    /** How far players nod their head down on every beat, and how much further on a note */
    private static final float HEAD_NOD = 0.14f, HEAD_ACCENT_NOD = 0.05f;
    /** How far players look down at, turn towards and cock their head towards the neck while playing */
    private static final float HEAD_LOOK_DOWN = 0.1f, HEAD_TURN = 0.2f, HEAD_TILT = 0.1f;

    /** How far players lean back while playing, rock forward on every beat, and sway to each side over 2 beats */
    private static final float LEAN_BACK = 2f, GROOVE_ROCK = 1.6f, SWAY_ANGLE = 2f;

    /** How long the motion takes to fade in as players start playing, and to settle once they stop */
    private static final double FADE_IN_TIME = 0.3, FADE_OUT_TIME = 2.4;
    /** How long players keep grooving after their last note */
    private static final double KEEP_GROOVING_TIME = 3.2;

    /** The semitones of the 7 natural notes of an octave, as laid out on a note grid row */
    private static final int[] NATURAL_NOTE_PITCHES = {0, 2, 4, 5, 7, 9, 11};
    /** The semitones the bass's note grid spans */
    private static final int GRID_SEMITONES = 2 * 12;

    private static final Map<UUID, Playing> PLAYING = new HashMap<>();

    /** Whether the player being rendered was moved, for its pose to be restored after */
    private static boolean moved;
    /**
     * The model whose head was cocked, for it to be straightened back up after.
     * Nothing else resets a head's roll, and the model is shared by all players.
     */
    private static HumanoidModel<?> cockedModel;

    /** The entity whose arms were last posed */
    private static LivingEntity posedEntity;

    private static final class Playing {
        double lastNoteAt = Double.NEGATIVE_INFINITY;
        /** How much players are moving, from 0 standing still to 1 playing */
        double activity;
        double updatedAt = now();

        /** How long a beat is, and how far into the current one players are, from 0 to 1 */
        double beat = DEFAULT_BEAT, beatPhase;
        /** How much of a beat the beat still has to shift by, to land on the notes */
        double beatShift;
        /** How many beats have gone by, for the sway to span 2 of them */
        long beats;

        /** Where along the neck the fretting hand is sliding from and to, from 0 lowest to 1 highest */
        float fretFrom = REST_HEIGHT, fretTo = REST_HEIGHT;
        double slidAt = Double.NEGATIVE_INFINITY;

        double pluckAt = Double.NEGATIVE_INFINITY;
        float pluckStrength, pluckFrom;
        /** Which finger plucks, 1 or -1 */
        int finger = 1;

        /** When each string was last plucked, and how hard */
        final double[] stringPluckedAt = new double[STRINGS];
        final float[] stringStrength = new float[STRINGS];

        Playing() {
            Arrays.fill(stringPluckedAt, Double.NEGATIVE_INFINITY);
        }

        // Worked out as the player is rendered, for their model to be posed by
        float fret = REST_HEIGHT, pluck, fingerShift, groove, nod, engaged;
    }


    @SubscribeEvent
    public static void onNoteSoundPlayed(final NoteSoundPlayedEvent event) {
        if (!event.level().isClientSide || !event.isByPlayer()
            || !Gw2BassScreen.INSTRUMENT_ID.equals(event.soundMeta().instrumentId()))
            return;

        final Playing playing = PLAYING.computeIfAbsent(event.entityInfo().get().entity.getUUID(), (uuid) -> new Playing());
        final double now = now();
        update(playing, now);

        final double sinceLastNote = now - playing.lastNoteAt;
        if (sinceLastNote >= CHORD_GAP)
            followRhythm(playing, sinceLastNote);
        playing.lastNoteAt = now;

        final int index = event.sound().index;
        final int semitone = index / NATURAL_NOTE_PITCHES.length * 12
            + NATURAL_NOTE_PITCHES[index % NATURAL_NOTE_PITCHES.length] + event.soundMeta().pitch();

        playing.fretFrom = playing.fret;
        playing.fretTo = Mth.clamp(semitone / (GRID_SEMITONES - 1f), 0, 1);
        playing.slidAt = now;

        // Pick up from wherever the last pluck is, rather than snapping back from nothing
        playing.pluckFrom = pluck(playing, now);
        playing.pluckStrength = Math.max(playing.pluckFrom,
            event.volume() * (float) Mth.clamp((now - playing.pluckAt) / PLUCK_RECOVERY_TIME, 0.3, 1));
        playing.pluckAt = now;
        playing.finger = -playing.finger;

        final int string = Math.min((int) (playing.fretTo * STRINGS), STRINGS - 1);
        playing.stringPluckedAt[string] = now;
        playing.stringStrength[string] = event.volume();
    }

    /**
     * Pulls the beat towards a note played the given time after the last one, so that players groove to their own rhythm
     */
    private static void followRhythm(final Playing playing, final double sinceLastNote) {
        final double offBeat = offBeat(playing);

        if (sinceLastNote > PHRASE_GAP) {
            // A fresh start; land the beat right on it
            playing.beatShift -= offBeat;
            return;
        }

        // Fold the gap into a beat that's comfortable to nod to
        double gap = sinceLastNote;
        while (gap < SHORTEST_BEAT)
            gap *= 2;
        while (gap > LONGEST_BEAT)
            gap /= 2;
        playing.beat = Mth.lerp(TEMPO_FOLLOW, playing.beat, gap);

        // Nudge the beat towards the note, if the note's anywhere near a beat at all
        if (Math.abs(offBeat) < 0.3)
            playing.beatShift -= offBeat * BEAT_LOCK;
    }

    /**
     * @return How far off the beat the given player is right now, with whatever the beat is still shifting by,
     * from -0.5 to 0.5 of a beat
     */
    private static double offBeat(final Playing playing) {
        final double phase = playing.beatPhase + playing.beatShift;
        return phase - Math.floor(phase + 0.5);
    }


    // Lowest priority, so that no other listener cancels the render after the player was moved
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPreRenderPlayer(final RenderPlayerEvent.Pre event) {
        straightenHead();

        final Player player = event.getEntity();
        final Playing playing = PLAYING.get(player.getUUID());
        if (playing == null)
            return;

        if (!hasBassOpen(player)) {
            PLAYING.remove(player.getUUID());
            return;
        }

        final double now = now();
        update(playing, now);

        final float slide = easeOut(Math.min((float) ((now - playing.slidAt) / FRET_SLIDE_TIME), 1));
        playing.fret = Mth.lerp(slide, playing.fretFrom, playing.fretTo);

        final float activity = ease(playing.activity);
        // Nod down sharply on the beat and ease back up through it
        final float onBeat = (float) Math.pow(0.5 + 0.5 * Math.cos(playing.beatPhase * Mth.TWO_PI), 2);
        final float pluck = pluck(playing, now);

        playing.engaged = activity;
        playing.groove = activity * onBeat;
        playing.pluck = pluck;
        playing.fingerShift = playing.finger * pluck;
        playing.nod = activity * (HEAD_NOD * onBeat + HEAD_ACCENT_NOD * pluck);

        if (activity <= 0)
            return;

        // Sway over every 4 beats, swinging out to one side and back over 2 of them, then the other side over the next 2
        final float sway = activity * SWAY_ANGLE
            * Mth.sin((float) (((playing.beats & 3) + playing.beatPhase) * Math.PI / 2));
        final float leanBack = activity * LEAN_BACK - GROOVE_ROCK * playing.groove;

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
        straightenHead();

        if (!moved)
            return;

        event.getPoseStack().popPose();
        moved = false;
    }

    @SubscribeEvent
    public static void onLevelUnload(final LevelEvent.Unload event) {
        PLAYING.clear();
        posedEntity = null;
        cockedModel = null;
    }


    /**
     * Puts the given player's hands on the bass they're playing, and moves their arms and head along with their playing
     * @param holdingArm The arm holding the bass, which plucks it
     */
    public static void poseArms(final HumanoidModel<?> model, final LivingEntity entity, final HumanoidArm holdingArm) {
        final Playing playing = PLAYING.get(entity.getUUID());
        final boolean right = holdingArm == HumanoidArm.RIGHT;

        final float fret = (playing == null) ? REST_HEIGHT : playing.fret;
        final float pluck = (playing == null) ? 0 : playing.pluck;
        final float fingerShift = (playing == null) ? 0 : playing.fingerShift;
        final float groove = (playing == null) ? 0 : playing.groove;

        // The fretting hand holds the neck
        final ModelPart fretArm = right ? model.leftArm : model.rightArm;
        final float[][] frets = right ? RIGHT_FRETS : LEFT_FRETS;
        final float along = fret * (frets.length - 1);
        final int from = Math.min((int) along, frets.length - 2);
        fretArm.xRot = Mth.lerp(along - from, frets[from][0], frets[from + 1][0]);
        fretArm.yRot = Mth.lerp(along - from, frets[from][1], frets[from + 1][1]);
        fretArm.zRot = Mth.lerp(along - from, frets[from][2], frets[from + 1][2]);

        // The plucking hand pulls through the strings, its fingers taking turns
        final ModelPart pluckArm = right ? model.rightArm : model.leftArm;
        final float[] pluckRest = right ? RIGHT_PLUCK : LEFT_PLUCK;
        final float outwards = right ? -1 : 1;
        pluckArm.xRot = pluckRest[0] + PLUCK_PULL * pluck + PLUCK_GROOVE * groove;
        pluckArm.yRot = pluckRest[1];
        pluckArm.zRot = pluckRest[2] + outwards * FINGER_SPREAD * fingerShift;

        posedEntity = entity;

        // Vanilla sways both arms gently once they're posed, which would rock the bass into the fretting hand
        final float age = entity.tickCount + Minecraft.getInstance().getFrameTime();
        cancelIdleSway(model.rightArm, age, 1);
        cancelIdleSway(model.leftArm, age, -1);

        if (playing == null)
            return;

        // Groove along with the head, looking down towards the neck.
        // The neck is on the side of the fretting arm, which turning towards takes a negative yaw from the right.
        final float towardsNeck = right ? -1 : 1;
        model.head.xRot += playing.nod + HEAD_LOOK_DOWN * playing.engaged;
        model.head.yRot += towardsNeck * HEAD_TURN * playing.engaged;
        // The roll is set rather than added onto, as nothing resets it between frames
        model.head.zRot = -towardsNeck * HEAD_TILT * playing.engaged;
        cockedModel = model;
    }

    /**
     * Takes away ahead of time the idle sway vanilla adds onto a posed arm
     * (see {@link net.minecraft.client.model.AnimationUtils#bobModelPart}), so that the arm ends up where it was posed
     * @param side 1 for the right arm, -1 for the left
     */
    private static void cancelIdleSway(final ModelPart arm, final float age, final float side) {
        arm.zRot -= side * (Mth.cos(age * 0.09f) * 0.05f + 0.05f);
        arm.xRot -= side * Mth.sin(age * 0.067f) * 0.05f;
    }

    /**
     * Keeps a bass that's being played where it hangs across the player's body, rather than letting it follow the
     * plucking arm holding it around.
     * Called by the held item layer once it moved to the given arm's hand, before it renders the item there.
     */
    public static void anchorHeldBass(final EntityModel<?> parentModel, final LivingEntity entity,
                                      final HumanoidArm arm, final PoseStack poseStack) {
        if (entity != posedEntity || !(parentModel instanceof HumanoidModel<?> model))
            return;

        final boolean right = arm == HumanoidArm.RIGHT;
        if ((right ? model.rightArmPose : model.leftArmPose) != ModArmPose.PLAYING_GW2_BASS)
            return;

        // Undo the arm's own rotation and take on the anchor's instead.
        // The arm's rotation is left out altogether, rather than offset from, since other mods may move it after
        // it was posed: Not Enough Animations eases arms towards their pose, which would have the bass twitch with
        // every pluck.
        final ModelPart part = right ? model.rightArm : model.leftArm;
        final float[] anchor = right ? RIGHT_ANCHOR : LEFT_ANCHOR;
        poseStack.mulPose(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot).conjugate());
        poseStack.mulPose(new Quaternionf().rotationZYX(anchor[2], anchor[1], anchor[0]));
    }


    /**
     * @param stack The bass being rendered
     * @param string Which of its strings, from 0 for the lowest
     * @return How far the given string of the given bass is swung across the fretboard right now, in pixels.
     * Only a bass being played in a player's hands vibrates.
     */
    public static float stringVibration(final ItemStack stack, final ItemDisplayContext context, final int string) {
        if ((context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) && (context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND))
            return 0;
        // Only the player whose arms were just posed is having their bass rendered in hand
        if ((posedEntity == null) || ((posedEntity.getMainHandItem() != stack) && (posedEntity.getOffhandItem() != stack)))
            return 0;

        final Playing playing = PLAYING.get(posedEntity.getUUID());
        if (playing == null)
            return 0;

        final double time = now() - playing.stringPluckedAt[string];
        final double ring = playing.stringStrength[string] * Math.exp(-time / STRING_RING_TIME[string]);
        if (ring < 0.01)
            return 0;

        return (float) (ring * STRING_SWING[string] * Math.sin(time * STRING_RATE[string] * Math.PI * 2));
    }

    /**
     * Straightens back up the head of whichever model was last cocked, so that no other player rendered with it
     * keeps the tilt
     */
    private static void straightenHead() {
        if (cockedModel == null)
            return;

        cockedModel.head.zRot = 0;
        cockedModel.hat.zRot = 0;
        cockedModel = null;
    }

    /**
     * Moves the given player's motion and beat along to now
     */
    private static void update(final Playing playing, final double now) {
        final double delta = Math.max(0, now - playing.updatedAt);
        playing.updatedAt = now;

        final boolean active = now - playing.lastNoteAt < KEEP_GROOVING_TIME;
        final double fadeStep = delta / (active ? FADE_IN_TIME : FADE_OUT_TIME);
        playing.activity = Mth.clamp(playing.activity + (active ? fadeStep : -fadeStep), 0, 1);

        // Shift onto the notes gradually, so the nodding doesn't jump
        final double shift = playing.beatShift * (1 - Math.exp(-delta / BEAT_SHIFT_TIME));
        playing.beatShift -= shift;
        playing.beatPhase += delta / playing.beat + shift;
        while (playing.beatPhase >= 1) {
            playing.beatPhase -= 1;
            playing.beats++;
        }
        while (playing.beatPhase < 0) {
            playing.beatPhase += 1;
            playing.beats--;
        }
    }

    /**
     * @return How far into its pluck the plucking hand is, from 0 to the latest note's volume
     */
    private static float pluck(final Playing playing, final double now) {
        final double time = now - playing.pluckAt;
        if (time < PLUCK_ATTACK_TIME) {
            final double t = time / PLUCK_ATTACK_TIME;
            return (float) Mth.lerp(t * t * (3 - 2 * t), playing.pluckFrom, playing.pluckStrength);
        }

        return (float) (playing.pluckStrength * Math.exp(-(time - PLUCK_ATTACK_TIME) / PLUCK_DECAY_TIME));
    }

    /**
     * @return Whether the given player has a GW2 bass open in their hand
     */
    private static boolean hasBassOpen(final Player player) {
        return InstrumentOpenProvider.isOpen(player) && InstrumentOpenProvider.isItem(player)
            && (InstrumentOpenProvider.getHand(player) != null)
            && player.getItemInHand(InstrumentOpenProvider.getHand(player)).is(ModItems.GW2_BASS.get());
    }


    /**
     * Eases a fade so that what it fades starts and settles softly
     */
    private static float ease(final double fade) {
        return (float) (fade * fade * (3 - 2 * fade));
    }
    private static float easeOut(final float t) {
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    private static double now() {
        return System.nanoTime() / 1e9;
    }
}
