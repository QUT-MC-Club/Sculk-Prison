package io.github.haykam821.sculkprison.mixin;

import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.haykam821.sculkprison.game.event.WardenDataListener;
import io.github.haykam821.sculkprison.game.player.WardenData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import net.minecraft.world.event.Vibrations;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import xyz.nucleoid.stimuli.EventInvokers;
import xyz.nucleoid.stimuli.Stimuli;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements Vibrations {
	private PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
	private void modifyWardenDeathSound(CallbackInfoReturnable<SoundEvent> ci) {
		if ((Object) this instanceof ServerPlayerEntity player) {
			try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
				WardenData warden = invokers.get(WardenDataListener.EVENT).getWardenData(player);

				if (warden != null) {
					ci.setReturnValue(SoundEvents.ENTITY_WARDEN_DEATH);
				}
			}
		}
	}

	@Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
	private void modifyWardenHurtSound(CallbackInfoReturnable<SoundEvent> ci) {
		if ((Object) this instanceof ServerPlayerEntity player) {
			try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
				WardenData warden = invokers.get(WardenDataListener.EVENT).getWardenData(player);

				if (warden != null) {
					ci.setReturnValue(SoundEvents.ENTITY_WARDEN_HURT);
				}
			}
		}
	}

	@Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
	private void modifyWardenStepSound(CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayerEntity player) {
			try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
				WardenData warden = invokers.get(WardenDataListener.EVENT).getWardenData(player);

				if (warden != null) {
					this.playSound(SoundEvents.ENTITY_WARDEN_STEP, 10, 1);
					ci.cancel();
				}
			}
		}
	}

	@Override
	protected float calculateNextStepSoundDistance() {
		if ((Object) this instanceof ServerPlayerEntity player) {
			try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
				WardenData warden = invokers.get(WardenDataListener.EVENT).getWardenData(player);

				if (warden != null) {
					return this.distanceTraveled + 0.55f;
				}
			}
		}

		return super.calculateNextStepSoundDistance();
	}

	@Override
	public Vibrations.ListenerData getVibrationListenerData() {
		if ((Object) this instanceof ServerPlayerEntity player) {
			try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
				WardenData warden = invokers.get(WardenDataListener.EVENT).getWardenData(player);

				if (warden != null) {
					return warden.getVibrationListenerData();
				}
			}
		}

		return null;
	}

	@Override
	public Vibrations.Callback getVibrationCallback() {
		if ((Object) this instanceof ServerPlayerEntity player) {
			try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
				WardenData warden = invokers.get(WardenDataListener.EVENT).getWardenData(player);

				if (warden != null) {
					return warden.getVibrationCallback();
				}
			}
		}

		return null;
	}

	@Override
	public void updateEventHandler(BiConsumer<EntityGameEventHandler<?>, ServerWorld> callback) {
		if ((Object) this instanceof ServerPlayerEntity player) {
			try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
				WardenData warden = invokers.get(WardenDataListener.EVENT).getWardenData(player);

				if (warden != null) {
					warden.updateEventHandler(callback);
				}
			}
		}
	}
}
