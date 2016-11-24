package net.shadowfacts.sleepingbag;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUseBed;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.shadowfacts.shadowmc.ShadowMC;

import java.util.List;
import java.util.UUID;

/**
 * @author shadowfacts
 */
public class ItemSleepingBag extends ItemArmor {

	private static final String TAG_SPAWN = "Spawn";
	private static final String TAG_POSITION = "Position";
	private static final String TAG_SLEEPING = "Sleeping";
	private static final String TAG_SLOT = "Slot";

	private static final int CHESTPIECE_SLOT = 2;
	private static final int OFF_HAND = 106;

	private static final int MESSAGE_ID = 8472;

	public ItemSleepingBag() {
		super(ArmorMaterial.IRON, 2, EntityEquipmentSlot.CHEST);
		setCreativeTab(CreativeTabs.MISC);
		setUnlocalizedName("sleepingbag");
		setRegistryName("sleepingbag");
	}

	@Override
	public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
		return "sleepingbag:textures/models/sleepingbag.png";
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, EntityEquipmentSlot slot, ModelBiped _default) {
		return slot == EntityEquipmentSlot.CHEST ? ModelSleepingBag.instance : _default;
	}

	public static EnumActionResult useSleepingBag(EntityPlayer player, World world, BlockPos pos, EnumHand hand) {
		int slot = hand == EnumHand.OFF_HAND ? OFF_HAND : player.inventory.currentItem;
		ItemStack stack = player.getHeldItem(hand);

		if (!world.isRemote) {
			ItemStack currentArmor = player.inventory.armorInventory.get(CHESTPIECE_SLOT);
			if (!currentArmor.isEmpty()) {
				currentArmor = currentArmor.copy();
			}

			final ItemStack sleepingBagCopy = stack.copy();

			if (sleepingBagCopy.getTagCompound() == null) sleepingBagCopy.setTagCompound(new NBTTagCompound());
			NBTTagCompound tag = sleepingBagCopy.getTagCompound();

			tag.setInteger(TAG_SLOT, slot);

			player.inventory.armorInventory.set(CHESTPIECE_SLOT, sleepingBagCopy);
			if (slot == OFF_HAND) {
				player.inventory.offHandInventory.set(0, currentArmor);
			} else {
				player.inventory.setInventorySlotContents(slot, currentArmor);
			}
			return EnumActionResult.SUCCESS;
		}
		player.inventory.setInventorySlotContents(slot, stack);
		return EnumActionResult.SUCCESS;
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		return useSleepingBag(player, world, pos, hand);
	}

	@Override
	public boolean isValidArmor(ItemStack stack, EntityEquipmentSlot slot, Entity entity) {
		return slot == EntityEquipmentSlot.CHEST;
	}

	@Override
	public void onArmorTick(World world, EntityPlayer player, ItemStack stack) {
		if (!(player instanceof EntityPlayerMP)) return;
		if (player.isPlayerSleeping()) return;

		if (stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		NBTTagCompound tag = stack.getTagCompound();
		if (tag.getBoolean(TAG_SLEEPING)) {
			restoreOriginalSpawn(player, tag);
			restoreOriginalPosition(player, tag);
			tag.removeTag(TAG_SLEEPING);
			getOutOfSleepingBag(player);
		} else {
			int posX = MathHelper.floor(player.posX);
			int posY = MathHelper.floor(player.posY);
			int posZ = MathHelper.floor(player.posZ);
			BlockPos pos = new BlockPos(posX, posY, posZ);

			if (canPlayerSleep(player, world, pos)) {
				storeOriginalSpawn(player, tag);
				storeOriginalPosition(player, tag);
				tag.setBoolean(TAG_SLEEPING, true);
				sleepSafe((EntityPlayerMP)player, world, pos);
			} else {
				getOutOfSleepingBag(player);
			}
		}
	}

	private static void sleepSafe(EntityPlayerMP player, World world, BlockPos pos) {
		if (player.isRiding()) player.dismountRidingEntity();

		ObfuscationReflectionHelper.setPrivateValue(EntityPlayer.class, player, true, "sleeping", "field_71083_bS");
		ObfuscationReflectionHelper.setPrivateValue(EntityPlayer.class, player, 0, "sleepTimer", "field_71076_b");

		AxisAlignedBB box = world.getBlockState(pos).getCollisionBoundingBox(world, pos);
		if (box == null || box.maxY < 1) pos = pos.up();

		player.bedLocation = pos;
		SleepingBag.network.sendTo(new PacketSetBedLocation(pos), player);

		player.motionX = player.motionZ = player.motionY = 0;
		world.updateAllPlayersSleepingFlag();

		SPacketUseBed sleepPacket = new SPacketUseBed(player, pos);
		player.getServerWorld().getEntityTracker().sendToTracking(player, sleepPacket);
		player.connection.sendPacket(sleepPacket);
	}

	private static EntityPlayer.SleepResult vanillaCanSleep(EntityPlayer player, World world, BlockPos pos) {
		PlayerSleepInBedEvent event = new PlayerSleepInBedEvent(player, pos);
		MinecraftForge.EVENT_BUS.post(event);
		if (event.getResultStatus() != null) return event.getResultStatus();

		if (!world.provider.isSurfaceWorld()) return EntityPlayer.SleepResult.NOT_POSSIBLE_HERE;
		if (world.isDaytime()) return EntityPlayer.SleepResult.NOT_POSSIBLE_NOW;

		Vec3i vec = new Vec3i(8, 5, 8);
		List<EntityMob> mobs = world.getEntitiesWithinAABB(EntityMob.class, new AxisAlignedBB(pos.subtract(vec), pos.add(vec)));
		if (!mobs.isEmpty()) return EntityPlayer.SleepResult.NOT_SAFE;

		return EntityPlayer.SleepResult.OK;
	}

	private static boolean canPlayerSleep(EntityPlayer player, World world, BlockPos pos) {
		if (player.isPlayerSleeping() || !player.isEntityAlive()) return false;

		if (!isSolidEnough(world, pos.down())) {
			ShadowMC.proxy.sendSpamlessMessage(player, new TextComponentTranslation("sleepingbag.no_ground"), MESSAGE_ID);
			return false;
		}

		EntityPlayer.SleepResult status = vanillaCanSleep(player, world, pos);

		if (status == EntityPlayer.SleepResult.OK) {
			return true;
		} else if (status == EntityPlayer.SleepResult.NOT_POSSIBLE_NOW) {
			ShadowMC.proxy.sendSpamlessMessage(player, new TextComponentTranslation("tile.bed.noSleep"), MESSAGE_ID);
		} else if (status == EntityPlayer.SleepResult.NOT_SAFE) {
			ShadowMC.proxy.sendSpamlessMessage(player, new TextComponentTranslation("tile.bed.notSafe"), MESSAGE_ID);
		}

		return false;
	}

	private static boolean isSolidEnough(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
		if (box == null) return false;

		double dx = box.maxX - box.minX;
		double dy = box.maxY - box.minY;
		double dz = box.maxZ - box.minZ;

		return dx >= 0.5 && dy >= 0.5 && dz >= 0.5;
	}

	private static void getOutOfSleepingBag(EntityPlayer player) {
		ItemStack stack = player.inventory.armorInventory.get(CHESTPIECE_SLOT);
		if (!stack.isEmpty() && stack.getItem() == SleepingBag.sleepingBag) {
			if (!tryReturnToSlot(player, stack)) {
				if (!player.inventory.addItemStackToInventory(stack)) {
					float f = 0.7f;
					float d0 = player.world.rand.nextFloat() * f + (1 - f) * 0.5f;
					float d1 = player.world.rand.nextFloat() * f + (1 - f) * 0.5f;
					float d2 = player.world.rand.nextFloat() * f + (1 - f) * 0.5f;
					EntityItem item = new EntityItem(player.world, player.posX + d0, player.posY + d1, player.posZ + d2, stack);
					item.setDefaultPickupDelay();
					if (stack.hasTagCompound()) {
						item.getEntityItem().setTagCompound(stack.getTagCompound().copy());
					}
					player.world.spawnEntity(item);
				}
			}
		}
	}

	private static boolean tryReturnToSlot(EntityPlayer player, ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		int returnSlot = tag.getInteger(TAG_SLOT);
		tag.removeTag(TAG_SLOT);

		ItemStack possiblyArmor;
		if (returnSlot == OFF_HAND) {
			possiblyArmor = player.inventory.offHandInventory.get(0);
		} else {
			possiblyArmor = player.inventory.getStackInSlot(returnSlot);
		}

		if (isChestplateOrElytra(possiblyArmor)) {
			player.inventory.armorInventory.set(CHESTPIECE_SLOT, possiblyArmor);
		} else {
			player.inventory.armorInventory.set(CHESTPIECE_SLOT, ItemStack.EMPTY);
			if (!possiblyArmor.isEmpty()) return false;
		}

		if (returnSlot == OFF_HAND) {
			player.inventory.offHandInventory.set(0, stack);
		} else {
			player.inventory.setInventorySlotContents(returnSlot, stack);
		}

		return true;
	}

	private static boolean isChestplateOrElytra(ItemStack stack) {
		if (stack == null) return false;
		Item item = stack.getItem();
		if (item instanceof ItemSleepingBag) return false;

		if (item instanceof ItemArmor) {
			ItemArmor armor = (ItemArmor)item;
			return armor.armorType == EntityEquipmentSlot.CHEST;
		}
		return (item instanceof ItemElytra);
	}

	private static void storeOriginalSpawn(EntityPlayer player, NBTTagCompound tag) {
		BlockPos pos = player.getBedLocation(player.world.provider.getDimension());
		if (pos != null) {
			tag.setLong(TAG_SPAWN, pos.toLong());
		}
	}

	private static void restoreOriginalSpawn(EntityPlayer player, NBTTagCompound tag) {
		if (tag.hasKey(TAG_SPAWN)) {
			BlockPos pos = BlockPos.fromLong(tag.getLong(TAG_SPAWN));
			player.setSpawnChunk(pos, false, player.world.provider.getDimension());
			tag.removeTag(TAG_SPAWN);
		}
	}

	private static void storeOriginalPosition(EntityPlayer player, NBTTagCompound tag) {
		NBTTagCompound posTag = new NBTTagCompound();
		posTag.setDouble("x", player.posX);
		posTag.setDouble("y", player.posY);
		posTag.setDouble("z", player.posZ);
		tag.setTag(TAG_POSITION, posTag);
	}

	private static void restoreOriginalPosition(EntityPlayer player, NBTTagCompound tag) {
		if (tag.hasKey(TAG_POSITION)) {
			NBTTagCompound posTag = tag.getCompoundTag(TAG_POSITION);
			player.setPosition(posTag.getDouble("x"), posTag.getDouble("y"), posTag.getDouble("z"));
			tag.removeTag(TAG_POSITION);
		}
	}


	public static boolean isWearingSleepingBag(EntityPlayer player) {
		ItemStack armor = player.inventory.armorInventory.get(CHESTPIECE_SLOT);
		return !armor.isEmpty() && armor.getItem() == SleepingBag.sleepingBag;
	}
}
