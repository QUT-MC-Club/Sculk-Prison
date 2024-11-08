package io.github.haykam821.sculkprison.game.player;

import org.joml.Vector3fc;

import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.util.math.MathHelper;

public class WardenColors {
	private static final int INACTIVE = 0x649683;
	private static final int ACTIVE = packRgb(DustColorTransitionParticleEffect.SCULK_BLUE);

	public static final int INACTIVE_ARMOR = INACTIVE;
	public static final int ACTIVE_ARMOR = ACTIVE;

	public static final int INACTIVE_TARGET = INACTIVE;
	public static final int ACTIVE_TARGET = ACTIVE;

	private static int packRgb(Vector3fc color) {
		return MathHelper.packRgb(color.x(), color.y(), color.z());
	}
}
