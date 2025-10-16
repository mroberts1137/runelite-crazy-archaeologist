package com.crazyarchaeologist;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

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
		return SoundEffect.ALARM;
	}

	@ConfigItem(
			keyName = "useNotification",
			name = "System Notification",
			description = "Send a system notification when special attack is detected (requires RuneLite notifications to be enabled)"
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

	enum SoundEffect {
		ALARM(2266),  // Alarm sound
		BOOP(3930),   // UI Boop
		PRAYER(2672), // Prayer activation
		GE_COIN(3924); // Grand Exchange coin sound

		private final int id;

		SoundEffect(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}
}