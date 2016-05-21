package net.shadowfacts.sleepingbag;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.SleepingLocationCheckEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

/**
 * @author shadowfacts
 */
@Mod(modid = SleepingBag.modId, name = SleepingBag.name, version = SleepingBag.version, acceptedMinecraftVersions = "[1.9.4]", dependencies = "required-after:shadowmc;")
public class SleepingBag {

	public static final String modId = "SleepingBag";
	public static final String name = "Sleeping Bag";
	public static final String version = "1.2.0";

//	Content
	public static ItemSleepingBag sleepingBag;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		sleepingBag = new ItemSleepingBag();
		GameRegistry.register(sleepingBag);

		if (event.getSide() == Side.CLIENT) {
			ModelLoader.setCustomModelResourceLocation(sleepingBag, 0, new ModelResourceLocation("sleepingbag:sleepingbag", "inventory"));
		}

		MinecraftForge.EVENT_BUS.register(this);

		GameRegistry.addShapedRecipe(new ItemStack(sleepingBag), "-- ", "###", '-', Blocks.CARPET, '#', Blocks.WOOL);
	}

	@SubscribeEvent
	public void handleSleepLocationCheck(SleepingLocationCheckEvent event) {
		if (ItemSleepingBag.isWearingSleepingBag(event.getEntityPlayer())) {
			event.setResult(Event.Result.ALLOW);
		}
	}

}
