package io.github.haykam821.sculkprison.game.player;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import io.github.haykam821.sculkprison.game.phase.SculkPrisonActivePhase;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Angriness;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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

	private final SculkPrisonActivePhase phase;
	private final ServerPlayerEntity player;

	private final Vibrations.ListenerData vibrationListenerData = new Vibrations.ListenerData();
	private final Vibrations.Callback vibrationCallback;
	private final EntityGameEventHandler<Vibrations.VibrationListener> gameEventHandler = new EntityGameEventHandler<>(new Vibrations.VibrationListener(this));

	private final Object2IntMap<Entity> suspectsToAngerLevel = new Object2IntOpenHashMap<>();

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
	}

	public boolean isOf(Entity player) {
		return this.player.equals(player);
	}

	public boolean isIn(Collection<ServerPlayerEntity> players) {
		return players.contains(this.player);
	}

	private void playSound(SoundEvent sound, float volume) {
		this.player.getWorld().playSoundFromEntity(null, this.player, sound, SoundCategory.PLAYERS, volume, this.player.getSoundPitch());
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
		} else {
			this.addAnger(sourceEntity, ANGRINESS_INCREASE, true);
		}
	}

	public static WardenData choose(SculkPrisonActivePhase phase, List<ServerPlayerEntity> players, Random random) {
		ServerPlayerEntity player = Util.getRandom(players, random);
		return new WardenData(phase, player);
	}
}
