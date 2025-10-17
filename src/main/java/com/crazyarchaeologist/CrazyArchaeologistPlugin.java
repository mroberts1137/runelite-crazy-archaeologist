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
		description = "Alerts and highlights dangerous tiles during Crazy Archaeologist's special attack",
		tags = {"boss", "pvm", "wilderness", "combat", "overlay"}
)
public class CrazyArchaeologistPlugin extends Plugin
{
	private static final int CRAZY_ARCHAEOLOGIST_ID = 6618;
	private static final int DERANGED_ARCHAEOLOGIST_ID = 7806;
	private static final String CRAZY_SPECIAL_ATTACK_TEXT = "Rain of knowledge!";
	private static final String DERANGED_SPECIAL_ATTACK_TEXT = "Learn to read!";
	private static final int CRAZY_PROJECTILE_ID = 1260;
	private static final int DERANGED_PROJECTILE_ID = 1260;
	private static final int DANGEROUS_TILE_RADIUS = 1;
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

		// Debug logging - log all NPC overhead text with their IDs
		if (config.debugLogging())
		{
			log.info("NPC Overhead Text - ID: {}, Name: '{}', Text: '{}'",
					npc.getId(), npc.getName(), overheadText);
		}

		// Check if this is Crazy Archaeologist
		if (config.trackCrazyArchaeologist() && npc.getId() == CRAZY_ARCHAEOLOGIST_ID)
		{
			if (overheadText != null && overheadText.contains(CRAZY_SPECIAL_ATTACK_TEXT))
			{
				log.info("Crazy Archaeologist special attack detected!");
				handleSpecialAttack("Crazy Archaeologist");
			}
		}

		// Check if this is Deranged Archaeologist
		if (config.trackDerangedArchaeologist() && npc.getId() == DERANGED_ARCHAEOLOGIST_ID)
		{
			if (overheadText != null && overheadText.contains(DERANGED_SPECIAL_ATTACK_TEXT))
			{
				log.info("Deranged Archaeologist special attack detected!");
				handleSpecialAttack("Deranged Archaeologist");
			}
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();

		// Debug logging - log all projectiles
		if (config.debugLogging())
		{
			WorldPoint target = projectile.getTargetPoint();
			log.info("Projectile Moved - ID: {}, Target: {}, Remaining Cycles: {}, Start Cycle: {}, End Cycle: {}",
					projectile.getId(),
					target != null ? target.toString() : "null",
					projectile.getRemainingCycles(),
					projectile.getStartCycle(),
					projectile.getEndCycle());
		}

		boolean isCrazyProjectile = config.trackCrazyArchaeologist() && projectile.getId() == CRAZY_PROJECTILE_ID;
		boolean isDerangedProjectile = config.trackDerangedArchaeologist() && projectile.getId() == DERANGED_PROJECTILE_ID;

		if (!isCrazyProjectile && !isDerangedProjectile)
		{
			return;
		}

		WorldPoint worldTarget = projectile.getTargetPoint();
		if (worldTarget == null)
		{
			return;
		}

		String bossName = isCrazyProjectile ? "Crazy" : "Deranged";
		log.info("{} Archaeologist special attack projectile tracked at {}", bossName, worldTarget);

		// Calculate when this specific projectile's tiles should clear
		int ticksUntilClear = (projectile.getRemainingCycles() / 30) + EXPLOSION_DELAY_TICKS;
		int clearTick = client.getTickCount() + ticksUntilClear;

		log.debug("Projectile targeting {} will clear at tick {} (current: {}, remaining: {})",
				worldTarget, clearTick, client.getTickCount(), ticksUntilClear);

		// Add all tiles in the 3x3 area around the target
		addDangerousTiles(worldTarget, clearTick);
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
				log.debug("Clearing tile {} at tick {}", entry.getKey(), currentTick);
				iterator.remove();
			}
		}
	}

	/**
	 * Adds a 3x3 area of dangerous tiles centered on the given point.
	 * If a tile is already marked with an earlier clear time, updates it to the later time.
	 */
	private void addDangerousTiles(WorldPoint center, int clearTick)
	{
		for (int dx = -DANGEROUS_TILE_RADIUS; dx <= DANGEROUS_TILE_RADIUS; dx++)
		{
			for (int dy = -DANGEROUS_TILE_RADIUS; dy <= DANGEROUS_TILE_RADIUS; dy++)
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