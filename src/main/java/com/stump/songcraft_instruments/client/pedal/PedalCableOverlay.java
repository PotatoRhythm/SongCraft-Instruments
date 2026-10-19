package com.stump.songcraft_instruments.client.pedal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.PedalBlock;
import com.stump.songcraft_instruments.block.blockentity.PedalBlockEntity;
import com.stump.songcraft_instruments.item.InstrumentItem;
import com.stump.songcraft_instruments.item.SpeakerCableItem;
import com.stump.songcraft_instruments.pedal.InstrumentPedals;
import com.stump.songcraft_instruments.pedal.PedalChain;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * While the speaker cable or a pedal is held, draws the cables between chained pedals: dashes marching from each
 * pedal towards the one it's plugged into, so the signal's direction shows. Each chained pedal is numbered by its
 * place in its chain, counting from the first, with bypassed pedals' numbers greyed out.
 * <p>While a held instrument is plugged into a pedal, the cables its notes go through (from that pedal on) are green.</p>
 * <p>A held instrument shows them too, but only while it's pointed at a pedal, as it plugs in by right-clicking one,
 * and for a moment after (even if the instrument is put away), so they don't flicker while the cursor moves between
 * pedals.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(bus = Bus.FORGE, modid = SCInstrumentMod.MODID, value = Dist.CLIENT)
public final class PedalCableOverlay {
    /**
     * How far around the player (in chunks) pedals are looked for
     */
    private static final int CHUNK_RADIUS = 4;
    /**
     * The height above a pedal's block position its cables attach at: just above its top
     */
    public static final double CABLE_HEIGHT = 0.4;

    private static final double DASH_LENGTH = 0.3, GAP_LENGTH = 0.2, DASH_SPEED = 0.05;
    private static final int COLOR_LINK = 0x55CCFF, COLOR_PLUGGED = 0x55FF66, ALPHA_LINK = 0xC0;
    /**
     * How long (in milliseconds) the cables stay shown after a held instrument stops pointing at a pedal,
     * or is put away
     */
    private static final long INSTRUMENT_LINGER = 2000;
    private static final int BADGE_ON = 0xFFFFFF, BADGE_BYPASSED = 0x888888, BADGE_BACKGROUND = 0x60000000;

    /**
     * When a held instrument last pointed at a pedal, see {@link #INSTRUMENT_LINGER}
     */
    private static long instrumentLastOnPedal = 0;

    private PedalCableOverlay() {}

    @SubscribeEvent
    public static void onRenderLevel(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;

        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        final ClientLevel level = minecraft.level;
        if ((player == null) || (level == null) || !shouldShow(minecraft, player, level))
            return;

        final List<PedalBlockEntity> pedals = nearbyPedals(level, player.chunkPosition());
        if (pedals.isEmpty())
            return;

        final PoseStack pose = event.getPoseStack();
        final Vec3 camera = event.getCamera().getPosition();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final double dashOffset = ((level.getGameTime() + event.getPartialTick()) * DASH_SPEED)
            % (DASH_LENGTH + GAP_LENGTH);

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        final Set<BlockPos> plugged = heldInstrumentPath(player, level);
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (final PedalBlockEntity pedal : pedals) {
            final PedalBlockEntity target = downstreamOf(level, pedal);
            if (target != null)
                drawDashes(pose, lines, cablePoint(pedal.getBlockPos()), cablePoint(target.getBlockPos()), dashOffset,
                    plugged.contains(pedal.getBlockPos()) ? COLOR_PLUGGED : COLOR_LINK, ALPHA_LINK);
        }
        buffers.endBatch(RenderType.lines());

        final List<PedalBlockEntity> chained = new ArrayList<>();
        for (final PedalBlockEntity pedal : pedals)
            if ((downstreamOf(level, pedal) != null) || (upstreamOf(level, pedal) != null))
                chained.add(pedal);

        // Every background before any number: drawn in one batch, translucent quads are sorted by distance,
        // and a background sorted after its number would dim it depending on the camera angle
        final VertexConsumer backgrounds = buffers.getBuffer(RenderType.textBackgroundSeeThrough());
        for (final PedalBlockEntity pedal : chained)
            drawBadgeBackground(minecraft, pose, backgrounds, pedal, placeInChain(level, pedal));
        buffers.endBatch(RenderType.textBackgroundSeeThrough());

        for (final PedalBlockEntity pedal : chained)
            drawBadge(minecraft, pose, buffers, pedal, placeInChain(level, pedal));
        buffers.endBatch();

        pose.popPose();
    }


