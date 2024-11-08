package io.github.haykam821.sculkprison.game.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import io.github.haykam821.sculkprison.game.phase.SculkPrisonActivePhase;
import io.github.haykam821.sculkprison.game.player.target.SonicBoomTarget;
import io.github.haykam821.sculkprison.game.player.target.SonicBoomTargetSelection;
import io.github.haykam821.sculkprison.game.player.target.StaticSonicBoomTarget;
import io.github.haykam821.sculkprison.game.player.target.TrackingSonicBoomTarget;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Angriness;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.GameEvent.Emitter;
import net.minecraft.world.event.Vibrations;
import net.minecraft.world.event.listener.EntityGameEventHandler;

public class WardenData implements Vibrations {
	private static final int ANGRINESS_INCREASE = 35;
	private static final int MINOR_ANGRINESS_INCREASE = 10;

	private static final int MAX_ANGER = 150;

	private static final int ANGER_DECREMENT_INTERVAL = SharedConstants.TICKS_PER_SECOND;

	private static final int MAX_VIBRATION_COOLDOWN = SharedConstants.TICKS_PER_SECOND * 2;

	private static final int SONIC_BOOM_DAMAGE_RADIUS = 1;
	private static final int SONIC_BOOM_DAMAGE_RADIUS_SQUARED = SONIC_BOOM_DAMAGE_RADIUS * SONIC_BOOM_DAMAGE_RADIUS;

	private final SculkPrisonActivePhase phase;
	private final ServerPlayerEntity player;

	private final Vibrations.ListenerData vibrationListenerData = new Vibrations.ListenerData();
	private final Vibrations.Callback vibrationCallback;
	private final EntityGameEventHandler<Vibrations.VibrationListener> gameEventHandler = new EntityGameEventHandler<>(new Vibrations.VibrationListener(this));

	private final Object2IntMap<Entity> suspectsToAngerLevel = new Object2IntOpenHashMap<>();

	private final List<SonicBoomTarget> sonicBoomTargets = new ArrayList<>();
	private final SonicBoomTargetSelection sonicBoomTargetSelection = new SonicBoomTargetSelection(this);

	private int maximumAnger;
	private boolean angerDirty;

	private int vibrationCooldown;

	private int ambientSoundChance;

	private WardenData(SculkPrisonActivePhase phase, ServerPlayerEntity player) {
		this.phase = Objects.requireNonNull(phase);
		this.player = Objects.requireNonNull(player);

		this.vibrationCallback = new WardenVibrationCallback(this, this.player);
	}

