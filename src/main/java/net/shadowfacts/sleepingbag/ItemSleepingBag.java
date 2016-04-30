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
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S0APacketUseBed;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.List;

/**
 * @author shadowfacts
 */
public class ItemSleepingBag extends ItemArmor {

	private static final String TAG_SPAWN = "Spawn";
	private static final String TAG_POSITION = "Position";
	private static final String TAG_SLEEPING = "Sleeping";
	private static final String TAG_SLOT = "Slot";

	private static final int CHESTPIECE_TYPE = 1;
	private static final int CHESTPIECE_SLOT = 2;

	public ItemSleepingBag() {
		super(ArmorMaterial.IRON, 2, 1);
		setCreativeTab(CreativeTabs.tabMisc);
		setUnlocalizedName("sleepingbag");
		setRegistryName("sleepingbag");
	}

	@Override
	public String getArmorTexture(ItemStack stack, Entity entity, int slot, String type) {
		return "sleepingbag:textures/models/sleepingbag.png";
	}

	@Override
	public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, int slot, ModelBiped _default) {
		return slot == CHESTPIECE_TYPE ? ModelSleepingBag.instance : _default;
	}

//	@Override
//	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
//		if (!world.isRemote) {
//			ItemStack currentArmor = player.inventory.armorInventory[CHESTPIECE_SLOT];
//			if (currentArmor != null) {
//				currentArmor = currentArmor.copy();
//			}
//
//			final ItemStack sleepingBagCopy = stack.copy();
//
//			if (sleepingBagCopy.getTagCompound() == null) sleepingBagCopy.setTagCompound(new NBTTagCompound());
//			NBTTagCompound tag = sleepingBagCopy.getTagCompound();
//
//			tag.setInteger(TAG_SLOT, player.inventory.currentItem);
//
//			player.inventory.armorInventory[CHESTPIECE_SLOT] = sleepingBagCopy;
//			if (currentArmor != null) {
//				return currentArmor;
//			}
//
//			stack.stackSize = 0;
//		}
//		return stack;
//	}


	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		return stack;
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (!world.isRemote) {
			ItemStack currentArmor = player.inventory.armorInventory[CHESTPIECE_SLOT];
			if (currentArmor != null) {
				currentArmor = currentArmor.copy();
			}

			final ItemStack sleepingBagCopy = stack.copy();

			if (sleepingBagCopy.getTagCompound() == null) sleepingBagCopy.setTagCompound(new NBTTagCompound());
			NBTTagCompound tag = sleepingBagCopy.getTagCompound();

			tag.setInteger(TAG_SLOT, player.inventory.currentItem);

			player.inventory.armorInventory[CHESTPIECE_SLOT] = sleepingBagCopy;
			if (currentArmor != null) {
				player.inventory.setInventorySlotContents(player.inventory.currentItem, currentArmor);
				return true;
//				return currentArmor;
			}


			player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
			return false;
//			stack.stackSize = 0;
		}
		player.inventory.setInventorySlotContents(player.inventory.currentItem, stack);
		return true;
	}

	@Override
	public boolean isValidArmor(ItemStack stack, int type, Entity entity) {
		return type == CHESTPIECE_TYPE;
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
			int posX = MathHelper.floor_double(player.posX);
			int posY = MathHelper.floor_double(player.posY);
			int posZ = MathHelper.floor_double(player.posZ);
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
		if (player.isRiding()) player.mountEntity(null);

		ObfuscationReflectionHelper.setPrivateValue(EntityPlayer.class, player, true, "sleeping", "field_71083_bS");
		ObfuscationReflectionHelper.setPrivateValue(EntityPlayer.class, player, 0, "sleepTimer", "field_71076_b");

		player.playerLocation = pos;

		player.motionX = player.motionZ = player.motionY = 0;
		world.updateAllPlayersSleepingFlag();

		S0APacketUseBed sleepPacket = new S0APacketUseBed(player, pos);
		player.getServerForPlayer().getEntityTracker().func_151248_b(player, sleepPacket);
		player.playerNetServerHandler.sendPacket(sleepPacket);
	}

	private static EntityPlayer.EnumStatus vanillaCanSleep(EntityPlayer player, World world, BlockPos pos) {
		PlayerSleepInBedEvent event = new PlayerSleepInBedEvent(player, pos);
		MinecraftForge.EVENT_BUS.post(event);
		if (event.result != null) return event.result;

		if (!world.provider.isSurfaceWorld()) return EntityPlayer.EnumStatus.NOT_POSSIBLE_HERE;
		if (world.isDaytime()) return EntityPlayer.EnumStatus.NOT_POSSIBLE_NOW;

		Vec3i vec = new Vec3i(8, 5, 8);
		List<EntityMob> mobs = world.getEntitiesWithinAABB(EntityMob.class, new AxisAlignedBB(pos.subtract(vec), pos.add(vec)));
		if (!mobs.isEmpty()) return EntityPlayer.EnumStatus.NOT_SAFE;

		return EntityPlayer.EnumStatus.OK;
	}

	private static boolean canPlayerSleep(EntityPlayer player, World world, BlockPos pos) {
		if (player.isPlayerSleeping() || !player.isEntityAlive()) return false;

		if (!isNotSuffocating(world, pos) || !isSolidEnough(world, pos.down())) {
			player.addChatComponentMessage(new ChatComponentTranslation("sleepingbag.no_ground"));
			return false;
		}

		EntityPlayer.EnumStatus status = vanillaCanSleep(player, world, pos);

		if (status == EntityPlayer.EnumStatus.OK) {
			return true;
		} else if (status == EntityPlayer.EnumStatus.NOT_POSSIBLE_NOW) {
			player.addChatComponentMessage(new ChatComponentTranslation("tile.bed.noSleep"));
		} else if (status == EntityPlayer.EnumStatus.NOT_SAFE) {
			player.addChatComponentMessage(new ChatComponentTranslation("tile.bed.notSafe"));
		}

		return false;
	}

	private static boolean isNotSuffocating(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		return state.getBlock().getCollisionBoundingBox(world, pos, state) == null || state.getBlock().isAir(world, pos);
	}

	private static boolean isSolidEnough(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		AxisAlignedBB box = state.getBlock().getCollisionBoundingBox(world, pos, state);
		if (box == null) return false;

		double dx = box.maxX - box.minX;
		double dy = box.maxY - box.minY;
		double dz = box.maxZ - box.minZ;

		return dx >= 0.5 && dy >= 0.5 && dz >= 0.5;
	}

	private static void getOutOfSleepingBag(EntityPlayer player) {
		ItemStack stack = player.inventory.armorInventory[CHESTPIECE_SLOT];
		if (stack != null && stack.getItem() == SleepingBag.sleepingBag) {
			if (!tryReturnToSlot(player, stack)) {
				if (!player.inventory.addItemStackToInventory(stack)) {
					float f = 0.7f;
					float d0 = player.worldObj.rand.nextFloat() * f + (1 - f) * 0.5f;
					float d1 = player.worldObj.rand.nextFloat() * f + (1 - f) * 0.5f;
					float d2 = player.worldObj.rand.nextFloat() * f + (1 - f) * 0.5f;
					EntityItem item = new EntityItem(player.worldObj, player.posX + d0, player.posY + d1, player.posZ + d2, stack);
					item.setDefaultPickupDelay();
					if (stack.hasTagCompound()) {
						item.getEntityItem().setTagCompound((NBTTagCompound)stack.getTagCompound().copy());
					}
					player.worldObj.spawnEntityInWorld(item);
				}
			}
		}
	}

	private static boolean tryReturnToSlot(EntityPlayer player, ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		int returnSlot = tag.getInteger(TAG_SLOT);
		tag.removeTag(TAG_SLOT);

		ItemStack possiblyArmor = player.inventory.mainInventory[returnSlot];
		if (isChestplate(possiblyArmor)) {
			player.inventory.armorInventory[CHESTPIECE_SLOT] = possiblyArmor;
		} else {
			player.inventory.armorInventory[CHESTPIECE_SLOT] = null;
			if (possiblyArmor != null) return false;
		}

		player.inventory.setInventorySlotContents(returnSlot, stack);
		return true;
	}

	private static boolean isChestplate(ItemStack stack) {
		if (stack == null) return false;
		Item item = stack.getItem();
		if (item instanceof ItemSleepingBag) return false;

		if (item instanceof ItemArmor) {
			ItemArmor armor = (ItemArmor)item;
			return armor.armorType == CHESTPIECE_TYPE;
		}

		return false;
	}

	private static void storeOriginalSpawn(EntityPlayer player, NBTTagCompound tag) {
		BlockPos pos = player.getBedLocation(player.worldObj.provider.getDimensionId());
		if (pos != null) {
			tag.setLong(TAG_SPAWN, pos.toLong());
		}
	}

	private static void restoreOriginalSpawn(EntityPlayer player, NBTTagCompound tag) {
		if (tag.hasKey(TAG_SPAWN)) {
			BlockPos pos = BlockPos.fromLong(tag.getLong(TAG_SPAWN));
			player.setSpawnChunk(pos, false, player.worldObj.provider.getDimensionId());
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
		ItemStack armor = player.inventory.armorInventory[CHESTPIECE_SLOT];
		return armor != null && armor.getItem() == SleepingBag.sleepingBag;
	}
}
