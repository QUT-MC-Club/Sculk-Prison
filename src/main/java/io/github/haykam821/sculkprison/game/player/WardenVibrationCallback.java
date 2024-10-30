package io.github.haykam821.sculkprison.game.player;

import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.GameEventTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.EntityPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.Vibrations;

public class WardenVibrationCallback implements Vibrations.Callback {
	private static final int RANGE = 16;

	private final WardenData warden;
	private final PositionSource positionSource;

	protected WardenVibrationCallback(WardenData warden, ServerPlayerEntity player) {
		this.warden = warden;
		this.positionSource = new EntityPositionSource(player, player.getStandingEyeHeight());
	}

	@Override
	public int getRange() {
		return RANGE;
	}

	@Override
	public PositionSource getPositionSource() {
		return this.positionSource;
	}

	@Override
	public TagKey<GameEvent> getTag() {
		return GameEventTags.WARDEN_CAN_LISTEN;
	}

	@Override
	public boolean accepts(ServerWorld world, BlockPos pos, GameEvent event, GameEvent.Emitter emitter) {
		return this.warden.canProduceVibration(world, pos, event, emitter);
	}

	@Override
	public void accept(ServerWorld world, BlockPos pos, GameEvent event, Entity sourceEntity, Entity entity, float distance) {
		this.warden.acceptGameEvent(world, pos, event, sourceEntity, entity, distance);
	}
}
