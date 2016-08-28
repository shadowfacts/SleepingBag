package net.shadowfacts.sleepingbag.mixin.client;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.reference.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author shadowfacts
 */
@Mixin(value = EntityPlayer.class, remap = false)
public abstract class MixinEntityPlayer extends EntityLiving {

	@Shadow private BlockPos sleepingPos;

	public MixinEntityPlayer(World a1) {
		super(a1);
	}

	@Inject(method = "cQ()F", at = @At("HEAD"), cancellable = true)
	public void oncQ(CallbackInfoReturnable<Float> ci) {
		if (sleepingPos == null || world.getBlockState(sleepingPos).getBlock() != Blocks.BED) {
			ci.setReturnValue(0.0f);
		}
	}

}
