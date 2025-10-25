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
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
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
	private static final int CRAZY_ARCHAEOLOGIST_ID = 6618;
	private static final String CRAZY_SPECIAL_ATTACK_TEXT = "Rain of knowledge!";
	private static final int CRAZY_EXPLOSION_RADIUS = 1;

	private static final int DERANGED_ARCHAEOLOGIST_ID = 7806;
	private static final String DERANGED_SPECIAL_ATTACK_TEXT = "Learn to Read!";
	private static final int DERANGED_EXPLOSION_RADIUS = 1;

	private static final int CHAOS_FANATIC_ID = 6619;
	private static final int CHAOS_FANATIC_PROJECTILE_ID = 551;
	private static final int CHAOS_FANATIC_EXPLOSION_RADIUS = 0;

	private static final int ARCHAEOLOGIST_PROJECTILE_ID = 1260;
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

	private final Map<WorldPoint, Integer> tileClearTimes = new HashMap<>();
	private boolean crazyArchaeologistActive = false;
	private boolean derangedArchaeologistActive = false;
	private boolean chaosFanaticAlertSent = false;

	@Provides
	CrazyArchaeologistConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CrazyArchaeologistConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		tileClearTimes.clear();
		crazyArchaeologistActive = false;
		derangedArchaeologistActive = false;
		chaosFanaticAlertSent = false;
	}

	public Set<WorldPoint> getDangerousTiles()
	{
		return tileClearTimes.keySet();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		int npcId = npc.getId();

		if (npcId == CRAZY_ARCHAEOLOGIST_ID) {
			crazyArchaeologistActive = true;
		} else if (npcId == DERANGED_ARCHAEOLOGIST_ID)
		{
			derangedArchaeologistActive = true;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		int npcId = npc.getId();

		if (npcId == CRAZY_ARCHAEOLOGIST_ID) {
			crazyArchaeologistActive = false;
		} else if (npcId == DERANGED_ARCHAEOLOGIST_ID)
		{
			derangedArchaeologistActive = false;
		}
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
		int npcId = npc.getId();

		if (overheadText == null)
		{
			return;
		}

		if (npcId == CRAZY_ARCHAEOLOGIST_ID && config.trackCrazyArchaeologist()
				&& overheadText.contains(CRAZY_SPECIAL_ATTACK_TEXT))
		{
			handleSpecialAttack("Crazy Archaeologist");
		}
		else if (npcId == DERANGED_ARCHAEOLOGIST_ID && config.trackDerangedArchaeologist()
				&& overheadText.contains(DERANGED_SPECIAL_ATTACK_TEXT))
		{
			handleSpecialAttack("Deranged Archaeologist");
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();
		int projectileId = projectile.getId();

		if (projectileId != ARCHAEOLOGIST_PROJECTILE_ID && projectileId != CHAOS_FANATIC_PROJECTILE_ID)
		{
			return;
		}

		WorldPoint worldTarget = projectile.getTargetPoint();
		if (worldTarget == null)
		{
			return;
		}

		int explosionRadius;
		boolean isCrazyProjectile = config.trackCrazyArchaeologist() && projectileId == ARCHAEOLOGIST_PROJECTILE_ID && crazyArchaeologistActive;
		boolean isDerangedProjectile = config.trackDerangedArchaeologist() && projectileId == ARCHAEOLOGIST_PROJECTILE_ID && derangedArchaeologistActive;
		boolean isChaosProjectile = config.trackChaosFanatic() && projectileId == CHAOS_FANATIC_PROJECTILE_ID;

		if (!isCrazyProjectile && !isDerangedProjectile && !isChaosProjectile)
		{
			return;
		}

		if (isChaosProjectile)
		{
			explosionRadius = CHAOS_FANATIC_EXPLOSION_RADIUS;

			if (!chaosFanaticAlertSent)
			{
				handleSpecialAttack("Chaos Fanatic");
				chaosFanaticAlertSent = true;
			}
		}
		else
		{
			explosionRadius = CRAZY_EXPLOSION_RADIUS;
		}

		int ticksUntilClear = (projectile.getRemainingCycles() / 30) + EXPLOSION_DELAY_TICKS;
		int clearTick = client.getTickCount() + ticksUntilClear;
		addDangerousTiles(worldTarget, clearTick, explosionRadius);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int currentTick = client.getTickCount();

        tileClearTimes.entrySet().removeIf(entry -> currentTick >= entry.getValue());

		if (tileClearTimes.isEmpty() && chaosFanaticAlertSent)
		{
			chaosFanaticAlertSent = false;
		}
	}

	private void addDangerousTiles(WorldPoint center, int clearTick, int radius)
	{
		for (int dx = -radius; dx <= radius; dx++)
		{
			for (int dy = -radius; dy <= radius; dy++)
			{
				WorldPoint tile = center.dx(dx).dy(dy);
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