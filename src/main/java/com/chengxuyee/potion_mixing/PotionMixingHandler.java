package com.chengxuyee.potion_mixing;

import com.chengxuyee.potion_mixing.config.ConfigLoader;
import com.chengxuyee.potion_mixing.config.PotionMixingConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class PotionMixingHandler {
    public static ItemStack mixPotions(RecipeInputInventory inventory) {
        PotionMixingConfig config = ConfigLoader.getConfig();
        Map<Item, Integer> typeCounts = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(Items.POTION)) {
                typeCounts.put(Items.POTION, typeCounts.getOrDefault(Items.POTION, 0) + 1);
            } else if (stack.isOf(Items.SPLASH_POTION)) {
                typeCounts.put(Items.SPLASH_POTION, typeCounts.getOrDefault(Items.SPLASH_POTION, 0) + 1);
            } else if (stack.isOf(Items.LINGERING_POTION)) {
                typeCounts.put(Items.LINGERING_POTION, typeCounts.getOrDefault(Items.LINGERING_POTION, 0) + 1);
            }
        }
        Item outputType = determineOutputType(typeCounts);
        Map<StatusEffect, StatusEffectInstance> mergedEffects = new HashMap<>();
        Map<StatusEffect, List<StatusEffectInstance>> instantEffectGroups = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            PotionContentsComponent comp = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (comp == null) continue;

            for (StatusEffectInstance effect : comp.getEffects()) {
                if (effect.getEffectType().value().isInstant()) {
                    instantEffectGroups
                            .computeIfAbsent(effect.getEffectType().value(), k -> new ArrayList<>())
                            .add(effect);
                } else {
                    mergedEffects.merge(
                            effect.getEffectType().value(),
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
            effects.sort((e1, e2) -> Integer.compare(e2.getAmplifier(), e1.getAmplifier()));

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
            allEffects = allEffects.subList(0, config.maxEffectEntries);
        }
        PotionContentsComponent newComponent = new PotionContentsComponent(
                Optional.empty(),
                Optional.empty(),
                allEffects,
                Optional.empty()
        );
        ItemStack result = new ItemStack(outputType);
        result.set(DataComponentTypes.POTION_CONTENTS, newComponent);
        result.set(DataComponentTypes.CUSTOM_NAME, getPotionName(outputType));
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
        }
        else if (splashCount >= lingeringCount) {
            return Items.SPLASH_POTION;
        }
        else {
            return Items.LINGERING_POTION;
        }
    }

    public static StatusEffectInstance selectBetterInstance(
            StatusEffectInstance existing, StatusEffectInstance newEffect
    ) {
        if (newEffect.getAmplifier() > existing.getAmplifier()) {
            return newEffect;
        }
        if (newEffect.getAmplifier() == existing.getAmplifier() &&
                newEffect.getDuration() > existing.getDuration()) {
            return newEffect;
        }
        return existing;
    }
}
