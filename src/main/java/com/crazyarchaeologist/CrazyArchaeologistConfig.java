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
}