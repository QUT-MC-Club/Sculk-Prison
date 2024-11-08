package io.github.haykam821.sculkprison.game.player.target;

import java.util.Objects;

import net.minecraft.util.math.Vec3d;

public class StaticSonicBoomTarget extends SonicBoomTarget {
	private final Vec3d pos;

	public StaticSonicBoomTarget(Vec3d pos) {
		this.pos = Objects.requireNonNull(pos);
	}

	@Override
	public Vec3d getPos() {
		return this.pos;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof StaticSonicBoomTarget target)) return false;

		return this.pos.equals(target.pos);
	}
}
