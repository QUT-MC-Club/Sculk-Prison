package io.github.haykam821.sculkprison;

import io.github.haykam821.sculkprison.game.SculkPrisonConfig;
import io.github.haykam821.sculkprison.game.phase.SculkPrisonWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.tag.TagFactory;
import net.minecraft.block.Block;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;

public class Main implements ModInitializer {
	public static final String MOD_ID = "sculkprison";

	private static final Identifier SCULK_PRISON_ID = new Identifier(MOD_ID, "sculk_prison");
	public static final GameType<SculkPrisonConfig> SCULK_PRISON_TYPE = GameType.register(SCULK_PRISON_ID, SculkPrisonConfig.CODEC, SculkPrisonWaitingPhase::open);

	private static final Identifier WARDEN_CAGE_BREAK_BLOCKS_ID = new Identifier(MOD_ID, "warden_cage_break_blocks");
	public static final Tag<Block> WARDEN_CAGE_BREAK_BLOCKS = TagFactory.BLOCK.create(WARDEN_CAGE_BREAK_BLOCKS_ID);

	@Override
	public void onInitialize() {
		return;
	}
}
