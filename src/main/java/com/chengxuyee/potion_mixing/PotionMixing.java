package com.chengxuyee.potion_mixing;

import com.chengxuyee.potion_mixing.config.ConfigLoader;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PotionMixing implements ModInitializer {
	public static final String MOD_ID = "potion_mixing";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ConfigLoader.loadConfig();
	}
}
