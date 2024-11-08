package io.github.haykam821.sculkprison.game.player.target;

import java.util.function.Consumer;

import org.joml.Vector3f;

import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity.BillboardMode;
import net.minecraft.item.Items;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

public class SonicBoomTargetElement extends ItemDisplayElement {
	private static final Vector3f SCALE = new Vector3f(0.6f, 0.6f, 0.0001f);

	private final Entity vehicleEntity;

	public SonicBoomTargetElement(Entity vehicleEntity) {
		super(Items.SCULK);

		this.setGlowing(true);
		this.setInvisible(true);

		this.setScale(SCALE);
		this.setBillboardMode(BillboardMode.CENTER);

		this.setSendPositionUpdates(false);

		this.vehicleEntity = vehicleEntity;
	}

	@Override
	public void startWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
		super.startWatching(player, packetConsumer);

		if (this.vehicleEntity != null) {
			packetConsumer.accept(new EntityPassengersSetS2CPacket(this.vehicleEntity));
		}
	}
}
