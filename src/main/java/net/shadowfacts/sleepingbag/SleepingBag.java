package net.shadowfacts.sleepingbag;

import net.fabricmc.api.Hook;
import net.fabricmc.base.Fabric;
import net.fabricmc.base.loader.Init;
import net.fabricmc.event.entity.PlayerTrySleepEvent;
import net.fabricmc.registry.Registries;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeRegistry;
import net.minecraft.reference.Blocks;
import net.minecraft.util.Identifier;

public class SleepingBag {

//	Content
	public static ItemSleepingBag sleepingBag;

	@Init
	public void init() {
		sleepingBag = new ItemSleepingBag();

		if (Fabric.getSidedHandler().getSide().hasClient()) {
			initClient();
		}

		Fabric.getLoadingBus().subscribe(this);
		Fabric.getEventBus().subscribe(sleepingBag);

		RecipeRegistry.getInstance().addShapedRecipe(new ItemStack(sleepingBag), "-- ", "###", '-', Blocks.CARPET, '#', Blocks.WOOL);
	}

	private void initClient() {
//		TODO: register item model
	}

	@Hook(name = "sleepingbag:registerItems", before = {}, after = "fabric:registerItems")
	public void registerItems() {
		Registries.register(new Identifier("sleepingbag:sleepingbag"), sleepingBag);
	}

}
