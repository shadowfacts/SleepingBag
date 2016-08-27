package net.shadowfacts.sleepingbag;

import net.fabricmc.api.Hook;
import net.fabricmc.api.Side;
import net.fabricmc.api.Sided;
import net.fabricmc.base.Fabric;
import net.fabricmc.event.entity.PlayerArmorTickEvent;
import net.fabricmc.event.entity.PlayerTrySleepEvent;
import net.minecraft.block.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.impl.EntityItem;
import net.minecraft.entity.mob.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerServer;
import net.minecraft.gui.CreativeTab;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.TagCompound;
import net.minecraft.network.packet.client.CPacketPlayerUseBed;
import net.minecraft.text.impl.TextComponentTranslatable;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

import java.util.List;

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

	public ItemSleepingBag() {
		super(ArmorMaterial.IRON, 2, EquipmentSlot.CHEST);
		setCreativeTab(CreativeTab.MISC);
		setTranslationKey("sleepingbag");
	}

//	@Override
//	public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
//		return "sleepingbag:textures/models/sleepingbag.png";
//	}
//
//	@Override
//	@Sided(Side.CLIENT)
//	public ModelBiped getArmorModel(EntityLiving entityLiving, ItemStack itemStack, EquipmentSlot slot, ModelBiped _default) {
//		return slot == EquipmentSlot.CHEST ? ModelSleepingBag.instance : _default;
//	}


	@Override
	public TypedActionResult<ItemStack> onRightClick(World world, EntityPlayer player, Hand hand) {
		return new TypedActionResult<>(ActionResult.PASS, player.getStackInHand(hand));
	}

	@Override
	public ActionResult activate(EntityPlayer player, World world, BlockPos pos, Hand hand, Facing facing, float hitX, float hitY, float hitZ) {
		ItemStack stack = player.getStackInHand(hand);
		int slot = hand == Hand.OFF ? OFF_HAND : player.inventory.selectedSlot;

		if (!world.isRemote) {
			ItemStack currentArmor = player.inventory.armor.get(CHESTPIECE_SLOT);
			if (currentArmor != null) {
				currentArmor = currentArmor.copy();
			}

			final ItemStack sleepingBagCopy = stack.copy();

			if (!sleepingBagCopy.hasTag()) sleepingBagCopy.setTag(new TagCompound());
			TagCompound tag = sleepingBagCopy.getTag();

			tag.setInt(TAG_SLOT, slot);

			player.inventory.armor.set(CHESTPIECE_SLOT, sleepingBagCopy);
			if (slot == OFF_HAND) {
				player.inventory.offHand.set(0, currentArmor);
			} else {
				player.inventory.setInvStack(slot, currentArmor);
			}
			return ActionResult.SUCCESS;
		}
		player.inventory.setInvStack(slot, stack);
		return ActionResult.SUCCESS;
	}

