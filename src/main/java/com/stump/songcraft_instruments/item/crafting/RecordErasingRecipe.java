package com.stump.songcraft_instruments.item.crafting;

import com.stump.songcraft_instruments.item.ModItems;
import com.stump.songcraft_instruments.item.record.WritableRecordItem;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class RecordErasingRecipe extends CustomRecipe {

    public RecordErasingRecipe(ResourceLocation pId, CraftingBookCategory pCategory) {
        super(pId, pCategory);
    }

    @Override
    public boolean matches(CraftingContainer pInv, Level pLevel) {
        return getIngredientsFromContainer(pInv).isPresent();
    }

    private static boolean isBurnedRecord(final ItemStack stack) {
        return stack.is(ModItems.RECORD_WRITABLE.get()) && ((WritableRecordItem)stack.getItem()).isBurned(stack);
    }

    @Override
    public ItemStack assemble(CraftingContainer container, net.minecraft.core.RegistryAccess registryAccess) {
        return new ItemStack(ModItems.RECORD_WRITABLE.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(
                container.getContainerSize(),
                ItemStack.EMPTY
        );

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);

            if (stack.is(Items.SPONGE)) {
                remainingItems.set(i, stack.copy());
            }
        }

        return remainingItems;
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.RECORD_ERASING.get();
    }

    private Optional<List<ItemStack>> getIngredientsFromContainer(CraftingContainer pInv) {
        ItemStack recordedRecord = ItemStack.EMPTY;
        ItemStack sponge = ItemStack.EMPTY;

        for (int i = 0; i < pInv.getContainerSize(); i++) {
            ItemStack stack = pInv.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (isBurnedRecord(stack)) {
                if (!recordedRecord.isEmpty()) {
                    return Optional.empty();
                }

                recordedRecord = stack;
            } else if (stack.is(Items.SPONGE)) {
                if (!sponge.isEmpty()) {
                    return Optional.empty();
                }

                sponge = stack;
            } else {
                return Optional.empty();
            }
        }

        if (recordedRecord.isEmpty() || sponge.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(List.of(recordedRecord, sponge));
    }

    /**
     * Used to determine if this recipe can fit in a grid of the given width/height
     */
    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return pWidth >= 3 && pHeight >= 3;
    }
}