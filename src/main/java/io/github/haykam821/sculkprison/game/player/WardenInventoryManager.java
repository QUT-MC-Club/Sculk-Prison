package io.github.haykam821.sculkprison.game.player;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public class WardenInventoryManager {
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
			.setDyeColor(active ? WardenColors.ACTIVE_ARMOR : WardenColors.INACTIVE_ARMOR)
			.setUnbreakable()
			.build();
	}
}