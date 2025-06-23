package com.chengxuyee.potion_mixing.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashMap;

public class ConfigLoader {
    private static final Logger LOGGER = LogManager.getLogger("PotionMixing");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PotionMixingConfig config = new PotionMixingConfig(); // 确保初始值不为null

    public static PotionMixingConfig getConfig() {
        return config;
    }

    public static void loadConfig() {
        Path configPath = getConfigPath();
        File configFile = configPath.toFile();

        try {
            if (configFile.exists()) {
                LOGGER.info("Loading config from {}", configFile.getAbsolutePath());
                try (FileReader reader = new FileReader(configFile)) {
                    PotionMixingConfig loadedConfig = GSON.fromJson(reader, PotionMixingConfig.class);
                    if (loadedConfig != null) {
                        config = loadedConfig;
                        LOGGER.info("Config loaded successfully");
                    } else {
                        LOGGER.warn("Loaded config is null, using defaults");
                    }
                }
            } else {
                LOGGER.info("Creating new config file at {}", configFile.getAbsolutePath());
                saveConfig();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load config, using defaults", e);
        }
        validateConfig();
    }

    public static void saveConfig() {
        Path configPath = getConfigPath();
        File configFile = configPath.toFile();

        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(config, writer);
                LOGGER.info("Saved config to {}", configFile.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    private static void validateConfig() {
        if (config.maxInstantEffectLevel < 1) {
            config.maxInstantEffectLevel = 1;
            LOGGER.warn("Adjusted maxInstantEffectLevel to minimum value 1");
        }

        if (config.maxEffectEntries < 1) {
            config.maxEffectEntries = 1;
            LOGGER.warn("Adjusted maxEffectEntries to minimum value 1");
        }
        if (config.effectLimits == null) {
            config.effectLimits = new HashMap<>();
            LOGGER.warn("Initialized null effectLimits map");
        }
        addDefaultLimit(StatusEffects.INSTANT_HEALTH.value());
        addDefaultLimit(StatusEffects.INSTANT_DAMAGE.value());
    }

    private static void addDefaultLimit(StatusEffect effect) {
        String key = effect.getTranslationKey();
        if (!config.effectLimits.containsKey(key)) {
            config.effectLimits.put(key, config.maxInstantEffectLevel);
            LOGGER.info("Added default limit for {}: {}", key, config.maxInstantEffectLevel);
        }
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("potionmixing.json");
    }
}
