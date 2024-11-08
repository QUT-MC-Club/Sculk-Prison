package io.github.haykam821.sculkprison.game.player.target;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import io.github.haykam821.sculkprison.game.player.WardenColors;
import io.github.haykam821.sculkprison.game.player.WardenData;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public abstract class SonicBoomTarget {
	private final ElementHolder holder = new ElementHolder();

	private DisplayElement element = null;
	private HolderAttachment attachment = null;

	public void setActive(boolean active) {
		if (this.element != null) {
			this.element.setGlowColorOverride(active ? WardenColors.ACTIVE_TARGET : WardenColors.INACTIVE_TARGET);
		}
	}

	public void attach(WardenData warden, ServerWorld world) {
		Entity entity = this.getEntity();

		this.element = new SonicBoomTargetElement(entity);
		this.setActive(false);

		this.holder.addElement(this.element);

		this.attachment = new ManualAttachment(this.holder, world, this::getPos);
		this.holder.setAttachment(this.attachment);

		if (entity != null) {
			for (int id : this.holder.getEntityIds()) {
				VirtualEntityUtils.addVirtualPassenger(entity, id);
			}
		}

		this.attachment.startWatching(warden.getNetworkHandler());
	}

	public void tick() {
		this.holder.tick();
	}

	public void destroy() {
		this.holder.destroy();
	}

	public abstract Vec3d getPos();

	public Entity getEntity() {
		return null;
	}
}
