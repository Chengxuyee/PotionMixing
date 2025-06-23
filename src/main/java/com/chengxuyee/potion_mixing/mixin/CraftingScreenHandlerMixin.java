package com.chengxuyee.potion_mixing.mixin;

import com.chengxuyee.potion_mixing.PotionMixingHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreenHandler.class)
public abstract class CraftingScreenHandlerMixin {
	@Inject(method = "updateResult", at = @At("TAIL"), cancellable = true)
	private static void handlePotionMixing(
			ScreenHandler handler, ServerWorld world, PlayerEntity player, RecipeInputInventory craftingInventory, CraftingResultInventory resultInventory, RecipeEntry<CraftingRecipe> recipe, CallbackInfo ci
	) {
		if (world.isClient || !isPotionMixRecipe(craftingInventory)) return;

		ItemStack mixedPotion = PotionMixingHandler.mixPotions(craftingInventory);
		resultInventory.setStack(0, mixedPotion);
		// 同步到客户端
		((ServerPlayerEntity) player).networkHandler.sendPacket(
				new ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 0, mixedPotion)
		);
		ci.cancel(); // 阻止原版处理
	}

	private static boolean isPotionMixRecipe(RecipeInputInventory inventory) {
		int potionCount = 0;
		for (int i = 0; i < 9; i++) {
			Item item = inventory.getStack(i).getItem();
			if (item == Items.AIR) continue;
			if (item != Items.POTION && item != Items.SPLASH_POTION && item != Items.LINGERING_POTION) {
				return false; // 存在非药水物品
			}
			potionCount++;
		}
		return potionCount >= 2; // 至少2瓶药水
	}
}
