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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.Getter;
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
	private static final String SPECIAL_ATTACK_TEXT = "Rain of knowledge!";
	private static final int SPECIAL_ATTACK_PROJECTILE_ID = 1260;
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

	@Getter
	private final Set<WorldPoint> dangerousTiles = new HashSet<>();

	// Map each tile to the tick it should be cleared at
	private final Map<WorldPoint, Integer> tileClearTimes = new HashMap<>();

	// Track which projectiles we've already processed to avoid duplicates
	private final Set<WorldPoint> trackedProjectiles = new HashSet<>();

	@Provides
	CrazyArchaeologistConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CrazyArchaeologistConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		dangerousTiles.clear();
		tileClearTimes.clear();
		trackedProjectiles.clear();
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) event.getActor();
		if (npc.getId() != CRAZY_ARCHAEOLOGIST_ID)
		{
			return;
		}

		String overheadText = event.getOverheadText();
		if (overheadText != null && overheadText.contains(SPECIAL_ATTACK_TEXT))
		{
			handleSpecialAttack();
			// Clear tracked projectiles for the new special attack
			trackedProjectiles.clear();
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();
		if (projectile.getId() != SPECIAL_ATTACK_PROJECTILE_ID)
		{
			return;
		}

		WorldPoint worldTarget = projectile.getTargetPoint();
		if (worldTarget == null)
		{
			return;
		}

		// Only process each unique projectile target once
		if (trackedProjectiles.contains(worldTarget))
		{
			return;
		}
		trackedProjectiles.add(worldTarget);

		int ticksUntilClear = (projectile.getRemainingCycles() / 30) + EXPLOSION_DELAY_TICKS;
		int clearTick = client.getTickCount() + ticksUntilClear;

		log.info("New projectile at {}, ticks until clear: {}, clearTick: {}, currentTick: {}",
				worldTarget, ticksUntilClear, clearTick, client.getTickCount());

		addDangerousTilesWithTimer(worldTarget, clearTick);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int currentTick = client.getTickCount();
		Set<WorldPoint> tilesToRemove = new HashSet<>();

		// Check each tile to see if it should be cleared
		for (Map.Entry<WorldPoint, Integer> entry : tileClearTimes.entrySet())
		{
			if (currentTick >= entry.getValue())
			{
				tilesToRemove.add(entry.getKey());
			}
		}

		// Remove expired tiles
		for (WorldPoint tile : tilesToRemove)
		{
			dangerousTiles.remove(tile);
			tileClearTimes.remove(tile);
			log.info("Cleared tile {} at tick {}", tile, currentTick);
		}

		// Clean up tracked projectiles that are definitely expired
		if (dangerousTiles.isEmpty())
		{
			trackedProjectiles.clear();
		}
	}

	private void addDangerousTilesWithTimer(WorldPoint center, int clearTick)
	{
		for (int dx = -DANGEROUS_TILE_RADIUS; dx <= DANGEROUS_TILE_RADIUS; dx++)
		{
			for (int dy = -DANGEROUS_TILE_RADIUS; dy <= DANGEROUS_TILE_RADIUS; dy++)
			{
				WorldPoint tile = center.dx(dx).dy(dy);

				// If tile is already marked, only update if new clear time is later
				if (tileClearTimes.containsKey(tile))
				{
					int existingClearTick = tileClearTimes.get(tile);
					if (clearTick > existingClearTick)
					{
						tileClearTimes.put(tile, clearTick);
						log.info("Updated tile {} clear time from {} to {}", tile, existingClearTick, clearTick);
					}
				}
				else
				{
					dangerousTiles.add(tile);
					tileClearTimes.put(tile, clearTick);
				}
			}
		}
	}

	private void handleSpecialAttack()
	{
		if (config.playSoundEffect())
		{
			client.playSoundEffect(config.soundEffect().getId());
		}

		if (config.sendNotification())
		{
			notifier.notify("Crazy Archaeologist special attack incoming!");
		}

		if (config.showChatMessage())
		{
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					"Crazy Archaeologist special attack!",
					null
			);
		}
	}
}