package com.stump.songcraft_instruments.pedal;

import com.stump.songcraft_instruments.SCInstrumentMod;
import com.stump.songcraft_instruments.block.blockentity.PedalBlockEntity;
import com.stump.songcraft_instruments.capability.instrumentOpen.InstrumentOpenProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Plugs instruments into effects pedals. An instrument plays through the pedal it's plugged into and every pedal after
 * it (see {@link PedalLinks}), wherever its notes are heard: where it's played, through each of its speakers, and by its
 * own player. A held instrument plugs in by right-clicking a pedal with it, a block instrument with the speaker cable.
 * <p>Only the instrument stores the link: the pedal's position, ID and dimension. Pedals only apply while they are
 * loaded and in the instrument's dimension, and a link to a pedal that's gone is dropped once its spot is loaded.</p>
 * <p>instrument tag layout: pedal: { pos: {X,Y,Z}, id: UUID, dimension: "namespace:path" }. Server only.</p>
 */
public final class InstrumentPedals {
    private static final String PEDAL_TAG = "pedal", POS_TAG = "pos", ID_TAG = "id", DIMENSION_TAG = "dimension";

    public enum PlugResult { PLUGGED, ALREADY_PLUGGED }

    private record Link(BlockPos pos, UUID pedalId, String dimension) {}

    private InstrumentPedals() {}


    //#region Plugging

    public static PlugResult plug(final CompoundTag modTag, final PedalBlockEntity pedal) {
        if (isPluggedInto(modTag, pedal))
            return PlugResult.ALREADY_PLUGGED;

        final CompoundTag linkTag = new CompoundTag();
        linkTag.put(POS_TAG, NbtUtils.writeBlockPos(pedal.getBlockPos()));
        linkTag.putUUID(ID_TAG, pedal.getPedalId());
        linkTag.putString(DIMENSION_TAG, dimensionOf(pedal.getLevel()));
        modTag.put(PEDAL_TAG, linkTag);
        return PlugResult.PLUGGED;
    }
    /**
     * @return Whether the instrument was plugged into anything
     */
    public static boolean unplug(final CompoundTag modTag) {
        if (!modTag.contains(PEDAL_TAG))
            return false;
        modTag.remove(PEDAL_TAG);
        return true;
    }

    /**
     * Only reads the tag, so it's safe on the client too, which shows a held instrument's chain
     * @return The position of the pedal the instrument is plugged into, or null if it isn't plugged into one
     * in the given level's dimension. Whether a pedal there is still the same one is up to {@link #isPluggedInto}.
     */
    @Nullable
    public static BlockPos pluggedPedalPos(final CompoundTag modTag, final Level level) {
        final Link link = read(modTag);
        return ((link != null) && link.dimension().equals(dimensionOf(level))) ? link.pos() : null;
    }

    public static boolean isPluggedInto(final CompoundTag modTag, final PedalBlockEntity pedal) {
        final Link link = read(modTag);
        return (link != null) && link.pos().equals(pedal.getBlockPos()) && link.pedalId().equals(pedal.getPedalId());
    }

    /**
     * Right-clicking a pedal with a held instrument: right-click plugs the instrument into the pedal,
     * shift + right-click unplugs it.
     */
    public static void useHeldInstrument(final Player player, final ItemStack instrument, final PedalBlockEntity pedal) {
        final CompoundTag modTag = SCInstrumentMod.modTag(instrument);

        if (player.isShiftKeyDown()) {
            final boolean wasPlugged = isPluggedInto(modTag, pedal);
            if (wasPlugged)
                unplug(modTag);
            sendUnplugMessage(player, wasPlugged);
        } else {
            sendPlugMessage(player, plug(modTag, pedal));
        }
        PedalMonitorSync.onPedalsChanged(player.level());
    }

    public static void sendPlugMessage(final Player player, final PlugResult result) {
        player.displayClientMessage((result == PlugResult.PLUGGED)
            ? Component.translatable("songcraft_instruments.pedal.instrument_plugged").withStyle(ChatFormatting.GREEN)
            : Component.translatable("songcraft_instruments.pedal.instrument_already_plugged").withStyle(ChatFormatting.YELLOW)
        , true);
    }
    public static void sendUnplugMessage(final Player player, final boolean wasPlugged) {
        player.displayClientMessage(wasPlugged
            ? Component.translatable("songcraft_instruments.pedal.instrument_unplugged").withStyle(ChatFormatting.GREEN)
            : Component.translatable("songcraft_instruments.pedal.instrument_not_plugged").withStyle(ChatFormatting.YELLOW)
        , true);
    }

    //#endregion


    //#region Resolving

