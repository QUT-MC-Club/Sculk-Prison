package io.github.haykam821.sculkprison.game.player;

import org.joml.Vector3fc;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public class WardenInventoryManager {
	private static final int ARMOR_COLOR = 0x649683;
	private static final int ACTIVE_ARMOR_COLOR = packRgb(DustColorTransitionParticleEffect.SCULK_BLUE);

	public static final ItemStack HELMET = WardenInventoryManager.createArmorStack(Items.LEATHER_HELMET, false);
	public static final ItemStack CHESTPLATE = WardenInventoryManager.createArmorStack(Items.LEATHER_CHESTPLATE, false);
	public static final ItemStack LEGGINGS = WardenInventoryManager.createArmorStack(Items.LEATHER_LEGGINGS, false);
	public static final ItemStack BOOTS = WardenInventoryManager.createArmorStack(Items.LEATHER_BOOTS, false);

	public static final ItemStack ACTIVE_HELMET = WardenInventoryManager.createArmorStack(Items.LEATHER_HELMET, true);

	private static void updateInventory(ServerPlayerEntity player) {
		player.currentScreenHandler.sendContentUpdates();
		player.playerScreenHandler.onContentChanged(player.getInventory());
	}

	public static void applyTo(ServerPlayerEntity player) {
		PlayerInventory inventory = player.getInventory();

		inventory.armor.set(3, HELMET.copy());
		inventory.armor.set(2, CHESTPLATE.copy());
		inventory.armor.set(1, LEGGINGS.copy());
		inventory.armor.set(0, BOOTS.copy());

		WardenInventoryManager.updateInventory(player);
	}

	public static void applyHelmet(ServerPlayerEntity player) {
		player.getInventory().armor.set(3, HELMET.copy());
		WardenInventoryManager.updateInventory(player);
	}

	public static void applyActiveHelmet(ServerPlayerEntity player) {
		player.getInventory().armor.set(3, ACTIVE_HELMET.copy());
		WardenInventoryManager.updateInventory(player);
	}

	private static ItemStack createArmorStack(ItemConvertible item, boolean active) {
		return ItemStackBuilder.of(item)
			.addEnchantment(Enchantments.BINDING_CURSE, 1)
			.setDyeColor(active ? ACTIVE_ARMOR_COLOR : ARMOR_COLOR)
			.setUnbreakable()
			.build();
	}

	private static int packRgb(Vector3fc color) {
		return MathHelper.packRgb(color.x(), color.y(), color.z());
	}
}