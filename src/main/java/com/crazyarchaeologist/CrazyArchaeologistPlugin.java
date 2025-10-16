package com.crazyarchaeologist;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
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

	// Track projectiles and their data
	private final Map<Projectile, ProjectileData> activeProjectiles = new HashMap<>();
	private final Set<WorldPoint> dangerousTiles = new HashSet<>();
	private boolean specialAttackActive = false;

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
		overlay = new CrazyArchaeologistOverlay(client, this, config);
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Crazy Archaeologist Helper stopped!");
		keyManager.unregisterKeyListener(hotkeyListener);
		overlayManager.remove(overlay);
		activeProjectiles.clear();
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
			log.info("Crazy Archaeologist said: '{}'", overheadText);

			if (overheadText != null && overheadText.contains(SPECIAL_ATTACK_TEXT)) {
				log.info("Special attack detected!");
				specialAttackActive = true;
				handleSpecialAttack();

				// Clear dangerous tiles after a delay (special attack duration)
				clientThread.invokeLater(() -> {
					specialAttackActive = false;
					dangerousTiles.clear();
				}); // Adjust this delay based on testing
			}
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event) {
		Projectile projectile = event.getProjectile();

		// Log all projectiles for debugging
		if (config.logProjectiles()) {
			log.info("Projectile detected - ID: {}, Start: {}, Target: {}, Remaining cycles: {}, Height: {}, Start Height: {}, End Height: {}",
					projectile.getId(),
					projectile.getX1() + "," + projectile.getY1(),
					projectile.getTarget(),
					projectile.getRemainingCycles(),
					projectile.getHeight(),
					projectile.getStartHeight(),
					projectile.getEndHeight()
			);
		}

		// Track projectiles if special attack is active or if they match known IDs
		if (specialAttackActive || isArchaeologistProjectile(projectile.getId())) {
			LocalPoint targetPoint = new LocalPoint(projectile.getTarget().getX(), projectile.getTarget().getY());
			WorldPoint worldTarget = WorldPoint.fromLocal(client, targetPoint);

			if (worldTarget != null) {
				ProjectileData data = new ProjectileData(
						projectile,
						worldTarget,
						client.getTickCount(),
						projectile.getRemainingCycles()
				);

				activeProjectiles.put(projectile, data);

				// Add 3x3 area as dangerous
				addDangerousTiles(worldTarget, 1); // 3x3 area (radius 1 from center)

				log.info("Tracking projectile {} landing at {}", projectile.getId(), worldTarget);
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		// Update projectile tracking and remove expired ones
		activeProjectiles.entrySet().removeIf(entry -> {
			ProjectileData data = entry.getValue();
			data.ticksRemaining--;
			return data.ticksRemaining <= 0;
		});
	}

	private boolean isArchaeologistProjectile(int projectileId) {
		// Add known projectile IDs here after testing
		// For now, log all projectiles when special attack is active
		return specialAttackActive;
	}

	private void addDangerousTiles(WorldPoint center, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				dangerousTiles.add(center.dx(dx).dy(dy));
			}
		}
	}

	private void handleSpecialAttack() {
		log.info("handleSpecialAttack() called!");

		if (config.useSound()) {
			log.info("Playing sound effect ID: {}", config.soundEffect().getId());
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

	public Map<Projectile, ProjectileData> getActiveProjectiles() {
		return activeProjectiles;
	}

	public Set<WorldPoint> getDangerousTiles() {
		return dangerousTiles;
	}

	// Data class to track projectile information
	public static class ProjectileData {
		public final Projectile projectile;
		public final WorldPoint targetTile;
		public final int startTick;
		public int ticksRemaining;

		public ProjectileData(Projectile projectile, WorldPoint targetTile, int startTick, int cycles) {
			this.projectile = projectile;
			this.targetTile = targetTile;
			this.startTick = startTick;
			this.ticksRemaining = cycles;
		}
	}
}