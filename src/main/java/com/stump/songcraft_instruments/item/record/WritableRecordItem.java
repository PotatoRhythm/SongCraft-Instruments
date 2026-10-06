package com.stump.songcraft_instruments.item.record;

import com.stump.songcraft_instruments.recording.Recording;
import com.stump.songcraft_instruments.recording.Recording.Performer;
import com.stump.songcraft_instruments.recording.RecordingCodec;
import com.stump.songcraft_instruments.util.ParticleColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A record, empty or burned. A burned record only holds the ID of its recording
 * (kept in the world save by {@link com.stump.songcraft_instruments.recording.RecordingStore RecordingStore})
 * and what its tooltip shows, so it stays small however long the recording is.
 */
public class WritableRecordItem extends Item {
    public static final String
        RECORDING_ID_TAG = "RecordingId",
        LENGTH_TAG = "Length",
        NOTE_COUNT_TAG = "NoteCount",
        PERFORMER_NAMES_TAG = "PerformerNames",
        // The particle colors of the performers, in the same order as PERFORMER_NAMES_TAG
        PERFORMER_COLORS_TAG = "PerformerColors",
        // Set when loopers are synced with the looper adapter, overriding when this copy loops back
        REPEAT_TICK_TAG = "RepeatTick"
    ;

    public WritableRecordItem(Properties properties) {
        super(properties.stacksTo(1));
    }


    public boolean isBurned(final ItemStack stack) {
        return getRecordingId(stack) != null;
    }

    /**
     * @return The ID of the record's recording, or null if the record is empty
     */
    public static @Nullable String getRecordingId(final ItemStack stack) {
        final CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(RECORDING_ID_TAG, Tag.TAG_STRING))
            return null;

        final String id = tag.getString(RECORDING_ID_TAG);
        return RecordingCodec.isValidId(id) ? id : null;
    }

    /**
     * Burns the recording into the record
     */
    public static void burn(final ItemStack stack, final String recordingId, final Recording recording) {
        final CompoundTag tag = stack.getOrCreateTag();
        tag.putString(RECORDING_ID_TAG, recordingId);
        tag.putInt(LENGTH_TAG, recording.length());
        tag.putInt(NOTE_COUNT_TAG, recording.noteCount());
        tag.remove(REPEAT_TICK_TAG);

        final ListTag names = new ListTag(), colors = new ListTag();
        for (final Performer performer : recording.performers()) {
            names.add(StringTag.valueOf(performer.name()));
            colors.add(new IntArrayTag(performer.colors()));
        }
        tag.put(PERFORMER_NAMES_TAG, names);
        tag.put(PERFORMER_COLORS_TAG, colors);
    }

    /**
     * @return The tick this copy loops back at if it was overridden, otherwise -1
     */
    public static int getRepeatTickOverride(final ItemStack stack) {
        final CompoundTag tag = stack.getTag();
        return (tag != null && tag.contains(REPEAT_TICK_TAG, Tag.TAG_INT)) ? tag.getInt(REPEAT_TICK_TAG) : -1;
    }
    public static void setRepeatTickOverride(final ItemStack stack, final int tick) {
        stack.getOrCreateTag().putInt(REPEAT_TICK_TAG, tick);
    }


    @Override
    public boolean isFoil(ItemStack pStack) {
        return isBurned(pStack);
    }

    @Override
    public Component getName(ItemStack pStack) {
        return Component.translatable(String.format(
            "item.songcraft_instruments.%s_record",
            isBurned(pStack) ? "burned" : "writable"
        ));
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        final CompoundTag tag = pStack.getTag();
        if (tag != null && isBurned(pStack)) {
            final ListTag names = tag.getList(PERFORMER_NAMES_TAG, Tag.TAG_STRING);
            final ListTag colors = tag.getList(PERFORMER_COLORS_TAG, Tag.TAG_INT_ARRAY);

            if (!names.isEmpty()) {
                final MutableComponent performers = Component.empty();
                for (int i = 0; i < names.size(); i++) {
                    if (i > 0)
                        performers.append(", ");

                    final int[] performerColors = (i < colors.size()) ? colors.getIntArray(i) : new int[0];
                    performers.append(gradientName(names.getString(i), performerColors));
                }

                pTooltipComponents.add(
                    Component.translatable("item.songcraft_instruments.record.performers", performers)
                        .withStyle(ChatFormatting.GRAY)
                );
            }

            if (tag.getInt(LENGTH_TAG) > 0) {
                final int seconds = tag.getInt(LENGTH_TAG) / 20;
                pTooltipComponents.add(
                    Component.translatable("item.songcraft_instruments.record.length",
                        String.format("%d:%02d", seconds / 60, seconds % 60), tag.getInt(NOTE_COUNT_TAG)
                    ).withStyle(ChatFormatting.GRAY)
                );
            }
        }

        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    /**
     * @return The name, with its letters colored across the performer's particle color gradient.
     * Uncolored if their colors are unknown.
     */
    private static Component gradientName(final String name, final int[] colors) {
        if ((colors.length == 0) || name.isEmpty())
            return Component.literal(name);

        final MutableComponent result = Component.empty();
        for (int i = 0; i < name.length(); i++) {
            final float t = (name.length() == 1) ? 0 : (float) i / (name.length() - 1);
            final int rgb = ParticleColorUtil.getGradientColor(t, colors);

            result.append(Component.literal(String.valueOf(name.charAt(i)))
                .withStyle((style) -> style.withColor(TextColor.fromRgb(rgb))));
        }
        return result;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return isBurned(stack) ? 1 : 16;
    }
}
