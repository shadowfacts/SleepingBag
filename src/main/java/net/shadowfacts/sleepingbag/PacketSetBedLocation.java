package net.shadowfacts.sleepingbag;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.shadowfacts.shadowmc.ShadowMC;
import net.shadowfacts.shadowmc.network.PacketBase;

/**
 * @author shadowfacts
 */
@NoArgsConstructor
@AllArgsConstructor
public class PacketSetBedLocation extends PacketBase<PacketSetBedLocation, IMessage> {

	public BlockPos bedLocation;

	@Override
	public IMessage onMessage(PacketSetBedLocation message, MessageContext ctx) {
		ShadowMC.proxy.getClientPLayer().bedLocation = message.bedLocation;
		return null;
	}

}
