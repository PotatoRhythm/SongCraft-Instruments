package com.stump.songcraft_instruments.item.emirecord;

import com.stump.songcraft_instruments.block.blockentity.LooperBlockEntity;
import com.stump.songcraft_instruments.block.blockentity.looper.LooperRecordWriter;
import com.stump.songcraft_instruments.item.ModItems;
import com.stump.songcraft_instruments.util.CommonUtil;
import com.stump.songcraft_instruments.util.LooperUtil;
import com.stump.songcraft_instruments.util.ParticleColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WritableRecordItem extends EMIRecordItem {
    public WritableRecordItem(Properties properties) {
        super(properties);
    }

    public boolean isBurned(final ItemStack stack) {
        return (stack.getOrCreateTag().contains(CHANNEL_TAG, Tag.TAG_COMPOUND) &&
               !stack.getTagElement(CHANNEL_TAG).getBoolean(WRITABLE_TAG))
            // May also be media burned
            || stack.getTag().contains(BurnedRecordItem.BURNED_MEDIA_TAG);
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return isBurned(pStack);
    }

    @Override
    public void onInsert(final ItemStack stack, final LooperBlockEntity lbe) {
        if (stack.getOrCreateTag().contains(BurnedRecordItem.BURNED_MEDIA_TAG))
            return;

        final CompoundTag channel = CommonUtil.getOrCreateElementTag(stack.getOrCreateTag(), CHANNEL_TAG);

        if (!channel.getBoolean(WRITABLE_TAG) && !RecordNotes.hasNotes(channel)) {
            // Record is empty; check if is legacy looper
            LooperUtil.migrateLegacyLooper(lbe).ifPresentOrElse(
                (recordData) -> stack.getTag().put(CHANNEL_TAG, recordData),
                // 100% empty
                () -> channel.putBoolean(WRITABLE_TAG, true)
            );
        }
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
        final CompoundTag channel = pStack.getTagElement(CHANNEL_TAG);
        if (channel != null && channel.contains(LooperRecordWriter.PERFORMER_NAMES_TAG, Tag.TAG_LIST)) {
            final ListTag names = channel.getList(LooperRecordWriter.PERFORMER_NAMES_TAG, Tag.TAG_STRING);
            final ListTag colors = channel.getList(LooperRecordWriter.PERFORMER_COLORS_TAG, Tag.TAG_INT_ARRAY);

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
        return isBurned(stack) ? super.getMaxStackSize(stack) : 16;
    }
}
