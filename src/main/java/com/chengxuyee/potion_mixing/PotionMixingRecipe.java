package com.chengxuyee.potion_mixing;
import com.chengxuyee.potion_mixing.config.ConfigLoader;
import com.chengxuyee.potion_mixing.config.PotionMixingConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class PotionMixingRecipe extends SpecialCraftingRecipe {
    public PotionMixingRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        return PotionMixingHandler.isPotionMixRecipe(input);
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        if (!PotionMixingHandler.isPotionMixRecipe(input)) {
            return ItemStack.EMPTY;
        }

        return PotionMixingHandler.mixPotions(input);
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(CraftingRecipeInput input) {
        DefaultedList<ItemStack> remainder = DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY);

        PotionMixingConfig config = ConfigLoader.getConfig();

        if (!config.returnEmptyBottles) {
            return remainder;
        }

        int potionCount = 0;

        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getStackInSlot(i);

            if (isPotionItem(stack)) {
                potionCount++;
            }
        }

        int bottlesToReturn = potionCount - 1;

        if (bottlesToReturn <= 0) {
            return remainder;
        }

        for (int i = 0; i < input.getSize() && bottlesToReturn > 0; i++) {
            ItemStack stack = input.getStackInSlot(i);

            if (isPotionItem(stack)) {
                remainder.set(i, new ItemStack(Items.GLASS_BOTTLE));
                bottlesToReturn--;
            }
        }

        return remainder;
    }

    private static boolean isPotionItem(ItemStack stack) {
        return stack.isOf(Items.POTION)
                || stack.isOf(Items.SPLASH_POTION)
                || stack.isOf(Items.LINGERING_POTION);
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return PotionMixing.POTION_MIXING_RECIPE_SERIALIZER;
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(Items.POTION);
    }
}
