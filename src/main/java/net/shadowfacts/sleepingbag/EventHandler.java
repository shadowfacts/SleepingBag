package net.shadowfacts.sleepingbag;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.SleepingLocationCheckEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

/**
 * @author shadowfacts
 */
public class EventHandler {

	@SubscribeEvent
	public void onPlayerRightClickAir(PlayerInteractEvent.RightClickEmpty event) {
		ItemStack offHand = event.getEntityPlayer().getHeldItemOffhand();
		ItemStack mainHand = event.getEntityPlayer().getHeldItemMainhand();
		EnumHand hand = offHand != null && offHand.getItem() == SleepingBag.sleepingBag ? EnumHand.OFF_HAND : mainHand != null && mainHand.getItem() == SleepingBag.sleepingBag ? EnumHand.MAIN_HAND : null;

		if (hand != null) {
			UUID uuid = event.getEntityPlayer().getUniqueID();
			int dimension = event.getWorld().provider.getDimension();
			BlockPos pos = event.getPos();
			SleepingBag.network.sendToServer(new PacketSleep(uuid, dimension, pos, hand));
		}
	}

	@SubscribeEvent
	public void handleSleepLocationCheck(SleepingLocationCheckEvent event) {
		if (ItemSleepingBag.isWearingSleepingBag(event.getEntityPlayer())) {
			event.setResult(Event.Result.ALLOW);
		}
	}

}
