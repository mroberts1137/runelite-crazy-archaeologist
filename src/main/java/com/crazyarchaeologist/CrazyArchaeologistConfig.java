package com.crazyarchaeologist;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("crazyarchaeologist")
public interface CrazyArchaeologistConfig extends Config {

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