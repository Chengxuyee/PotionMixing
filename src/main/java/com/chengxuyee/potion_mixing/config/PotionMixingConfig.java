package com.chengxuyee.potion_mixing.config;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;

import java.util.HashMap;
import java.util.Map;
public class PotionMixingConfig {
    public int maxInstantEffectLevel = 5;
    public int maxEffectEntries = 9;
    public Map<String, Integer> effectLimits = new HashMap<>();

    public PotionMixingConfig() {
        effectLimits.put(StatusEffects.INSTANT_HEALTH.value().getTranslationKey(), maxInstantEffectLevel);
        effectLimits.put(StatusEffects.INSTANT_DAMAGE.value().getTranslationKey(), maxInstantEffectLevel);
    }

    public int getEffectLimit(StatusEffect effect) {
        if (effectLimits == null) {
            return maxInstantEffectLevel;
        }

        String key = effect.getTranslationKey();
        return effectLimits.getOrDefault(key, maxInstantEffectLevel);
    }
}