    private static boolean shouldShow(final Minecraft minecraft, final LocalPlayer player, final ClientLevel level) {
        final Item mainHand = player.getMainHandItem().getItem(), offhand = player.getOffhandItem().getItem();
        if (isCableOrPedal(mainHand) || isCableOrPedal(offhand))
            return true;
        final long now = Util.getMillis();
        if (((mainHand instanceof InstrumentItem) || (offhand instanceof InstrumentItem))
                && (minecraft.hitResult instanceof BlockHitResult hit) && (hit.getType() == HitResult.Type.BLOCK)
                && (pedalAt(level, hit.getBlockPos()) != null))
            instrumentLastOnPedal = now;
        return now - instrumentLastOnPedal < INSTRUMENT_LINGER;
    }
    private static boolean isCableOrPedal(final Item item) {
        return (item instanceof SpeakerCableItem) || ((item instanceof BlockItem block) && (block.getBlock() instanceof PedalBlock));
    }

    public static Vec3 cablePoint(final BlockPos pedal) {
        return new Vec3(pedal.getX() + 0.5, pedal.getY() + CABLE_HEIGHT, pedal.getZ() + 0.5);
    }


    //#region Finding pedals

    private static List<PedalBlockEntity> nearbyPedals(final ClientLevel level, final ChunkPos center) {
        final List<PedalBlockEntity> pedals = new ArrayList<>();
        for (int x = center.x - CHUNK_RADIUS; x <= center.x + CHUNK_RADIUS; x++) {
            for (int z = center.z - CHUNK_RADIUS; z <= center.z + CHUNK_RADIUS; z++) {
                final LevelChunk chunk = level.getChunkSource().getChunk(x, z, false);
                if (chunk == null)
                    continue;
                for (final BlockEntity blockEntity : chunk.getBlockEntities().values())
                    if (blockEntity instanceof PedalBlockEntity pedal)
                        pedals.add(pedal);
            }
        }
        return pedals;
    }

    @Nullable
    private static PedalBlockEntity pedalAt(final ClientLevel level, final BlockPos pos) {
        return (level.isLoaded(pos) && (level.getBlockEntity(pos) instanceof PedalBlockEntity pedal)) ? pedal : null;
    }

    /**
     * @return The pedal the given one is plugged into, with both agreeing (as {@code PedalLinks} counts a link), or null
     */
    @Nullable
    private static PedalBlockEntity downstreamOf(final ClientLevel level, final PedalBlockEntity pedal) {
        final BlockPos targetPos = pedal.getOutput();
        final PedalBlockEntity target = (targetPos == null) ? null : pedalAt(level, targetPos);
        return ((target != null) && pedal.getBlockPos().equals(target.getInput())) ? target : null;
    }

    /**
     * @return The pedal plugged into the given one, with both agreeing, or null
     */
    @Nullable
    private static PedalBlockEntity upstreamOf(final ClientLevel level, final PedalBlockEntity pedal) {
        final BlockPos sourcePos = pedal.getInput();
        final PedalBlockEntity source = (sourcePos == null) ? null : pedalAt(level, sourcePos);
        return ((source != null) && pedal.getBlockPos().equals(source.getOutput())) ? source : null;
    }

    /**
     * @return The positions of the pedals the held instrument (main hand first) plays through: the one it's plugged
     * into and every one after it. Empty if it isn't plugged in, or its pedal isn't loaded.
     */
    private static Set<BlockPos> heldInstrumentPath(final LocalPlayer player, final ClientLevel level) {
        for (final ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (!(stack.getItem() instanceof InstrumentItem))
                continue;
            final CompoundTag modTag = stack.getTagElement(SCInstrumentMod.MODID);
            final BlockPos pedalPos = (modTag == null) ? null : InstrumentPedals.pluggedPedalPos(modTag, level);
            final PedalBlockEntity first = (pedalPos == null) ? null : pedalAt(level, pedalPos);
            if ((first == null) || !InstrumentPedals.isPluggedInto(modTag, first))
                continue;

            final Set<BlockPos> path = new HashSet<>();
            for (PedalBlockEntity pedal = first; (pedal != null) && (path.size() < PedalChain.MAX_LENGTH)
                    && path.add(pedal.getBlockPos()); )
                pedal = downstreamOf(level, pedal);
            return path;
        }
        return Set.of();
    }

