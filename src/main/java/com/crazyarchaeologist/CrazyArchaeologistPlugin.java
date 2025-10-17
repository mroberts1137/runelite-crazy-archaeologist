/*
 * Copyright (c) 2025, YourName <youremail@example.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.crazyarchaeologist;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
		name = "Crazy Archaeologist",
		description = "Alerts and highlights dangerous tiles during Crazy/Deranged Archaeologist and Chaos Fanatic special attacks",
		tags = {"boss", "pvm", "wilderness", "combat", "overlay", "crazy archaeologist", "deranged archaeologist", "chaos fanatic"}
)
public class CrazyArchaeologistPlugin extends Plugin
{
	// Crazy Archaeologist (Wilderness)
	private static final int CRAZY_ARCHAEOLOGIST_ID = 6618;
	private static final String CRAZY_SPECIAL_ATTACK_TEXT = "Rain of knowledge!";
	private static final int CRAZY_SPECIAL_ATTACK_PROJECTILE_ID = 1260;
	private static final int CRAZY_EXPLOSION_RADIUS = 1; // 3x3 area

	// Deranged Archaeologist (Fossil Island)
	private static final int DERANGED_ARCHAEOLOGIST_ID = 7806;
	private static final String DERANGED_SPECIAL_ATTACK_TEXT = "Learn to Read!";
	private static final int DERANGED_SPECIAL_ATTACK_PROJECTILE_ID = 1260;
	private static final int DERANGED_EXPLOSION_RADIUS = 1; // 3x3 area

	// Chaos Fanatic (Wilderness)
	private static final int CHAOS_FANATIC_ID = 6619;
	private static final int CHAOS_FANATIC_PROJECTILE_ID = 551;
	private static final int CHAOS_FANATIC_EXPLOSION_RADIUS = 0; // Single tile only

	private static final int EXPLOSION_DELAY_TICKS = 0;

	@Inject
	private Client client;

	@Inject
	private Notifier notifier;

	@Inject
	private CrazyArchaeologistConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private CrazyArchaeologistOverlay overlay;

	// Map each dangerous tile to its clear tick time
	private final Map<WorldPoint, Integer> tileClearTimes = new HashMap<>();

	// Track if we've already alerted for Chaos Fanatic's current attack
	private boolean chaosFanaticAlertSent = false;

	@Provides
	CrazyArchaeologistConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CrazyArchaeologistConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		log.info("Crazy Archaeologist plugin started - Debug logging enabled");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		tileClearTimes.clear();
		chaosFanaticAlertSent = false;
		log.info("Crazy Archaeologist plugin stopped");
	}

	/**
	 * Returns the set of currently dangerous tiles for the overlay to render
	 */
	public Set<WorldPoint> getDangerousTiles()
	{
		return tileClearTimes.keySet();
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) event.getActor();
		String overheadText = event.getOverheadText();

		// Log all NPC overhead text for debugging
		if (config.enableDebugLogging())
		{
			log.info("NPC Overhead Text - ID: {}, Name: {}, Text: '{}'",
					npc.getId(), npc.getName(), overheadText);
		}

		// Check if it's one of our tracked archaeologists (Chaos Fanatic doesn't use overhead text)
		boolean isCrazy = npc.getId() == CRAZY_ARCHAEOLOGIST_ID;
		boolean isDeranged = npc.getId() == DERANGED_ARCHAEOLOGIST_ID;

		if (!isCrazy && !isDeranged)
		{
			return;
		}

		// Check if the boss is enabled in config
		if (isCrazy && !config.trackCrazyArchaeologist())
		{
			return;
		}
		if (isDeranged && !config.trackDerangedArchaeologist())
		{
			return;
		}

		// Check for special attack text
		if (overheadText != null)
		{
			boolean isSpecialAttack = false;
			String bossName = "";

			if (isCrazy && overheadText.contains(CRAZY_SPECIAL_ATTACK_TEXT))
			{
				isSpecialAttack = true;
				bossName = "Crazy Archaeologist";
			}
			else if (isDeranged && overheadText.contains(DERANGED_SPECIAL_ATTACK_TEXT))
			{
				isSpecialAttack = true;
				bossName = "Deranged Archaeologist";
			}

			if (isSpecialAttack)
			{
				log.info("{} special attack detected!", bossName);
				handleSpecialAttack(bossName);
			}
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();

		// Log all projectiles when debug logging is enabled
		if (config.enableDebugLogging())
		{
			WorldPoint target = projectile.getTargetPoint();
			log.info("Projectile - ID: {}, RemainingCycles: {}, Target: {}",
					projectile.getId(),
					projectile.getRemainingCycles(),
					target != null ? target.toString() : "null");
		}

		// Check if it's one of our tracked projectiles
		boolean isCrazyProjectile = projectile.getId() == CRAZY_SPECIAL_ATTACK_PROJECTILE_ID;
		boolean isDerangedProjectile = projectile.getId() == DERANGED_SPECIAL_ATTACK_PROJECTILE_ID;
		boolean isChaosFanaticProjectile = projectile.getId() == CHAOS_FANATIC_PROJECTILE_ID;

		if (!isCrazyProjectile && !isDerangedProjectile && !isChaosFanaticProjectile)
		{
			return;
		}

		// Check if the corresponding boss is enabled
		if (isCrazyProjectile && !config.trackCrazyArchaeologist())
		{
			return;
		}
		if (isDerangedProjectile && !config.trackDerangedArchaeologist())
		{
			return;
		}
		if (isChaosFanaticProjectile && !config.trackChaosFanatic())
		{
			return;
		}

		WorldPoint worldTarget = projectile.getTargetPoint();
		if (worldTarget == null)
		{
			return;
		}

		// Determine the explosion radius for this boss
		int explosionRadius;
		String bossType;

		if (isCrazyProjectile)
		{
			explosionRadius = CRAZY_EXPLOSION_RADIUS;
			bossType = "Crazy Archaeologist";
		}
		else if (isDerangedProjectile)
		{
			explosionRadius = DERANGED_EXPLOSION_RADIUS;
			bossType = "Deranged Archaeologist";
		}
		else // isChaosFanaticProjectile
		{
			explosionRadius = CHAOS_FANATIC_EXPLOSION_RADIUS;
			bossType = "Chaos Fanatic";

			// Alert on first projectile detection
			if (!chaosFanaticAlertSent)
			{
				log.info("Chaos Fanatic special attack detected!");
				handleSpecialAttack(bossType);
				chaosFanaticAlertSent = true;
			}
		}

		// Calculate when this specific projectile's tiles should clear
		int ticksUntilClear = (projectile.getRemainingCycles() / 30) + EXPLOSION_DELAY_TICKS;
		int clearTick = client.getTickCount() + ticksUntilClear;

		log.info("{} projectile (ID: {}) targeting {} with radius {} will clear at tick {} (current: {}, remaining: {})",
				bossType, projectile.getId(), worldTarget, explosionRadius, clearTick, client.getTickCount(), ticksUntilClear);

		// Add dangerous tiles based on the explosion radius
		addDangerousTiles(worldTarget, clearTick, explosionRadius);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int currentTick = client.getTickCount();

		// Remove tiles whose clear time has passed
		Iterator<Map.Entry<WorldPoint, Integer>> iterator = tileClearTimes.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<WorldPoint, Integer> entry = iterator.next();
			if (currentTick >= entry.getValue())
			{
				if (config.enableDebugLogging())
				{
					log.info("Clearing tile {} at tick {}", entry.getKey(), currentTick);
				}
				iterator.remove();
			}
		}

		// Reset Chaos Fanatic alert flag when all tiles are cleared
		if (tileClearTimes.isEmpty() && chaosFanaticAlertSent)
		{
			chaosFanaticAlertSent = false;
			log.info("Chaos Fanatic attack ended, ready for next alert");
		}
	}

	/**
	 * Adds dangerous tiles in an area around the given center point.
	 * If a tile is already marked with an earlier clear time, updates it to the later time.
	 *
	 * @param center The center point of the explosion
	 * @param clearTick The game tick when these tiles should be cleared
	 * @param radius The radius of the explosion (0 = single tile, 1 = 3x3, 2 = 5x5, etc.)
	 */
	private void addDangerousTiles(WorldPoint center, int clearTick, int radius)
	{
		for (int dx = -radius; dx <= radius; dx++)
		{
			for (int dy = -radius; dy <= radius; dy++)
			{
				WorldPoint tile = center.dx(dx).dy(dy);

				// If tile already exists, keep the later (maximum) clear time
				// This ensures tiles don't clear prematurely if multiple projectiles overlap
				tileClearTimes.merge(tile, clearTick, Math::max);
			}
		}
	}

	private void handleSpecialAttack(String bossName)
	{
		if (config.playSoundEffect())
		{
			client.playSoundEffect(config.soundEffect().getId());
		}

		if (config.sendNotification())
		{
			notifier.notify(bossName + " special attack incoming!");
		}

		if (config.showChatMessage())
		{
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					bossName + " special attack!",
					null
			);
		}
	}
}