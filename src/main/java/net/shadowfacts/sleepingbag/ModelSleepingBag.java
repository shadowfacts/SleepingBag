package net.shadowfacts.sleepingbag;

import net.minecraft.client.render.entity.EntityModelBiped;
import net.minecraft.client.render.entity.EntityModelBox;
import net.minecraft.entity.Entity;

/**
 * @author shadowfacts
 */
public class ModelSleepingBag extends EntityModelBiped {

	public static final ModelSleepingBag instance = new ModelSleepingBag();

	private EntityModelBox main;
	private EntityModelBox pillow;

	private ModelSleepingBag() {
		textureWidth = 128;
		textureHeight = 64;

		main = new EntityModelBox(this, 0, 0);
		main.add("main", -9F, 0F, -3F, 18, 26, 7);
		main.setRotationPoint(0F, 0F, 0F);
		main.setTextureSize(128, 64);
		main.mirror = true;
		setRotation(main, 0F, 0F, 0F);
		pillow = new EntityModelBox(this, 50, 0);
		pillow.add("pillow", -8F, -9F, 0F, 8, 18, 1);
		pillow.setRotationPoint(0F, 0F, 3F);
		pillow.setTextureSize(128, 64);
		pillow.mirror = true;
		setRotation(pillow, 0F, 0F, 1.570796F);
	}

	private static void setRotation(EntityModelBox model, float x, float y, float z) {
		model.pitch = x;
		model.yaw = y;
		model.roll = z;
	}

	@Override
	public void render(Entity entity, float swingTime, float swingAmpl, float rightArmAngle, float headAngleX, float headAngleY, float scale) {
		main.render(scale);
		pillow.render(scale);
	}

}
