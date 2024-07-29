package com.chengxuyee.potion_mixing.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


//HOW CAN I WRITE SUCH SHITTY CODE??????????????
@Mixin(CraftingScreenHandler.class)
public abstract class UpdateResultMixin{
	private static byte air;
	private static byte potion;
	private static byte splashPotion;
	private static byte lingeringPotion;
	private static ItemStack itemStack;
	private static PotionContentsComponent component;
	private static ServerPlayerEntity serverPlayerEntity;
	private static boolean newEffect;

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
			splashPotion = (byte)0;
			lingeringPotion = (byte)0;
			for(int i = 0; i <= 8; i++){
				ItemStack stack = craftingInventory.getStack(i);
				if(stack.getItem() == Items.POTION){
					potion += (byte)1;
				}else if(stack.getItem() == Items.SPLASH_POTION){
					splashPotion += (byte)1;
				}else if(stack.getItem() == Items.LINGERING_POTION){
					lingeringPotion += (byte)1;
				}else if(stack.getItem() == Items.AIR){
					air += (byte)1;
				}
			}
			if(air + potion + splashPotion + lingeringPotion == (byte)9 && potion + splashPotion + lingeringPotion > 1) {
				//Beginning of the new codes
				if(splashPotion > potion && splashPotion > lingeringPotion){
					itemStack = new ItemStack(Items.SPLASH_POTION);
				}else if(lingeringPotion > potion && lingeringPotion > splashPotion){
					itemStack = new ItemStack(Items.LINGERING_POTION);
				}else if(splashPotion == lingeringPotion && splashPotion > potion){
					itemStack = new ItemStack(Items.SPLASH_POTION);
				}else{
					itemStack = new ItemStack(Items.POTION);
				}
				component = PotionContentsComponent.DEFAULT;
				for(int i = 0; i <= 8; i++){
					ItemStack ij = craftingInventory.getStack(i);
					if(ij.getItem() != Items.AIR){
						for (StatusEffectInstance k : ij.get(DataComponentTypes.POTION_CONTENTS).getEffects()) {
							newEffect = true;
							if(!(k.getEffectType().equals(StatusEffects.INSTANT_HEALTH) || k.getEffectType().equals(StatusEffects.INSTANT_DAMAGE))){
								for(StatusEffectInstance p : component.getEffects()){
									if(p.getEffectType().equals(k.getEffectType())){
										if(newEffect){
											newEffect = false;
										}
										if(k.getAmplifier() > p.getAmplifier()){
											component = replace(component, p, k);
										}else if(k.getAmplifier() == p.getAmplifier()){
											if(k.getDuration() > p.getDuration()){
												component = replace(component, p, k);
											}
										}
										break;
									}
								}
							}
							if(newEffect){
								component = component.with(k);
							}
						}
					}
				}
				itemStack.set(DataComponentTypes.POTION_CONTENTS, component);
				if(itemStack.getItem() == Items.POTION){
					itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§fMixed Potion"));
				}else if(itemStack.getItem() == Items.SPLASH_POTION){
					itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§fMixed Splash Potion"));
				}else{
					itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§fMixed Lingering Potion"));
				}
				//End of the new codes
				serverPlayerEntity = (ServerPlayerEntity) player;
				resultInventory.setStack(0, itemStack);
				handler.setPreviousTrackedSlot(0, itemStack);
				serverPlayerEntity.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 0, itemStack));
			}
		}
	}

	private static PotionContentsComponent replace(PotionContentsComponent component, StatusEffectInstance effect1, StatusEffectInstance effect2){
		PotionContentsComponent component1 = PotionContentsComponent.DEFAULT;
		for(StatusEffectInstance l : component.customEffects()){
			if(l.equals(effect1)){
				component1 = component1.with(effect2);
			}else{
				component1 = component1.with(l);
			}
		}
		return component1;
	}
}
