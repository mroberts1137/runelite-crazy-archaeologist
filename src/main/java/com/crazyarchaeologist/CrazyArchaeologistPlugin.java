package com.crazyarchaeologist;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;

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

	private static final int CRAZY_ARCHAEOLOGIST_ID = 6618;
	private static final String SPECIAL_ATTACK_TEXT = "Rain of knowledge!";

	@Inject
	private Client client;

	@Inject
	private Notifier notifier;

	@Inject
	private CrazyArchaeologistConfig config;

	@Inject
	private KeyManager keyManager;

	private final Map<Integer, NPC> crazyArchaeologists = new HashMap<>();

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.testHotkey()) {
		@Override
		public void hotkeyPressed() {
			log.info("Manual trigger activated!");
			handleSpecialAttack();
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
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Crazy Archaeologist Helper stopped!");
		keyManager.unregisterKeyListener(hotkeyListener);
		crazyArchaeologists.clear();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event) {
		NPC npc = event.getNpc();
		if (npc.getId() == CRAZY_ARCHAEOLOGIST_ID) {
			crazyArchaeologists.put(npc.getIndex(), npc);
			log.info("Crazy Archaeologist spawned! Index: {}, Name: {}", npc.getIndex(), npc.getName());
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event) {
		NPC npc = event.getNpc();
		if (npc.getId() == CRAZY_ARCHAEOLOGIST_ID) {
			crazyArchaeologists.remove(npc.getIndex());
			log.info("Crazy Archaeologist despawned! Index: {}", npc.getIndex());
		}
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event) {
		if (!(event.getActor() instanceof NPC)) {
			return;
		}

		NPC npc = (NPC) event.getActor();
		String overheadText = event.getOverheadText();

		log.info("NPC overhead text changed - NPC ID: {}, Name: {}, Text: '{}'",
				npc.getId(), npc.getName(), overheadText);

		// Check if this is a Crazy Archaeologist
		if (npc.getId() == CRAZY_ARCHAEOLOGIST_ID || crazyArchaeologists.containsKey(npc.getIndex())) {
			log.info("Crazy Archaeologist said: '{}'", overheadText);

			// Check if the overhead text contains the special attack phrase
			if (overheadText != null && overheadText.contains(SPECIAL_ATTACK_TEXT)) {
				log.info("Special attack detected via overhead text!");
				handleSpecialAttack();
			}
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event) {
		if (!(event.getActor() instanceof NPC)) {
			return;
		}

		NPC npc = (NPC) event.getActor();

		// Check if this is a Crazy Archaeologist
		if (npc.getId() == CRAZY_ARCHAEOLOGIST_ID || crazyArchaeologists.containsKey(npc.getIndex())) {
			int animation = npc.getAnimation();

			// Log all animations for debugging
			if (animation != -1) {
				log.info("Crazy Archaeologist animation changed to: {}", animation);
			}
		}
	}

	private void handleSpecialAttack() {
		log.info("handleSpecialAttack() called!");

		if (config.useSound()) {
			log.info("Playing sound effect...");
			client.playSoundEffect(SoundEffectID.UI_BOOP);
		}

		if (config.useNotification()) {
			log.info("Sending notification...");
			notifier.notify("Crazy Archaeologist: Special Attack Incoming!");
		}

		if (config.useGameMessage()) {
			log.info("Adding game message...");
			client.addChatMessage(
					ChatMessageType.GAMEMESSAGE,
					"",
					"<col=ff0000>Crazy Archaeologist Special Attack!</col>",
					null
			);
		}
	}
}