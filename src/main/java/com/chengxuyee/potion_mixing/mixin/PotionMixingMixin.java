package com.chengxuyee.potion_mixing.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreenHandler.class)
public abstract class PotionMixingMixin {
	private static byte air;
	private static byte potion;
	private static byte splash_potion;
	private static byte lingering_potion;
	private static ItemStack itemStack;

	@Inject(method = "updateResult", at = @At("TAIL"))

	private static void injected(ScreenHandler handler,
								 World world,
								 PlayerEntity player,
								 RecipeInputInventory craftingInventory,
								 CraftingResultInventory resultInventory,
								 @Nullable RecipeEntry<CraftingRecipe> recipe,
								 CallbackInfo ci) {
		if(!world.isClient){
			//Check if the player is trying to mix the potions together.
			air = (byte)0;
			potion = (byte)0;
			splash_potion = (byte)0;
			lingering_potion = (byte)0;
			for(int i = 0; i <= 8; i++){
				ItemStack stack = craftingInventory.getStack(i);
				if(stack.getItem() == Items.POTION){
					potion += (byte)1;
				}else if(stack.getItem() == Items.SPLASH_POTION){
					splash_potion += (byte)1;
				}else if(stack.getItem() == Items.LINGERING_POTION){
					lingering_potion += (byte)1;
				}else if(stack.getItem() == Items.AIR){
					air += (byte)1;
				}
			}
			if(air + potion + splash_potion + lingering_potion == (byte)9 && potion + splash_potion + lingering_potion > 0) {
				//Beginning of the new codes
				if(splash_potion > potion && splash_potion > lingering_potion){
					itemStack = new ItemStack(Items.SPLASH_POTION);
				}else if(lingering_potion > potion && lingering_potion > splash_potion){
					itemStack = new ItemStack(Items.LINGERING_POTION);
				}else{
					itemStack = new ItemStack(Items.POTION);
				}
				PotionContentsComponent component = PotionContentsComponent.DEFAULT;
				for(int i = 0; i <= 8; i++){
					ItemStack ij = craftingInventory.getStack(i);
					if(ij.getItem() != Items.AIR){
						for (StatusEffectInstance k : ij.get(DataComponentTypes.POTION_CONTENTS).getEffects()) {
							component = component.with(k);
						}
					}
				}
				itemStack.set(DataComponentTypes.POTION_CONTENTS, component);
				if(itemStack.getItem() == Items.POTION){
					itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(MinecraftClient.getInstance().getLanguageManager().getLanguage().equals("zh_cn") ? "§f混合的药水" : "§fMixed Potion"));
				}else if(itemStack.getItem() == Items.SPLASH_POTION){
					itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(MinecraftClient.getInstance().getLanguageManager().getLanguage().equals("zh_cn") ? "§f喷溅型混合药水" : "§fMixed Splash Potion"));
				}else{
					itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(MinecraftClient.getInstance().getLanguageManager().getLanguage().equals("zh_cn") ? "§f滞留型混合药水" : "§fMixed Lingering Potion"));
				}
				//End of the new codes
				ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) player;
				resultInventory.setStack(0, itemStack);
				handler.setPreviousTrackedSlot(0, itemStack);
				serverPlayerEntity.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 0, itemStack));
			}
		}
	}
}