    /**
     * @return The pedals the instrument the player has open (held or block) plays through right now
     */
    public static PedalChain chainOfOpenInstrument(final Player player) {
        if (!InstrumentOpenProvider.isOpen(player))
            return PedalChain.NONE;
        final Level level = player.level();

        if (InstrumentOpenProvider.isItem(player)) {
            final InteractionHand hand = InstrumentOpenProvider.getHand(player);
            return (hand == null) ? PedalChain.NONE : chainOf(level, SCInstrumentMod.modTag(player.getItemInHand(hand)));
        }

        final BlockPos instrumentPos = InstrumentOpenProvider.getBlockPos(player);
        final BlockEntity instrument = (instrumentPos == null) ? null : level.getBlockEntity(instrumentPos);
        return (instrument == null) ? PedalChain.NONE : chainOf(level, instrument);
    }

    /**
     * @return The ID of the Lo-fi pedal (the tape) the instrument the player has open plays through,
     * or null if there's none (see {@link PedalLinks#findTape})
     */
    @Nullable
    public static UUID tapeOfOpenInstrument(final Player player) {
        final CompoundTag modTag = openInstrumentTag(player);
        final PedalBlockEntity pedal = (modTag == null) ? null : pluggedPedal(player.level(), modTag);
        return (pedal == null) ? null : PedalLinks.findTape(player.level(), pedal);
    }

    /**
     * @return The positions of the pedals the instrument the player has open plays through, in signal order,
     * bypassed ones included (see {@link PedalLinks#resolvePath}). Empty if it isn't plugged in.
     */
    public static List<BlockPos> pedalPathOfOpenInstrument(final Player player) {
        final CompoundTag modTag = openInstrumentTag(player);
        final PedalBlockEntity pedal = (modTag == null) ? null : pluggedPedal(player.level(), modTag);
        return (pedal == null) ? List.of() : PedalLinks.resolvePath(player.level(), pedal);
    }

    /**
     * @return The mod tag of the instrument the player has open (held or block), or null if there's none
     */
    @Nullable
    private static CompoundTag openInstrumentTag(final Player player) {
        if (!InstrumentOpenProvider.isOpen(player))
            return null;

        if (InstrumentOpenProvider.isItem(player)) {
            final InteractionHand hand = InstrumentOpenProvider.getHand(player);
            return (hand == null) ? null : SCInstrumentMod.modTag(player.getItemInHand(hand));
        }

        final BlockPos instrumentPos = InstrumentOpenProvider.getBlockPos(player);
        final BlockEntity instrument = (instrumentPos == null) ? null : player.level().getBlockEntity(instrumentPos);
        return (instrument == null) ? null : SCInstrumentMod.modTag(instrument);
    }

    public static PedalChain chainOf(final Level level, final BlockEntity instrument) {
        final CompoundTag modTag = SCInstrumentMod.modTag(instrument);
        final boolean wasPlugged = modTag.contains(PEDAL_TAG);

        final PedalChain chain = chainOf(level, modTag);
        if (wasPlugged && !modTag.contains(PEDAL_TAG))
            instrument.setChanged();
        return chain;
    }

    /**
     * Drops the link if the pedal is known to be gone
     * @return The pedals the instrument plays through right now: none if it isn't plugged in, or its pedal isn't loaded
     */
    private static PedalChain chainOf(final Level level, final CompoundTag modTag) {
        final PedalBlockEntity pedal = pluggedPedal(level, modTag);
        return (pedal == null) ? PedalChain.NONE : PedalLinks.resolveChain(level, pedal);
    }

    /**
     * Drops the link if the pedal is known to be gone
     * @return The pedal the instrument is plugged into, or null if it isn't, or its pedal isn't loaded
     */
    @Nullable
    private static PedalBlockEntity pluggedPedal(final Level level, final CompoundTag modTag) {
        final Link link = read(modTag);
        if ((link == null) || !link.dimension().equals(dimensionOf(level)) || !level.isLoaded(link.pos()))
            return null;

        final PedalBlockEntity pedal = PedalLinks.pedalAt(level, link.pos());
        if ((pedal == null) || !pedal.getPedalId().equals(link.pedalId())) {
            modTag.remove(PEDAL_TAG);
            return null;
        }
        return pedal;
    }

    //#endregion


    @Nullable
    private static Link read(final CompoundTag modTag) {
        if (!modTag.contains(PEDAL_TAG, Tag.TAG_COMPOUND))
            return null;

        final CompoundTag linkTag = modTag.getCompound(PEDAL_TAG);
        if (!linkTag.contains(POS_TAG, Tag.TAG_COMPOUND) || !linkTag.hasUUID(ID_TAG))
            return null;
        return new Link(NbtUtils.readBlockPos(linkTag.getCompound(POS_TAG)), linkTag.getUUID(ID_TAG),
            linkTag.getString(DIMENSION_TAG));
    }

    private static String dimensionOf(@Nullable final Level level) {
        return (level == null) ? "" : level.dimension().location().toString();
    }
}
