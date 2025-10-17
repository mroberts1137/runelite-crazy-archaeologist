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

import java.awt.Color;

import lombok.Getter;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("crazyarchaeologist")
public interface CrazyArchaeologistConfig extends Config
{
	@ConfigSection(
			name = "Boss Selection",
			description = "Choose which bosses to track",
			position = 0
	)
	String bossSection = "bossSelection";

	@ConfigSection(
			name = "Notifications",
			description = "Notification settings",
			position = 1
	)
	String notificationSection = "notifications";

	@ConfigSection(
			name = "Overlay",
			description = "Tile marker settings",
			position = 2
	)
	String overlaySection = "overlay";

	@ConfigSection(
			name = "Debug",
			description = "Debug settings",
			position = 3
	)
	String debugSection = "debug";

	@ConfigItem(
			keyName = "trackCrazyArchaeologist",
			name = "Track Crazy Archaeologist",
			description = "Enable tracking for Crazy Archaeologist",
			section = bossSection,
			position = 0
	)
	default boolean trackCrazyArchaeologist()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackDerangedArchaeologist",
			name = "Track Deranged Archaeologist",
			description = "Enable tracking for Deranged Archaeologist",
			section = bossSection,
			position = 1
	)
	default boolean trackDerangedArchaeologist()
	{
		return true;
	}

	@ConfigItem(
			keyName = "playSoundEffect",
			name = "Play sound",
			description = "Play a sound when the special attack is used",
			section = notificationSection,
			position = 0
	)
	default boolean playSoundEffect()
	{
		return true;
	}

	@ConfigItem(
			keyName = "soundEffect",
			name = "Sound effect",
			description = "Which sound effect to play",
			section = notificationSection,
			position = 1
	)
	default SoundEffect soundEffect()
	{
		return SoundEffect.ALERT;
	}

	@ConfigItem(
			keyName = "sendNotification",
			name = "Send notification",
			description = "Send a system notification when the special attack is used",
			section = notificationSection,
			position = 2
	)
	default boolean sendNotification()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showChatMessage",
			name = "Chat message",
			description = "Display a message in game chat when the special attack is used",
			section = notificationSection,
			position = 3
	)
	default boolean showChatMessage()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showTileMarkers",
			name = "Highlight tiles",
			description = "Highlight dangerous tiles where projectiles will land",
			section = overlaySection,
			position = 0
	)
	default boolean showTileMarkers()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
			keyName = "tileFillColor",
			name = "Fill color",
			description = "Color to fill dangerous tiles",
			section = overlaySection,
			position = 1
	)
	default Color tileFillColor()
	{
		return new Color(255, 0, 0, 25);
	}

	@ConfigItem(
			keyName = "tileBorderColor",
			name = "Border color",
			description = "Color for dangerous tile borders",
			section = overlaySection,
			position = 2
	)
	default Color tileBorderColor()
	{
		return Color.RED;
	}

	@ConfigItem(
			keyName = "tileBorderWidth",
			name = "Border width",
			description = "Width of the tile border in pixels",
			section = overlaySection,
			position = 3
	)
	default int tileBorderWidth()
	{
		return 2;
	}

	@ConfigItem(
			keyName = "enableDebugLogging",
			name = "Enable Debug Logging",
			description = "Enable debug logging to help identify NPC IDs, projectile IDs, and overhead text. Check the RuneLite logs.",
			section = debugSection,
			position = 0
	)
	default boolean enableDebugLogging()
	{
		return false;
	}

	enum SoundEffect
	{
		ALERT("Alert", 4039),
		BELL("Bell", 3672),
		CHIME("Chime", 3930),
		PRAYER("Prayer", 2672),
		ALARM("Alarm", 2266),
		GE_COIN("GE coin", 3924);

		private final String name;
		@Getter
		private final int id;

		SoundEffect(String name, int id)
		{
			this.name = name;
			this.id = id;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}
}