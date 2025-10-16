package com.crazyarchaeologist;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.ConfigSection;

import java.awt.event.KeyEvent;

@ConfigGroup("crazyarchaeologist")
public interface CrazyArchaeologistConfig extends Config {

	@ConfigSection(
			name = "Alerts",
			description = "Alert settings",
			position = 0
	)
	String alertSection = "alerts";

	@ConfigSection(
			name = "Visuals",
			description = "Visual indicator settings",
			position = 1
	)
	String visualSection = "visuals";

	@ConfigItem(
			keyName = "testHotkey",
			name = "Manual Test Trigger",
			description = "Press this key to manually test the alert (for debugging)",
			section = alertSection
	)
	default Keybind testHotkey() {
		return new Keybind(KeyEvent.VK_F8, 0);
	}

	@ConfigItem(
			keyName = "useSound",
			name = "Play Sound",
			description = "Play a sound when special attack is detected",
			section = alertSection
	)
	default boolean useSound() {
		return true;
	}

	@ConfigItem(
			keyName = "soundEffect",
			name = "Sound Effect",
			description = "Which sound effect to play",
			section = alertSection
	)
	default SoundEffect soundEffect() {
		return SoundEffect.GE_COIN;
	}

	@ConfigItem(
			keyName = "useNotification",
			name = "System Notification",
			description = "Send a system notification when special attack is detected",
			section = alertSection
	)
	default boolean useNotification() {
		return true;
	}

	@ConfigItem(
			keyName = "useGameMessage",
			name = "Game Message",
			description = "Display a message in the game chat when special attack is detected",
			section = alertSection
	)
	default boolean useGameMessage() {
		return true;
	}

	@ConfigItem(
			keyName = "showProjectiles",
			name = "Show Projectiles",
			description = "Highlight active projectiles and where they will land",
			section = visualSection
	)
	default boolean showProjectiles() {
		return true;
	}

	@ConfigItem(
			keyName = "showDangerTiles",
			name = "Show Danger Tiles",
			description = "Highlight tiles that are dangerous after projectiles land",
			section = visualSection
	)
	default boolean showDangerTiles() {
		return true;
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