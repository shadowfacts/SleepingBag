package net.shadowfacts.sleepingbag;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.shadowfacts.shadowmc.network.PacketBase;

import java.util.UUID;

/**
 * @author shadowfacts
 */
@NoArgsConstructor
@AllArgsConstructor
public class PacketSleep extends PacketBase<PacketSleep, IMessage> {

	public UUID id;
	public int dimension;
	public BlockPos pos;
	public EnumHand hand;

	@Override
	public IMessage onMessage(PacketSleep message, MessageContext ctx) {
		WorldServer world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(message.dimension);
		EntityPlayer player = world.getPlayerEntityByUUID(message.id);
		ItemSleepingBag.useSleepingBag(player, world, message.pos, message.hand);
		return null;
	}

}
