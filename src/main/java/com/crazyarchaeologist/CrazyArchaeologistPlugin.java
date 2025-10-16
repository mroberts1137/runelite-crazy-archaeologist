package com.crazyarchaeologist;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@PluginDescriptor(
		name = "Crazy Archaeologist Helper",
		description = "Alerts you when Crazy Archaeologist uses his special attack and shows where projectiles will land",
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

	@Inject
	private CrazyArchaeologistOverlay overlay;

	// Track active projectiles and their landing spots
	private final Map<Projectile, WorldPoint> activeProjectiles = new HashMap<>();
	private final List<TileDanger> dangerousTiles = new ArrayList<>();

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
				log.info("Special attack detected via overhead text!");
				handleSpecialAttack();
			}
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event) {
		Projectile projectile = event.getProjectile();

		// Log all projectile information to find the right IDs
		if (projectile.getId() > 0) {
			Actor interacting = projectile.getInteracting();

			// Check if this projectile might be from Crazy Archaeologist
			if (interacting instanceof NPC) {
				NPC npc = (NPC) interacting;
				if (npc.getId() == CRAZY_ARCHAEOLOGIST_ID) {
					logProjectileInfo(projectile, "FROM Crazy Archaeologist");
				}
			}

			// Also log projectiles with no clear source (might be the special attack)
			if (interacting == null) {
				// Calculate distance from player to see if it's relevant
				LocalPoint projectilePoint = new LocalPoint(
						projectile.getX1(), projectile.getY1()
				);
				Player player = client.getLocalPlayer();
				if (player != null) {
					int distance = projectilePoint.distanceTo(player.getLocalLocation());
					if (distance < 2000) { // Only log nearby projectiles
						logProjectileInfo(projectile, "NO SOURCE (possible special)");
					}
				}
			}

			// Calculate where this projectile will land
			if (!activeProjectiles.containsKey(projectile)) {
				WorldPoint landingPoint = calculateLandingPoint(projectile);
				if (landingPoint != null) {
					activeProjectiles.put(projectile, landingPoint);
					log.info("Tracked new projectile landing at: {}", landingPoint);
				}
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		// Clean up expired projectiles and update dangerous tiles
		List<Projectile> toRemove = new ArrayList<>();

		for (Map.Entry<Projectile, WorldPoint> entry : activeProjectiles.entrySet()) {
			Projectile proj = entry.getKey();

			// Check if projectile is finished
			if (proj.getRemainingCycles() <= 0) {
				toRemove.add(proj);

				// Add danger tiles when projectile lands (3x3 area)
				WorldPoint center = entry.getValue();
				int duration = 5; // Ticks the tiles stay dangerous after impact

				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						WorldPoint dangerPoint = new WorldPoint(
								center.getX() + dx,
								center.getY() + dy,
								center.getPlane()
						);
						dangerousTiles.add(new TileDanger(dangerPoint, duration));
					}
				}
			}
		}

		toRemove.forEach(activeProjectiles::remove);

		// Update dangerous tile timers
		dangerousTiles.removeIf(tile -> {
			tile.ticksRemaining--;
			return tile.ticksRemaining <= 0;
		});
	}

//	@Subscribe
//	public void onAnimationChanged(AnimationChanged event) {
//		if (!(event.getActor() instanceof NPC)) {
//			return;
//		}
//
//		NPC npc = (NPC) event.getActor();
//
//		if (npc.getId() == CRAZY_ARCHAEOLOGIST_ID) {
//			int animation = npc.getAnimation();
//
//			if (animation != -1) {
//				log.info("Crazy Archaeologist animation changed to: {}", animation);
//			}
//		}
//	}

	private void logProjectileInfo(Projectile projectile, String context) {
		log.info("=== PROJECTILE {} ===", context);
		log.info("  ID: {}", projectile.getId());
		log.info("  Start: ({}, {})", projectile.getX1(), projectile.getY1());
		log.info("  End: ({}, {})", projectile.getX(), projectile.getY());
		log.info("  Start Height: {}", projectile.getStartHeight());
		log.info("  End Height: {}", projectile.getEndHeight());
		log.info("  Start Cycle: {}", projectile.getStartCycle());
		log.info("  End Cycle: {}", projectile.getEndCycle());
		log.info("  Remaining Cycles: {}", projectile.getRemainingCycles());
		log.info("  Slope: {}", projectile.getSlope());
		log.info("  Floor: {}", projectile.getFloor());

		Actor interacting = projectile.getInteracting();
		if (interacting != null) {
			log.info("  Interacting: {} ({})",
					interacting.getName(),
					interacting instanceof NPC ? ((NPC) interacting).getId() : "Player");
		} else {
			log.info("  Interacting: null");
		}
	}

	private WorldPoint calculateLandingPoint(Projectile projectile) {
		LocalPoint startPoint = new LocalPoint(projectile.getX1(), projectile.getY1());

		// The target location is given by getX() and getY()
		LocalPoint endPoint = new LocalPoint((int) projectile.getX(), (int) projectile.getY());

		WorldPoint worldEnd = WorldPoint.fromLocal(client, endPoint);

		return worldEnd;
	}

	private void handleSpecialAttack() {
		log.info("handleSpecialAttack() called!");

		if (config.useSound()) {
			log.info("Playing sound effect ID: {}", config.soundEffect().getId());
			try {
				client.playSoundEffect(config.soundEffect().getId());
				log.info("Sound effect played successfully");
			} catch (Exception e) {
				log.error("Error playing sound effect", e);
			}
		}

		if (config.useNotification()) {
			log.info("Sending notification...");
			try {
				notifier.notify("Crazy Archaeologist: Special Attack Incoming!");
				log.info("Notification sent successfully");
			} catch (Exception e) {
				log.error("Error sending notification", e);
			}
		}

		if (config.useGameMessage()) {
			log.info("Adding game message...");
			try {
				client.addChatMessage(
						ChatMessageType.GAMEMESSAGE,
						"",
						"<col=ff0000>Crazy Archaeologist Special Attack!</col>",
						null
				);
				log.info("Game message added successfully");
			} catch (Exception e) {
				log.error("Error adding game message", e);
			}
		}
	}

	// Getters for overlay
	public Map<Projectile, WorldPoint> getActiveProjectiles() {
		return activeProjectiles;
	}

	public List<TileDanger> getDangerousTiles() {
		return dangerousTiles;
	}

	// Inner class to track dangerous tiles
	public static class TileDanger {
		public final WorldPoint location;
		public int ticksRemaining;

		public TileDanger(WorldPoint location, int ticksRemaining) {
			this.location = location;
			this.ticksRemaining = ticksRemaining;
		}
	}
}