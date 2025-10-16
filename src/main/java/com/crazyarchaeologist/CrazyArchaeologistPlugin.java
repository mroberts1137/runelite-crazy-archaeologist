package com.crazyarchaeologist;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@PluginDescriptor(
		name = "Crazy Archaeologist Helper",
		description = "Alerts you when Crazy Archaeologist uses his special attack",
		tags = {"boss", "pvm", "wilderness", "archaeologist", "crazy"}
)
public class CrazyArchaeologistPlugin extends Plugin {

	private static final int CRAZY_ARCHAEOLOGIST_ID = 6618;
	private static final String SPECIAL_ATTACK_TEXT = "Rain of knowledge!";
	private static final int SPECIAL_ATTACK_PROJECTILE_ID = 1260;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private Notifier notifier;

	@Inject
	private CrazyArchaeologistConfig config;

	@Inject
	private KeyManager keyManager;

	@Inject
	private OverlayManager overlayManager;

	private CrazyArchaeologistOverlay overlay;

	// Track dangerous tiles and when to clear them
	private final Set<WorldPoint> dangerousTiles = new HashSet<>();
	private int clearTilesAtTick = -1;

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.testHotkey()) {
		@Override
		public void hotkeyPressed() {
			log.info("Manual trigger activated!");
			clientThread.invoke(() -> handleSpecialAttack());
		}
	};

	@Provides
	CrazyArchaeologistConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(CrazyArchaeologistConfig.class);
	}

	@Override
	protected void startUp() throws Exception {
		log.info("Crazy Archaeologist Helper started!");
		keyManager.registerKeyListener(hotkeyListener);
		overlay = new CrazyArchaeologistOverlay(this, config, client);
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Crazy Archaeologist Helper stopped!");
		keyManager.unregisterKeyListener(hotkeyListener);
		overlayManager.remove(overlay);
		dangerousTiles.clear();
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event) {
		if (!(event.getActor() instanceof NPC)) {
			return;
		}

		NPC npc = (NPC) event.getActor();
		String overheadText = event.getOverheadText();

		if (npc.getId() == CRAZY_ARCHAEOLOGIST_ID) {
			if (overheadText != null && overheadText.contains(SPECIAL_ATTACK_TEXT)) {
				log.info("Special attack detected!");
				handleSpecialAttack();
			}
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event) {
		Projectile projectile = event.getProjectile();

		if (projectile.getId() == SPECIAL_ATTACK_PROJECTILE_ID) {
			LocalPoint targetLocal = projectile.getTarget();
			if (targetLocal != null) {
				WorldPoint worldTarget = WorldPoint.fromLocal(client, targetLocal);
				if (worldTarget != null) {
					log.info("Special attack projectile landing at: {}", worldTarget);

					// Add 3x3 area as dangerous
					addDangerousTiles(worldTarget, 1);

					// Set when to clear tiles (after projectile lands + explosion time)
					// Projectile has 79 cycles, convert to ticks (1 cycle = 1 tick typically)
					clearTilesAtTick = client.getTickCount() + projectile.getRemainingCycles() / 30;
				}
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		// Clear dangerous tiles after they've exploded
		if (clearTilesAtTick > 0 && client.getTickCount() >= clearTilesAtTick) {
			dangerousTiles.clear();
			clearTilesAtTick = -1;
			log.info("Cleared dangerous tiles");
		}
	}

	private void addDangerousTiles(WorldPoint center, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				dangerousTiles.add(center.dx(dx).dy(dy));
			}
		}
		log.info("Added {} dangerous tiles around {}", (radius * 2 + 1) * (radius * 2 + 1), center);
	}

	private void handleSpecialAttack() {
		if (config.useSound()) {
			client.playSoundEffect(config.soundEffect().getId());
		}

		if (config.useNotification()) {
			notifier.notify("Crazy Archaeologist: Special Attack Incoming!");
		}

		if (config.useGameMessage()) {
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					"<col=ff0000>Crazy Archaeologist Special Attack!</col>",
					null
			);
		}
	}

	public Set<WorldPoint> getDangerousTiles() {
		return dangerousTiles;
	}
}