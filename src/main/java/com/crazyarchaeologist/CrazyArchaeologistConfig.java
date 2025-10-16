package com.crazyarchaeologist;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("crazyarchaeologist")
public interface CrazyArchaeologistConfig extends Config
{
    @ConfigItem(
            keyName = "enableNotification",
            name = "Enable Notification",
            description = "Enables Windows notification when special attack is used",
            position = 1
    )
    default boolean enableNotification()
    {
        return true;
    }

    @ConfigItem(
            keyName = "enableSound",
            name = "Enable Sound",
            description = "Plays a sound when special attack is used",
            position = 2
    )
    default boolean enableSound()
    {
        return true;
    }

    @ConfigItem(
            keyName = "useAnimationDetection",
            name = "Use Animation Detection",
            description = "Detect special attack by animation instead of chat message (more reliable but may need animation ID verification)",
            position = 3
    )
    default boolean useAnimationDetection()
    {
        return false;
    }
}