package io.github.haykam821.sculkprison.game.event;

import io.github.haykam821.sculkprison.game.player.WardenData;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface WardenDataListener {
	public StimulusEvent<WardenDataListener> EVENT = StimulusEvent.create(WardenDataListener.class, context -> {
		return entity -> {
			try {
				for (WardenDataListener listener : context.getListeners()) {
					WardenData warden = listener.getWardenData(entity);

					if (warden != null) {
						return warden;
					}
				}
			} catch (Throwable throwable) {
				context.handleException(throwable);
			}

			return null;
		};
	});

	public WardenData getWardenData(ServerPlayerEntity entity);
}
