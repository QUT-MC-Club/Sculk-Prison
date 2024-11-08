package io.github.haykam821.sculkprison.game.player.target;

import java.util.Comparator;

import io.github.haykam821.sculkprison.game.player.WardenData;
import net.minecraft.SharedConstants;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class SonicBoomTargetSelection {
	private static final int TICKS_UNTIL_SONIC_BOOM = SharedConstants.TICKS_PER_SECOND * 3;
	private static final int TICKS_UNTIL_SELECTION_LOCKED = TICKS_UNTIL_SONIC_BOOM - 34;

	private static final double MAX_SELECTION_RANGE = 0.15;

	private final WardenData warden;

	private SonicBoomTarget target = null;
	private int ticksUntilSelected = TICKS_UNTIL_SONIC_BOOM;

	public SonicBoomTargetSelection(WardenData warden) {
		this.warden = warden;
	}

	public void tick() {
		if (this.ticksUntilSelected > TICKS_UNTIL_SELECTION_LOCKED) {
			this.selectTarget();
		}

		if (this.target != null) {
			this.ticksUntilSelected -= 1;

			if (this.ticksUntilSelected == TICKS_UNTIL_SELECTION_LOCKED) {
				this.warden.playSound(SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 3, 1);
			} else if (this.ticksUntilSelected <= 0) {
				this.warden.createSonicBoom(this.target);
				this.setTarget(null);
			}
		}
	}

	private void selectTarget() {
		if (!this.warden.isAttemptingSonicBoom()) {
			this.setTarget(null);
			return;
		}

		Vec3d pos = this.warden.getEyePos();
		Vec3d idealTargetPos = this.warden.getIdealSonicBoomTargetPos();

		SonicBoomTarget target = this.warden.getSonicBoomTargets().stream()
			.sorted(Comparator.comparingDouble(targetx -> {
				Vec3d targetPos = targetx.getPos().subtract(pos).normalize();
				return targetPos.distanceTo(idealTargetPos);
			}))
			.findFirst()
			.filter(targetx -> {
				Vec3d targetPos = targetx.getPos().subtract(pos).normalize();
				return targetPos.distanceTo(idealTargetPos) <= MAX_SELECTION_RANGE;
			})
			.orElse(null);

		this.setTarget(target);
	}

	private void setTarget(SonicBoomTarget target) {
		if (target != this.target) {
			this.ticksUntilSelected = TICKS_UNTIL_SONIC_BOOM;

			if (this.target != null) {
				this.target.setActive(false);
			}

			if (target != null) {
				target.setActive(true);
			}
		}

		this.target = target;
	}
}
