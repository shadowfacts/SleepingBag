package net.shadowfacts.sleepingbag.mixin.common;

import net.minecraft.block.IBlockState;
import net.minecraft.block.impl.BlockBed;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.reference.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author shadowfacts
 */
@Mixin(value = EntityPlayer.class, remap = false)
public abstract class MixinEntityPlayer extends EntityLiving {

	@Shadow private BlockPos sleepingPos;
	@Shadow private boolean sleeping;
	@Shadow private int sleepTimer;

	public MixinEntityPlayer(World a1) {
		super(a1);
	}

	@Shadow public void a(BlockPos a1, boolean a2) {}

	@Overwrite
	public void a(boolean a1, boolean a2, boolean a3) {
		this.a(0.6F, 1.8F);
		if(this.sleepingPos != null) {
			IBlockState v1 = this.world.getBlockState(this.sleepingPos);
			if (v1.getBlock() == Blocks.BED) {
				this.world.setBlockState(this.sleepingPos, v1.with(BlockBed.OCCUPIED, Boolean.valueOf(false)), 4);
				BlockPos v2 = BlockBed.a(this.world, this.sleepingPos, 0);
				if(v2 == null) {
					v2 = this.sleepingPos.up();
				}

				this.setPosition((double)((float)v2.getX() + 0.5F), (double)((float)v2.getY() + 0.1F), (double)((float)v2.getZ() + 0.5F));
			}
		}

		this.sleeping = false;
		if(!this.world.isRemote && a2) {
			this.world.updateSleepingStatus();
		}

		this.sleepTimer = a1?0:100;
		if(a3) {
			this.a(this.sleepingPos, false);
		}

	}

}
