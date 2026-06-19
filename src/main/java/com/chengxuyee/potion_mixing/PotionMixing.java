package com.chengxuyee.potion_mixing;

import com.chengxuyee.potion_mixing.config.ConfigLoader;
import net.fabricmc.api.ModInitializer;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PotionMixing implements ModInitializer {
    public static final String MOD_ID = "potion_mixing";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final RecipeSerializer<PotionMixingRecipe> POTION_MIXING_RECIPE_SERIALIZER =
            new SpecialRecipeSerializer<>(PotionMixingRecipe::new);

    @Override
    public void onInitialize() {
        Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(MOD_ID, "potion_mixing"),
                POTION_MIXING_RECIPE_SERIALIZER
        );

        ConfigLoader.loadConfig();

        LOGGER.info("Potion Mixing initialized");
    }
}
