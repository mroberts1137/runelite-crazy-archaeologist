package com.crazyarchaeologist;

import net.runelite.client.config.*;

import java.awt.event.KeyEvent;

@ConfigGroup("crazyarchaeologist")
public interface CrazyArchaeologistConfig extends Config {

	@ConfigItem(
			keyName = "testHotkey",
			name = "Manual Test Trigger",
			description = "Press this key to manually test the alert (for debugging)"
	)
	default Keybind testHotkey() {
		return new Keybind(KeyEvent.VK_F8, 0);
	}

	@ConfigItem(
			keyName = "useSound",
			name = "Play Sound",
			description = "Play a sound when special attack is detected"
	)
	default boolean useSound() {
		return true;
	}

	@ConfigItem(
			keyName = "soundEffect",
			name = "Sound Effect",
			description = "Which sound effect to play"
	)
	default SoundEffect soundEffect() {
		return SoundEffect.GE_COIN;
	}

	@ConfigItem(
			keyName = "useNotification",
			name = "System Notification",
			description = "Send a system notification when special attack is detected"
	)
	default boolean useNotification() {
		return true;
	}

	@ConfigItem(
			keyName = "useGameMessage",
			name = "Game Message",
			description = "Display a message in the game chat when special attack is detected"
	)
	default boolean useGameMessage() {
		return true;
	}

	@ConfigSection(
			name = "Overlay Settings",
			description = "Configure tile marking",
			position = 10
	)
	String overlaySection = "overlay";

	@ConfigItem(
			keyName = "showOverlay",
			name = "Show Tile Markers",
			description = "Mark dangerous tiles where projectiles will land",
			section = overlaySection
	)
	default boolean showOverlay() {
		return true;
	}

	@Range(
			min = 0,
			max = 255
	)
	@ConfigItem(
			keyName = "tileAlpha",
			name = "Tile Transparency",
			description = "Alpha value for dangerous tile highlighting (0-255)",
			section = overlaySection
	)
	default int tileAlpha() {
		return 100;
	}

	enum SoundEffect {
		ALARM(2266),
		BOOP(3930),
		PRAYER(2672),
		GE_COIN(3924);

		private final int id;

		SoundEffect(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}
}