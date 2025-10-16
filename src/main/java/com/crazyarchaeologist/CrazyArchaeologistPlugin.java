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
import java.util.HashSet;
import java.util.Set;

@Slf4j
@PluginDescriptor(
		name = "Crazy Archaeologist Helper",
		description = "Alerts you when Crazy Archaeologist uses his special attack and marks dangerous tiles",
		tags = {"boss", "pvm", "wilderness", "archaeologist", "crazy"}
)
public class CrazyArchaeologistPlugin extends Plugin {

	private static final int CRAZY_ARCHAEOLOGIST_ID = 6618;
	private static final String SPECIAL_ATTACK_TEXT = "Rain of knowledge!";
	private static final int SPECIAL_ATTACK_PROJECTILE_ID = 1260;
	private static final int DANGER_TILE_DURATION_TICKS = 6; // Adjust based on testing

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

	private final Set<WorldPoint> dangerousTiles = new HashSet<>();
	private int dangerTilesExpireTick = 0;

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

		// Only track the special attack projectiles
		if (projectile.getId() == SPECIAL_ATTACK_PROJECTILE_ID) {
			LocalPoint target = projectile.getTarget();
			LocalPoint targetLocal = new LocalPoint(target.getX(), target.getY());
			WorldPoint worldTarget = WorldPoint.fromLocal(client, targetLocal);

			if (worldTarget != null) {
				log.info("Special attack projectile landing at: {}", worldTarget);

				// Add 3x3 area as dangerous (1 tile radius from center)
				addDangerousTiles(worldTarget, 1);

				// Set expiration for dangerous tiles
				int remainingCycles = projectile.getRemainingCycles();
				int estimatedLandingTick = client.getTickCount() + (remainingCycles / 30); // 30 client cycles per game tick
				dangerTilesExpireTick = Math.max(dangerTilesExpireTick, estimatedLandingTick + DANGER_TILE_DURATION_TICKS);
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		// Clear dangerous tiles after they expire
		if (client.getTickCount() >= dangerTilesExpireTick && !dangerousTiles.isEmpty()) {
			log.info("Clearing dangerous tiles");
			dangerousTiles.clear();
		}
	}

	private void addDangerousTiles(WorldPoint center, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				WorldPoint tile = center.dx(dx).dy(dy);
				dangerousTiles.add(tile);
				log.info("Added dangerous tile: {}", tile);
			}
		}
	}

	private void handleSpecialAttack() {
		log.info("handleSpecialAttack() called!");

		if (config.useSound()) {
			try {
				client.playSoundEffect(config.soundEffect().getId());
			} catch (Exception e) {
				log.error("Error playing sound effect", e);
			}
		}

		if (config.useNotification()) {
			try {
				notifier.notify("Crazy Archaeologist: Special Attack Incoming!");
			} catch (Exception e) {
				log.error("Error sending notification", e);
			}
		}

		if (config.useGameMessage()) {
			try {
				client.addChatMessage(
						ChatMessageType.GAMEMESSAGE,
						"",
						"<col=ff0000>Crazy Archaeologist Special Attack!</col>",
						null
				);
			} catch (Exception e) {
				log.error("Error adding game message", e);
			}
		}
	}

	public Set<WorldPoint> getDangerousTiles() {
		return dangerousTiles;
	}
}