	public void initialize() {
		WardenInventoryManager.applyTo(player);
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, StatusEffectInstance.INFINITE, 1, true, false));

		this.player.updateEventHandler(EntityGameEventHandler::onEntitySetPosCallback);
}

	public void tick() {
		if (this.player.getRandom().nextInt(1000) < this.ambientSoundChance) {
			this.ambientSoundChance = -80;
			this.playSound(SoundEvents.ENTITY_WARDEN_AMBIENT, 4);
		} else {
			this.ambientSoundChance += 1;
		}

		if (this.player.age % this.getHeartRate() == 0) {
			this.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, 5);
		}

		if (this.player.age % ANGER_DECREMENT_INTERVAL == 0) {
			for (Entity entity : this.suspectsToAngerLevel.keySet()) {
				this.setAnger(entity, this.suspectsToAngerLevel.getInt(entity) - 1, false);
			}
		}

		if (this.vibrationCooldown > 0) {
			this.setVibrationCooldown(this.vibrationCooldown - 1);
		}

		Vibrations.Ticker.tick(this.player.getWorld(), this.vibrationListenerData, this.vibrationCallback);

		for (SonicBoomTarget target : this.sonicBoomTargets) {
			target.tick();
		}

		this.sonicBoomTargetSelection.tick();
	}

	public void createSonicBoom(SonicBoomTarget target) {
		ServerWorld world = this.player.getServerWorld();

		// Determine a line from the warden to the target
		Vec3d startPos = this.getSonicBoomStartPos();
		Vec3d endPos = target.getPos();

		Vec3d offset = endPos.subtract(startPos);
		Vec3d normalizedOffset = offset.normalize();

		// Create particles along the line
		for (int index = 1; index < MathHelper.floor(offset.length()) + 7; index += 1) {
			Vec3d stepPos = startPos.add(normalizedOffset.multiply(index));
			world.spawnParticles(ParticleTypes.SONIC_BOOM, stepPos.getX(), stepPos.getY(), stepPos.getZ(), 1, 0, 0, 0, 0);
		}

		// Play the sonic boom sound
		this.playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 3, 1);

		// Apply entity damage and knockback effects
		Iterator<ServerPlayerEntity> iterator = this.phase.getPlayers().iterator();

		while (iterator.hasNext()) {
			ServerPlayerEntity player = iterator.next();

			if (player == this.player) continue;
			if (player.getPos().squaredDistanceTo(endPos) > SONIC_BOOM_DAMAGE_RADIUS_SQUARED) continue;

			this.phase.eliminate(player, Text.translatable("text.sculkprison.eliminated.sonic_boom", player.getDisplayName(), this.player.getDisplayName()), false);
			iterator.remove();

			double knockbackResistance = player.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);

			double horizontalKnockbackScale = 2.5 * (1.0 - knockbackResistance);
			double verticalKnockbackScale = 0.5 * (1.0 - knockbackResistance);

			double velocityX = normalizedOffset.getX() * horizontalKnockbackScale;
			double velocityY = normalizedOffset.getY() * verticalKnockbackScale;
			double velocityZ = normalizedOffset.getZ() * horizontalKnockbackScale;

			player.addVelocity(velocityX, velocityY, velocityZ);
		}
	}

	public boolean isAttemptingSonicBoom() {
		return this.player.isUsingItem() || this.player.isSneaking();
	}

	private Vec3d getSonicBoomStartPos() {
		return this.player.getPos().add(0, 1.6, 0);
	}

	public Collection<SonicBoomTarget> getSonicBoomTargets() {
		return this.sonicBoomTargets;
	}

	private void addSonicBoomTarget(SonicBoomTarget target) {
		if (!this.sonicBoomTargets.contains(target)) {
			this.sonicBoomTargets.add(target);
			target.attach(this, this.player.getServerWorld());
		}
	}

	public void removeSonicBoomTargetsFor(Entity entity) {
		Iterator<SonicBoomTarget> iterator = this.sonicBoomTargets.iterator();

		while (iterator.hasNext()) {
			SonicBoomTarget target = iterator.next();

			if (entity == target.getEntity()) {
				target.destroy();
				iterator.remove();
			}
		}
	}

	private void addSonicBoomTargetForVibration(Entity entity) {
		if (entity != null) {
			int anger = this.suspectsToAngerLevel.getInt(entity);

			if (anger >= MAX_ANGER) {
				this.addSonicBoomTarget(new TrackingSonicBoomTarget(entity));
			} else {
				this.addSonicBoomTarget(new StaticSonicBoomTarget(entity.getPos()));
			}
		}
	}

	public ServerPlayNetworkHandler getNetworkHandler() {
		return this.player.networkHandler;
	}

	public Vec3d getEyePos() {
		return this.player.getEyePos();
	}

	public Vec3d getIdealSonicBoomTargetPos() {
		return Vec3d.fromPolar(this.player.getPitch(), this.player.getYaw());
	}

	public boolean isOf(Entity player) {
		return this.player.equals(player);
	}

	public boolean isIn(Collection<ServerPlayerEntity> players) {
		return players.contains(this.player);
	}

	private void playSound(SoundEvent sound, float volume) {
		this.playSound(sound, volume, this.player.getSoundPitch());
	}

	public void playSound(SoundEvent sound, float volume, float pitch) {
		this.player.getWorld().playSoundFromEntity(null, this.player, sound, SoundCategory.PLAYERS, volume, pitch);
	}

	private int getHeartRate() {
		float angriness = this.getAnger() / (float) Angriness.ANGRY.getThreshold();
		return 40 - MathHelper.floor(MathHelper.clamp(angriness, 0, 1) * 30);
	}

	private int getAnger() {
		if (this.angerDirty) {
			this.maximumAnger = this.suspectsToAngerLevel.object2IntEntrySet().stream()
				.map(Object2IntMap.Entry::getIntValue)
				.max(Comparator.comparingInt(value -> value))
				.orElse(0);

			this.angerDirty = false;
		}

		return this.maximumAnger;
	}

	private void addAnger(Entity entity, int amount, boolean updateMaximumAnger) {
		this.setAnger(entity, this.suspectsToAngerLevel.getInt(entity) + amount, updateMaximumAnger);
	}

	private void setAnger(Entity entity, int amount, boolean updateMaximumAnger) {
		if (entity != null) {
			int clampedAmount = MathHelper.clamp(amount, 0, MAX_ANGER);

			if (clampedAmount != 0) {
				int oldAmount = this.suspectsToAngerLevel.put(entity, clampedAmount);

				if (clampedAmount != oldAmount) {
					this.angerDirty = true;
				}
			} else if (this.suspectsToAngerLevel.containsKey(entity)) {
				this.suspectsToAngerLevel.removeInt(entity);
				this.angerDirty = true;
			}
		}
	}

	public void setVibrationCooldown(int vibrationCooldown) {
		int clampedCooldown = Math.max(0, vibrationCooldown);

		if (clampedCooldown == 0 && this.vibrationCooldown > 0) {
			WardenInventoryManager.applyHelmet(player);
		} else if (clampedCooldown > 0 && this.vibrationCooldown == 0) {
			WardenInventoryManager.applyActiveHelmet(player);
		}

		this.vibrationCooldown = clampedCooldown;
	}

	@Override
	public Vibrations.ListenerData getVibrationListenerData() {
		return this.vibrationListenerData;
	}

	@Override
	public Vibrations.Callback getVibrationCallback() {
		return this.vibrationCallback;
	}

	public void updateEventHandler(BiConsumer<EntityGameEventHandler<?>, ServerWorld> callback) {
		callback.accept(this.gameEventHandler, this.player.getServerWorld());
	}

	public boolean canProduceVibration(ServerWorld world, BlockPos pos, GameEvent event, Emitter emitter) {
		if (this.vibrationCooldown > 0) return false;
		if (this.phase.getLockTime() > 0) return false;

		if (this.isOf(emitter.sourceEntity())) return false;
		if (!(emitter.sourceEntity() instanceof ServerPlayerEntity)) return false;

		return true;
	}

	protected void acceptGameEvent(ServerWorld world, BlockPos pos, GameEvent event, Entity sourceEntity, Entity entity, float distance) {
		this.setVibrationCooldown(MAX_VIBRATION_COOLDOWN);
		this.playSound(SoundEvents.ENTITY_WARDEN_TENDRIL_CLICKS, 5);

		if (entity instanceof ServerPlayerEntity) {
			int angerIncrease = this.player.isInRange(entity, 30) ? ANGRINESS_INCREASE : MINOR_ANGRINESS_INCREASE;
			this.addAnger(entity, angerIncrease, true);
			this.addSonicBoomTargetForVibration(entity);
		} else {
			this.addAnger(sourceEntity, ANGRINESS_INCREASE, true);
			this.addSonicBoomTargetForVibration(sourceEntity);
		}
	}

	public static WardenData choose(SculkPrisonActivePhase phase, List<ServerPlayerEntity> players, Random random) {
		ServerPlayerEntity player = Util.getRandom(players, random);
		return new WardenData(phase, player);
	}
}
