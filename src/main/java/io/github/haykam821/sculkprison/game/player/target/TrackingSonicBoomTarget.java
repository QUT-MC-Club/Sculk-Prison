package io.github.haykam821.sculkprison.game.player.target;

import java.util.Objects;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class TrackingSonicBoomTarget extends SonicBoomTarget {
	private final Entity entity;

	public TrackingSonicBoomTarget(Entity entity) {
		this.entity = Objects.requireNonNull(entity);
	}

	@Override
	public Vec3d getPos() {
		return this.entity.getEyePos();
	}

	@Override
	public Entity getEntity() {
		return this.entity;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof TrackingSonicBoomTarget target)) return false;

		return this.entity.equals(target.entity);
	}
}
