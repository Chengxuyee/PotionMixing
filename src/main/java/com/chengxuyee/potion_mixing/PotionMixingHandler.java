package com.chengxuyee.potion_mixing;

import com.chengxuyee.potion_mixing.config.ConfigLoader;
import com.chengxuyee.potion_mixing.config.PotionMixingConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PotionMixingHandler {

    public static final String MIXED_POTION_MARKER_KEY = "PotionMixingMixed";

    public static ItemStack mixPotions(CraftingRecipeInput input) {
        if (!isPotionMixRecipe(input)) {
            return ItemStack.EMPTY;
        }

        PotionMixingConfig config = ConfigLoader.getConfig();

        Map<Item, Integer> typeCounts = new HashMap<>();

        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getStackInSlot(i);

            if (stack.isOf(Items.POTION)) {
                typeCounts.put(Items.POTION, typeCounts.getOrDefault(Items.POTION, 0) + 1);
            } else if (stack.isOf(Items.SPLASH_POTION)) {
                typeCounts.put(Items.SPLASH_POTION, typeCounts.getOrDefault(Items.SPLASH_POTION, 0) + 1);
            } else if (stack.isOf(Items.LINGERING_POTION)) {
                typeCounts.put(Items.LINGERING_POTION, typeCounts.getOrDefault(Items.LINGERING_POTION, 0) + 1);
            }
        }

        Item outputType = determineOutputType(typeCounts);

        Map<StatusEffect, StatusEffectInstance> mergedEffects = new LinkedHashMap<>();
        Map<StatusEffect, List<StatusEffectInstance>> instantEffectGroups = new LinkedHashMap<>();

        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getStackInSlot(i);

            PotionContentsComponent component = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (component == null) {
                continue;
            }

            for (StatusEffectInstance effect : component.getEffects()) {
                StatusEffect effectType = effect.getEffectType().value();

                if (effectType.isInstant()) {
                    instantEffectGroups
                            .computeIfAbsent(effectType, key -> new ArrayList<>())
                            .add(effect);
                } else {
                    mergedEffects.merge(
                            effectType,
                            effect,
                            PotionMixingHandler::selectBetterInstance
                    );
                }
            }
        }

        List<StatusEffectInstance> allEffects = new ArrayList<>(mergedEffects.values());

        for (Map.Entry<StatusEffect, List<StatusEffectInstance>> entry : instantEffectGroups.entrySet()) {
            StatusEffect effectType = entry.getKey();
            List<StatusEffectInstance> effects = entry.getValue();

            int maxLevel = config.getEffectLimit(effectType);

            effects.sort((first, second) -> Integer.compare(second.getAmplifier(), first.getAmplifier()));

            int totalLevel = 0;
            List<StatusEffectInstance> validEffects = new ArrayList<>();

            for (StatusEffectInstance effect : effects) {
                int effectLevel = effect.getAmplifier() + 1;
                int newTotal = totalLevel + effectLevel;

                if (newTotal <= maxLevel) {
                    validEffects.add(effect);
                    totalLevel = newTotal;
                } else {
                    int remaining = maxLevel - totalLevel;

                    if (remaining > 0) {
                        StatusEffectInstance partialEffect = new StatusEffectInstance(
                                effect.getEffectType(),
                                effect.getDuration(),
                                remaining - 1,
                                effect.isAmbient(),
                                effect.shouldShowParticles(),
                                effect.shouldShowIcon()
                        );

                        validEffects.add(partialEffect);
                    }

                    break;
                }
            }

            allEffects.addAll(validEffects);
        }

        if (allEffects.size() > config.maxEffectEntries) {
            allEffects = new ArrayList<>(allEffects.subList(0, config.maxEffectEntries));
        }

        PotionContentsComponent newComponent = new PotionContentsComponent(
                Optional.of(Potions.WATER),
                Optional.empty(),
                allEffects
        );

        ItemStack result = new ItemStack(outputType);
        result.set(DataComponentTypes.POTION_CONTENTS, newComponent);
        result.set(DataComponentTypes.CUSTOM_NAME, getPotionName(outputType));

        NbtCompound customData = new NbtCompound();
        customData.putBoolean(MIXED_POTION_MARKER_KEY, true);
        result.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));

        return result;

    }

    private static Text getPotionName(Item outputType) {
        if (outputType == Items.POTION) {
            return Text.literal("Mixed Potion").formatted(Formatting.WHITE);
        } else if (outputType == Items.SPLASH_POTION) {
            return Text.literal("Mixed Splash Potion").formatted(Formatting.WHITE);
        } else {
            return Text.literal("Mixed Lingering Potion").formatted(Formatting.WHITE);
        }
    }

    private static Item determineOutputType(Map<Item, Integer> typeCounts) {
        int potionCount = typeCounts.getOrDefault(Items.POTION, 0);
        int splashCount = typeCounts.getOrDefault(Items.SPLASH_POTION, 0);
        int lingeringCount = typeCounts.getOrDefault(Items.LINGERING_POTION, 0);

        if (potionCount >= splashCount && potionCount >= lingeringCount) {
            return Items.POTION;
        } else if (splashCount >= lingeringCount) {
            return Items.SPLASH_POTION;
        } else {
            return Items.LINGERING_POTION;
        }
    }

    public static StatusEffectInstance selectBetterInstance(
            StatusEffectInstance existing,
            StatusEffectInstance newEffect
    ) {
        if (newEffect.getAmplifier() > existing.getAmplifier()) {
            return newEffect;
        }

        if (newEffect.getAmplifier() == existing.getAmplifier() && newEffect.getDuration() > existing.getDuration()) {
            return newEffect;
        }

        return existing;
    }

    public static boolean isPotionMixRecipe(CraftingRecipeInput input) {
        int potionCount = 0;

        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getStackInSlot(i);

            if (stack.isEmpty()) {
                continue;
            }

            Item item = stack.getItem();

            if (item != Items.POTION && item != Items.SPLASH_POTION && item != Items.LINGERING_POTION) {
                return false;
            }

            potionCount++;
        }

        return potionCount >= 2;
    }
}
