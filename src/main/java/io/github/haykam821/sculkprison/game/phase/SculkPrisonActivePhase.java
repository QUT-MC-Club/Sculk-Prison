package io.github.haykam821.sculkprison.game.phase;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import io.github.haykam821.sculkprison.Main;
import io.github.haykam821.sculkprison.game.SculkPrisonBar;
import io.github.haykam821.sculkprison.game.SculkPrisonConfig;
import io.github.haykam821.sculkprison.game.WardenInventoryManager;
import io.github.haykam821.sculkprison.game.WinTeam;
import io.github.haykam821.sculkprison.game.map.SculkPrisonMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class SculkPrisonActivePhase implements PlayerAttackEntityEvent, GameActivityEvents.Enable, GameActivityEvents.Tick, GamePlayerEvents.Offer, PlayerDeathEvent, GamePlayerEvents.Remove {
	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final SculkPrisonMap map;
	private final SculkPrisonConfig config;
	private final SculkPrisonBar bar;

	private final List<ServerPlayerEntity> players;
	private final ServerPlayerEntity warden;
	private final boolean singleplayer;

	private int lockTime;
	private int surviveTime;

	public SculkPrisonActivePhase(GameSpace gameSpace, ServerWorld world, SculkPrisonMap map, SculkPrisonConfig config, List<ServerPlayerEntity> players, GlobalWidgets widgets) {
		this.world = world;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
		this.bar = new SculkPrisonBar(widgets);

		this.players = players;
		this.warden = this.players.get(this.world.getRandom().nextInt(this.players.size()));
		this.singleplayer = this.players.size() == 1;

		this.lockTime = this.config.getLockTime();
		this.surviveTime = this.config.getSurviveTime();
	}

	public static void setRules(GameActivity activity, boolean pvp) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.BREAK_BLOCKS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.INTERACTION);
		activity.deny(GameRuleType.PLACE_BLOCKS);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.THROW_ITEMS);

		if (pvp) {
			activity.allow(GameRuleType.PVP);
		} else {
			activity.deny(GameRuleType.PVP);
		}
	}

	public static void open(GameSpace gameSpace, ServerWorld world, SculkPrisonMap map, SculkPrisonConfig config) {
		gameSpace.setActivity(activity -> {
			GlobalWidgets widgets = GlobalWidgets.addTo(activity);

			SculkPrisonActivePhase phase = new SculkPrisonActivePhase(gameSpace, world, map, config, Lists.newArrayList(gameSpace.getPlayers()), widgets);
			SculkPrisonActivePhase.setRules(activity, true);

			// Listeners
			activity.listen(PlayerAttackEntityEvent.EVENT, phase);
			activity.listen(GameActivityEvents.ENABLE, phase);
			activity.listen(GameActivityEvents.TICK, phase);
			activity.listen(GamePlayerEvents.OFFER, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.REMOVE, phase);
		});
	}

	// Listeners
	@Override
	public ActionResult onAttackEntity(ServerPlayerEntity attacker, Hand hand, Entity attacked, EntityHitResult hitResult) {
		if (attacker.equals(this.warden) && attacked instanceof ServerPlayerEntity) {
			this.eliminate((ServerPlayerEntity) attacked, Text.translatable("text.sculkprison.eliminated.warden", attacked.getDisplayName(), attacker.getDisplayName()), true);
		}
		return ActionResult.FAIL;
	}

	@Override
	public void onEnable() {
		for (ServerPlayerEntity player : this.players) {
			player.changeGameMode(GameMode.ADVENTURE);

			if (player.equals(this.warden)) {
				WardenInventoryManager.applyTo(player);
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, StatusEffectInstance.INFINITE, 1, true, false));
			}
			SculkPrisonActivePhase.spawn(this.world, this.map, player, player.equals(this.warden));
		}
	}

	@Override
	public void onTick() {
		this.lockTime -= 1;
		if (this.lockTime < 0) {
			this.surviveTime -= 1;
		} else if (this.lockTime == 0) {
			this.unlockCage();
			this.bar.changeToSurvive();
		}

		this.bar.tick(this);

		Iterator<ServerPlayerEntity> iterator = this.players.iterator();
		while (iterator.hasNext()) {
			ServerPlayerEntity player = iterator.next();
			if (player.getY() < 64) {
				this.eliminate(player, ".out_of_bounds", false);
				iterator.remove();
			}
		}

		if (!this.singleplayer) this.checkWinners();
		if (this.surviveTime < 0) this.endWithWinner(WinTeam.PLAYERS);
		if (this.players.isEmpty()) this.endWithNoWinners();
	}

	@Override
	public PlayerOfferResult onOfferPlayer(PlayerOffer offer) {
		return offer.accept(this.world, SculkPrisonMap.WARDEN_SPAWN).and(() -> {
			this.setSpectator(offer.player());
		});
	}

	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		SculkPrisonActivePhase.spawn(this.world, this.map, player, player.equals(this.warden));
		return ActionResult.FAIL;
	}

	@Override
	public void onRemovePlayer(ServerPlayerEntity player) {
		this.eliminate(player, true);
		this.players.remove(player);
	}

	// Utilities
	/**
	 * Breaks every block in the {@code sculkprison:warden_cage_break_blocks} block tag within the bounds defined by {@link SculkPrisonMap#WARDEN_CAGE}.
	 */
	private void unlockCage() {
		for (BlockPos pos : SculkPrisonMap.WARDEN_CAGE) {
			if (this.world.getBlockState(pos).isIn(Main.WARDEN_CAGE_BREAK_BLOCKS)) {
				this.world.breakBlock(pos, false);
			}
		}
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.changeGameMode(GameMode.SPECTATOR);
	}

	/**
	 * Eliminates a given player and prints a custom message to the chat.
	 * @param remove whether to remove the player from {@link SculkPrisonActivePhase#players}
	 */
	private void eliminate(ServerPlayerEntity player, Text message, boolean remove) {
		this.gameSpace.getPlayers().sendMessage(message);

		if (remove) {
			this.players.remove(player);
		}
		this.setSpectator(player);
	}


	private void eliminate(ServerPlayerEntity player, String suffix, boolean remove) {
		this.eliminate(player, Text.translatable("text.sculkprison.eliminated" + suffix, player.getDisplayName()).formatted(Formatting.RED), remove);
	}

	/**
	 * Eliminates a given player and prints a default message to the chat.
	 */
	private void eliminate(ServerPlayerEntity player, boolean remove) {
		this.eliminate(player, "", remove);
	}

	/**
	 * Ends the game, printing the win message of a specific team.
	 */
	private void endWithWinner(WinTeam team) {
		this.gameSpace.getPlayers().sendMessage(team.getWinMessage());
		this.gameSpace.close(GameCloseReason.FINISHED);
	}

	private void endWithNoWinners() {
		this.gameSpace.getPlayers().sendMessage(Text.translatable("text.sculkprison.no_winners").formatted(Formatting.RED));
		this.gameSpace.close(GameCloseReason.FINISHED);
	}

	/**
	 * Checks each team's win conditions, ending the game if any are met.
	 * {@linkplain SculkPrisonActivePhase#surviveTime Survive time} is not checked using this method, and singleplayer never checks win conditions.
	 */
	private void checkWinners() {
		if (!this.players.contains(this.warden)) {
			this.endWithWinner(WinTeam.PLAYERS);
		} else if (this.players.size() == 1) {
			this.endWithWinner(WinTeam.WARDEN);
		}
	}

	public float getBarProgress() {
		if (this.lockTime < 0) {
			return (this.config.getSurviveTime() - this.surviveTime) / (float) this.config.getSurviveTime();
		}
		return (this.config.getLockTime() - this.lockTime) / (float) this.config.getLockTime();
	}

	public int getLockTime() {
		return this.lockTime;
	}

	/**
	 * Spawns a given player within the map.
	 * @param warden whether to use the {@linkplain SculkPrisonMap#WARDEN_SPAWN warden spawn} instead of the {@linkplain SculkPrisonMap#SPAWN default spawn}
	 */
	public static void spawn(ServerWorld world, SculkPrisonMap map, ServerPlayerEntity player, boolean warden) {
		Vec3d pos = warden ? SculkPrisonMap.WARDEN_SPAWN : SculkPrisonMap.SPAWN;
		player.teleport(world, pos.getX(), pos.getY(), pos.getZ(), 0, 0);
	}
}
