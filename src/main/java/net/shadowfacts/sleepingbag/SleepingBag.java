package net.shadowfacts.sleepingbag;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

/**
 * @author shadowfacts
 */
@Mod(modid = SleepingBag.MOD_ID, name = SleepingBag.NAME, version = SleepingBag.VERSION, dependencies = "required-after:shadowmc@[3.7.0,);")
public class SleepingBag {

	public static final String MOD_ID = "sleepingbag";
	public static final String NAME = "Sleeping Bag";
	public static final String VERSION = "@VERSION@";

	public static SimpleNetworkWrapper network;

//	Content
	public static ItemSleepingBag sleepingBag;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		sleepingBag = new ItemSleepingBag();
		GameRegistry.register(sleepingBag);

		if (event.getSide() == Side.CLIENT) {
			ModelLoader.setCustomModelResourceLocation(sleepingBag, 0, new ModelResourceLocation("sleepingbag:sleepingbag", "inventory"));
			MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
		}

		MinecraftForge.EVENT_BUS.register(new EventHandler());

		GameRegistry.addShapedRecipe(new ItemStack(sleepingBag), "-- ", "###", '-', Blocks.CARPET, '#', Blocks.WOOL);

		network = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID);
		network.registerMessage(PacketSleep.class, PacketSleep.class, 0, Side.SERVER);
		network.registerMessage(PacketSetBedLocation.class, PacketSetBedLocation.class, 1, Side.CLIENT);
	}

}
