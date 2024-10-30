package io.github.haykam821.sculkprison.game.player;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum WinTeam {
	PLAYERS(Text.translatable("text.sculkprison.win.players").formatted(Formatting.YELLOW)),
	WARDEN(Text.translatable("text.sculkprison.win.warden").formatted(Formatting.DARK_AQUA));

	private final Text name;

	private WinTeam(Text name) {
		this.name = name;
	}
	
	public Text getWinMessage() {
		return Text.translatable("text.sculkprison.win", this.name).formatted(Formatting.GOLD);
	}
}
