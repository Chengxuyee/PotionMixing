package com.chengxuyee.potion_mixing.mixin;

import com.chengxuyee.potion_mixing.PotionMixingHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingRecipeRegistry.class)
public abstract class BrewingRecipeRegistryMixin {
    @Inject(method = "craft", at = @At("RETURN"), cancellable = true)
    private void potion_mixing$preserveMixedPotionContents(
            ItemStack ingredient,
            ItemStack input,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (input.isEmpty()) {
            return;
        }

        Item outputItem = getPotionTypeConversionOutput(ingredient, input);

        if (outputItem == null) {
            return;
        }

        PotionContentsComponent contents = input.get(DataComponentTypes.POTION_CONTENTS);

        if (contents == null) {
            return;
        }

        if (!isPotionMixingPotion(input)) {
            return;
        }

        ItemStack originalResult = cir.getReturnValue();

        if (!originalResult.isOf(outputItem) && !originalResult.isOf(input.getItem())) {
            return;
        }

        ItemStack fixedResult = input.copyComponentsToNewStack(outputItem, 1);
        updateMixedPotionName(input, fixedResult, outputItem);

        cir.setReturnValue(fixedResult);
    }

    private static Item getPotionTypeConversionOutput(ItemStack ingredient, ItemStack input) {
        if (input.isOf(Items.POTION) && ingredient.isOf(Items.GUNPOWDER)) {
            return Items.SPLASH_POTION;
        }

        if (input.isOf(Items.SPLASH_POTION) && ingredient.isOf(Items.DRAGON_BREATH)) {
            return Items.LINGERING_POTION;
        }

        return null;
    }

    private static boolean isPotionMixingPotion(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);

        if (customData != null
                && customData.copyNbt().getBoolean(PotionMixingHandler.MIXED_POTION_MARKER_KEY)) {
            return true;
        }

        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);

        if (customName == null) {
            return false;
        }

        String name = customName.getString();

        return "Mixed Potion".equals(name) || "Mixed Splash Potion".equals(name) || "Mixed Lingering Potion".equals(name);
    }

    private static void updateMixedPotionName(ItemStack input, ItemStack result, Item outputItem) {
        Text customName = input.get(DataComponentTypes.CUSTOM_NAME);

        if (customName == null) {
            return;
        }

        String name = customName.getString();

        if (outputItem == Items.SPLASH_POTION && "Mixed Potion".equals(name)) {
            result.set(
                    DataComponentTypes.CUSTOM_NAME,
                    Text.literal("Mixed Splash Potion").formatted(Formatting.WHITE)
            );
            return;
        }

        if (outputItem == Items.LINGERING_POTION && ("Mixed Splash Potion".equals(name) || "Mixed Potion".equals(name))) {
            result.set(
                    DataComponentTypes.CUSTOM_NAME,
                    Text.literal("Mixed Lingering Potion").formatted(Formatting.WHITE)
            );
        }
    }
}
