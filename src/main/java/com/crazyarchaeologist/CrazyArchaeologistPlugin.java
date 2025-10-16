package com.crazyarchaeologist;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@PluginDescriptor(
		name = "Crazy Archaeologist Helper",
		description = "Alerts you when Crazy Archaeologist uses his special attack",
		tags = {"boss", "pvm", "wilderness", "archaeologist", "crazy"}
)
public class CrazyArchaeologistPlugin extends Plugin {

	private static final int CRAZY_ARCHAEOLOGIST_ID = 6619;
	private static final String SPECIAL_ATTACK_TEXT = "Rain of knowledge!";
	private static final int SEARCH_RADIUS = 15; // tiles

	@Inject
	private Client client;

	@Inject
	private Notifier notifier;

	@Inject
	private CrazyArchaeologistConfig config;

	private boolean nearBoss = false;

	@Provides
	CrazyArchaeologistConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(CrazyArchaeologistConfig.class);
	}

	@Override
	protected void startUp() throws Exception {
		log.info("Crazy Archaeologist Helper started!");
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Crazy Archaeologist Helper stopped!");
		nearBoss = false;
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (client.getGameState() != GameState.LOGGED_IN) {
			return;
		}

		Player player = client.getLocalPlayer();
		if (player == null) {
			return;
		}

		// Check if Crazy Archaeologist is nearby
		nearBoss = isNearCrazyArchaeologist(player);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event) {
		if (!nearBoss || event.getType() != ChatMessageType.PUBLICCHAT) {
			return;
		}

		String message = Text.removeTags(event.getMessage());

		// Check if the message contains the special attack phrase
		if (message.contains(SPECIAL_ATTACK_TEXT)) {
			handleSpecialAttack();
		}
	}

	private boolean isNearCrazyArchaeologist(Player player) {
		List<NPC> npcs = client.getNpcs();

		for (NPC npc : npcs) {
			if (npc.getId() == CRAZY_ARCHAEOLOGIST_ID) {
				int distance = npc.getWorldLocation().distanceTo(player.getWorldLocation());
				if (distance <= SEARCH_RADIUS) {
					return true;
				}
			}
		}

		return false;
	}

	private void handleSpecialAttack() {
		if (config.useSound()) {
			client.playSoundEffect(SoundEffectID.UI_BOOP);
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

		log.debug("Crazy Archaeologist special attack detected!");
	}
}