//	@Override
//	public boolean isValidArmor(ItemStack stack, EquipmentSlot slot, Entity entity) {
//		return slot == EquipmentSlot.CHEST;
//	}

	@Hook(name = "sleepingbag:onArmorTick", before = {}, after = {})
	public void onArmorTick(PlayerArmorTickEvent event) {
		EntityPlayer player = event.getPlayer();
		World world = player.getWorld();
		ItemStack stack = event.getStack();

		if (!(player instanceof EntityPlayerServer)) return;
		if (player.sleeping) return;

		if (stack.getTag() == null) {
			stack.setTag(new TagCompound());
		}
		TagCompound tag = stack.getTag();
		if (tag.getBoolean(TAG_SLEEPING)) {
			restoreOriginalSpawn(player, tag);
			restoreOriginalPosition(player, tag);
			tag.removeTag(TAG_SLEEPING);
			getOutOfSleepingBag(player);
		} else {
			int posX = MathUtils.floor(player.x);
			int posY = MathUtils.floor(player.y);
			int posZ = MathUtils.floor(player.z);
			BlockPos pos = new BlockPos(posX, posY, posZ);

			if (canPlayerSleep(player, world, pos)) {
				storeOriginalSpawn(player, tag);
				storeOriginalPosition(player, tag);
				tag.setBoolean(TAG_SLEEPING, true);
				sleepSafe((EntityPlayerServer)player, world, pos);
			} else {
				getOutOfSleepingBag(player);
			}
		}
	}

	private static void sleepSafe(EntityPlayerServer player, World world, BlockPos pos) {
		if (player.getVehicle() != null) player.dismountVehicle();

		ReflectionHelper.set(EntityPlayer.class, player, true, "sleeping");
		ReflectionHelper.set(EntityPlayer.class, player, 0, "sleepTimer");

		((EntityPlayer)player).g = pos;

		player.velocityX = player.velocityY = player.velocityZ = 0;
		world.updateSleepingStatus();

		CPacketPlayerUseBed sleepPacket = new CPacketPlayerUseBed(player, pos);
		player.getWorldServer().getEntityTracker().sendToAllTrackingAndSelf(player, sleepPacket);
		player.networkHandler.sendPacket(sleepPacket);
	}

	private static EntityPlayer.SleepResult vanillaCanSleep(EntityPlayer player, World world, BlockPos pos) {
		PlayerTrySleepEvent event = new PlayerTrySleepEvent(player, pos);
		Fabric.getEventBus().publish(event);
		if (event.getResult() != null) return event.getResult();

		if (world.getProperties().dimension == DimensionType.OVERWORLD.getID()) return EntityPlayer.SleepResult.WRONG_DIMENSION;
		if (world.getProperties().getTimeOfDay() % 24000 < 12000) return EntityPlayer.SleepResult.WRONG_TIME;

		Vec3i vec = new Vec3i(8, 5, 8);
		List<EntityMob> mobs = world.getEntitiesIn(EntityMob.class, new BoundingBox(pos.subtract(vec), pos.add(vec)));
		if (!mobs.isEmpty()) return EntityPlayer.SleepResult.NOT_SAFE;

		return EntityPlayer.SleepResult.SUCCESS;
	}

	private static boolean canPlayerSleep(EntityPlayer player, World world, BlockPos pos) {
		if (player.sleeping || !player.isValid()) return false;

		if (!isNotSuffocating(world, pos) || !isSolidEnough(world, pos.down())) {
			player.addChatMsg(new TextComponentTranslatable("sleepingbag.no_ground"), true);
			return false;
		}

		EntityPlayer.SleepResult status = vanillaCanSleep(player, world, pos);

		if (status == EntityPlayer.SleepResult.SUCCESS) {
			return true;
		} else if (status == EntityPlayer.SleepResult.WRONG_TIME) {
			player.addChatMsg(new TextComponentTranslatable("tile.bed.noSleep"), true);
		} else if (status == EntityPlayer.SleepResult.NOT_SAFE) {
			player.addChatMsg(new TextComponentTranslatable("tile.bed.notSafe"), true);
		}

		return false;
	}

	private static boolean isNotSuffocating(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		return state.getCollisionBox(world, pos) == null || state.getBlock().isAir(world, pos);
	}

	private static boolean isSolidEnough(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		BoundingBox box = state.getCollisionBox(world, pos);
		if (box == null) return false;

		double dx = box.maxX - box.minX;
		double dy = box.maxY - box.minY;
		double dz = box.maxZ - box.minZ;

		return dx >= 0.5 && dy >= 0.5 && dz >= 0.5;
	}

	private static void getOutOfSleepingBag(EntityPlayer player) {
		ItemStack stack = player.inventory.armor.get(CHESTPIECE_SLOT);
		if (stack != ItemStack.NULL_STACK && stack.getItem() == SleepingBag.sleepingBag) {
			if (!tryReturnToSlot(player, stack)) {
				if (!player.inventory.insertStack(stack)) {
					float f = 0.7f;
					float d0 = player.world.rand.nextFloat() * f + (1 - f) * 0.5f;
					float d1 = player.world.rand.nextFloat() * f + (1 - f) * 0.5f;
					float d2 = player.world.rand.nextFloat() * f + (1 - f) * 0.5f;
					EntityItem item = new EntityItem(player.world, player.x + d0, player.y + d1, player.z + d2, stack);
					item.j();
					if (stack.hasTag()) {
						item.getStack().setTag(stack.getTag().copy());
					}
					player.world.spawnEntity(item);
				}
			}
		}
	}

	private static boolean tryReturnToSlot(EntityPlayer player, ItemStack stack) {
		TagCompound tag = stack.getTag();
		int returnSlot = tag.getInt(TAG_SLOT);
		tag.removeTag(TAG_SLOT);

		ItemStack possiblyArmor;
		if (returnSlot == OFF_HAND) {
			possiblyArmor = player.inventory.offHand.get(0);
		} else {
			possiblyArmor = player.inventory.getInvStack(returnSlot);
		}

		if (isChestplate(possiblyArmor)) {
			player.inventory.armor.set(CHESTPIECE_SLOT, possiblyArmor);
		} else {
			player.inventory.armor.set(CHESTPIECE_SLOT, ItemStack.NULL_STACK);
			if (possiblyArmor != ItemStack.NULL_STACK) return false;
		}

		if (returnSlot == OFF_HAND) {
			player.inventory.offHand.set(0, stack);
		} else {
			player.inventory.setInvStack(returnSlot, stack);
		}

		return true;
	}

	private static boolean isChestplate(ItemStack stack) {
		if (stack == null) return false;
		Item item = stack.getItem();
		if (item instanceof ItemSleepingBag) return false;

		if (item instanceof ItemArmor) {
			ItemArmor armor = (ItemArmor)item;
			return armor.slotType == EquipmentSlot.CHEST;
		}

		return false;
	}

	private static void storeOriginalSpawn(EntityPlayer player, TagCompound tag) {
		BlockPos pos = player.spawnPosition;
		if (pos != null) {
			tag.setLong(TAG_SPAWN, pos.toLong());
		}
	}

	private static void restoreOriginalSpawn(EntityPlayer player, TagCompound tag) {
		if (tag.hasKey(TAG_SPAWN)) {
			player.spawnPosition = BlockPos.fromLong(tag.getLong(TAG_SPAWN));
			tag.removeTag(TAG_SPAWN);
		}
	}

	private static void storeOriginalPosition(EntityPlayer player, TagCompound tag) {
		TagCompound posTag = new TagCompound();
		posTag.setDouble("x", player.x);
		posTag.setDouble("y", player.y);
		posTag.setDouble("z", player.z);
		tag.setTag(TAG_POSITION, posTag);
	}

	private static void restoreOriginalPosition(EntityPlayer player, TagCompound tag) {
		if (tag.hasKey(TAG_POSITION)) {
			TagCompound posTag = tag.getTagCompound(TAG_POSITION);
			player.setPosition(posTag.getDouble("x"), posTag.getDouble("y"), posTag.getDouble("z"));
			tag.removeTag(TAG_POSITION);
		}
	}

	@Hook(name = "sleepingbag:onTrySleep", before = {}, after = {})
	public void onTrySleep(PlayerTrySleepEvent event) {
		ItemStack armor = event.getPlayer().inventory.armor.get(CHESTPIECE_SLOT);
		if (armor != ItemStack.NULL_STACK && armor.getItem() == SleepingBag.sleepingBag) {
			event.setResult(EntityPlayer.SleepResult.SUCCESS);
		}
	}

}
