package com.crazyarchaeologist;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@PluginDescriptor(
		name = "Crazy Archaeologist Helper",
		description = "Alerts you when Crazy Archaeologist uses his special attack",
		tags = {"boss", "pvm", "wilderness", "archaeologist", "crazy"}
)
public class CrazyArchaeologistPlugin extends Plugin {

	private static final int CRAZY_ARCHAEOLOGIST_ID = 6619;
	private static final String SPECIAL_ATTACK_TEXT = "Rain of knowledge!";
	private static final int SPECIAL_ATTACK_ANIMATION = 1162; // You may need to verify this animation ID

	@Inject
	private Client client;

	@Inject
	private Notifier notifier;

	@Inject
	private CrazyArchaeologistConfig config;

	private final Map<Integer, NPC> crazyArchaeologists = new HashMap<>();

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
		crazyArchaeologists.clear();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event) {
		NPC npc = event.getNpc();
		if (npc.getId() == CRAZY_ARCHAEOLOGIST_ID) {
			crazyArchaeologists.put(npc.getIndex(), npc);
			log.debug("Crazy Archaeologist spawned at index: {}", npc.getIndex());
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event) {
		NPC npc = event.getNpc();
		if (npc.getId() == CRAZY_ARCHAEOLOGIST_ID) {
			crazyArchaeologists.remove(npc.getIndex());
			log.debug("Crazy Archaeologist despawned at index: {}", npc.getIndex());
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event) {
		if (!(event.getActor() instanceof NPC)) {
			return;
		}

		NPC npc = (NPC) event.getActor();

		// Check if this is a Crazy Archaeologist
		if (crazyArchaeologists.containsKey(npc.getIndex())) {
			int animation = npc.getAnimation();

			// Log all animations for debugging (remove this in production)
			if (animation != -1) {
				log.debug("Crazy Archaeologist animation: {}", animation);
			}

			// Check for special attack animation
			if (animation == SPECIAL_ATTACK_ANIMATION) {
				handleSpecialAttack();
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event) {
		if (event.getType() != ChatMessageType.PUBLICCHAT || crazyArchaeologists.isEmpty()) {
			return;
		}

		String message = Text.removeTags(event.getMessage());

		// Check if the message contains the special attack phrase
		if (message.contains(SPECIAL_ATTACK_TEXT)) {
			handleSpecialAttack();
		}
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