    /**
     * @return The pedal's place in its chain, counting from the first pedal as 1
     */
    private static int placeInChain(final ClientLevel level, final PedalBlockEntity pedal) {
        int place = 1;
        // Bounded, in case the client briefly sees a loop while links are being updated
        for (PedalBlockEntity source = upstreamOf(level, pedal);
             (source != null) && (place <= PedalChain.MAX_LENGTH); source = upstreamOf(level, source))
            place++;
        return place;
    }

    //#endregion


    //#region Drawing

    /**
     * Draws a dashed line from {@code from} to {@code to}, its dashes shifted along by {@code offset}
     * so they march towards {@code to}
     */
    private static void drawDashes(final PoseStack pose, final VertexConsumer lines, final Vec3 from, final Vec3 to,
                                   final double offset, final int rgb, final int alpha) {
        final Vec3 span = to.subtract(from);
        final double length = span.length();
        if (length < 1e-3)
            return;
        final Vec3 direction = span.scale(1 / length);

        final Matrix4f matrix = pose.last().pose();
        final Matrix3f normal = pose.last().normal();
        final int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;

        // Starts a dash early, so one is always partly in view at the start
        for (double start = offset - (DASH_LENGTH + GAP_LENGTH); start < length; start += DASH_LENGTH + GAP_LENGTH) {
            final double dashStart = Math.max(start, 0), dashEnd = Math.min(start + DASH_LENGTH, length);
            if (dashEnd <= dashStart)
                continue;

            final Vec3 a = from.add(direction.scale(dashStart)), b2 = from.add(direction.scale(dashEnd));
            lines.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(r, g, b, alpha)
                .normal(normal, (float) direction.x, (float) direction.y, (float) direction.z).endVertex();
            lines.vertex(matrix, (float) b2.x, (float) b2.y, (float) b2.z).color(r, g, b, alpha)
                .normal(normal, (float) direction.x, (float) direction.y, (float) direction.z).endVertex();
        }
    }

    /**
     * Draws the pedal's place in its chain above it, facing the camera and seen through blocks,
     * as name tags are. Its background is drawn separately, by {@link #drawBadgeBackground}.
     */
    private static void drawBadge(final Minecraft minecraft, final PoseStack pose, final MultiBufferSource buffers,
                                  final PedalBlockEntity pedal, final int number) {
        final Font font = minecraft.font;
        final Component text = Component.literal(Integer.toString(number));

        pushBadgePose(minecraft, pose, pedal.getBlockPos());
        font.drawInBatch(text, -font.width(text) / 2f, 0, pedal.isBypassed() ? BADGE_BYPASSED : BADGE_ON, false,
            pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    /**
     * Draws the dark box behind a badge's number, the size {@link Font} gives a name tag's
     */
    private static void drawBadgeBackground(final Minecraft minecraft, final PoseStack pose,
                                            final VertexConsumer backgrounds, final PedalBlockEntity pedal,
                                            final int number) {
        final Font font = minecraft.font;
        final float halfWidth = font.width(Integer.toString(number)) / 2f;
        final float left = -halfWidth - 1, right = halfWidth + 1, top = -1, bottom = font.lineHeight;

        pushBadgePose(minecraft, pose, pedal.getBlockPos());
        final Matrix4f matrix = pose.last().pose();
        final int a = (BADGE_BACKGROUND >>> 24) & 0xFF, r = (BADGE_BACKGROUND >> 16) & 0xFF,
            g = (BADGE_BACKGROUND >> 8) & 0xFF, b = BADGE_BACKGROUND & 0xFF;
        backgrounds.vertex(matrix, left, top, 0).color(r, g, b, a).uv2(LightTexture.FULL_BRIGHT).endVertex();
        backgrounds.vertex(matrix, left, bottom, 0).color(r, g, b, a).uv2(LightTexture.FULL_BRIGHT).endVertex();
        backgrounds.vertex(matrix, right, bottom, 0).color(r, g, b, a).uv2(LightTexture.FULL_BRIGHT).endVertex();
        backgrounds.vertex(matrix, right, top, 0).color(r, g, b, a).uv2(LightTexture.FULL_BRIGHT).endVertex();
        pose.popPose();
    }

    /**
     * Pushes the pose a badge is drawn in: above the pedal, facing the camera, in font pixels
     */
    private static void pushBadgePose(final Minecraft minecraft, final PoseStack pose, final BlockPos pos) {
        pose.pushPose();
        pose.translate(pos.getX() + 0.5, pos.getY() + CABLE_HEIGHT + 0.35, pos.getZ() + 0.5);
        pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025f, -0.025f, 0.025f);
    }

    //#endregion
}
