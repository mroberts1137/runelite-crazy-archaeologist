package com.crazyarchaeologist;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
        name = "Crazy Archaeologist Alert",
        description = "Alerts you when the Crazy Archaeologist uses his special attack",
        tags = {"boss", "notification", "pvm", "combat"}
)
public class CrazyArchaeologistPlugin extends Plugin
{
    private static final int CRAZY_ARCHAEOLOGIST_ID = 6619;
    private static final String SPECIAL_ATTACK_TEXT = "Rain of knowledge!";
    private static final int SPECIAL_ATTACK_ANIMATION = 3353;

    @Inject
    private Client client;

    @Inject
    private Notifier notifier;

    @Inject
    private CrazyArchaeologistConfig config;

    @Override
    protected void startUp() throws Exception
    {
        log.info("Crazy Archaeologist Alert started!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("Crazy Archaeologist Alert stopped!");
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE &&
                event.getType() != ChatMessageType.SPAM)
        {
            return;
        }

        String message = event.getMessage();

        if (message.contains(SPECIAL_ATTACK_TEXT))
        {
            if (isCrazyArchaeologistNearby())
            {
                sendAlert();
            }
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if (!config.useAnimationDetection())
        {
            return;
        }

        Actor actor = event.getActor();

        if (!(actor instanceof NPC))
        {
            return;
        }

        NPC npc = (NPC) actor;

        if (npc.getId() == CRAZY_ARCHAEOLOGIST_ID &&
                npc.getAnimation() == SPECIAL_ATTACK_ANIMATION)
        {
            sendAlert();
        }
    }

    private boolean isCrazyArchaeologistNearby()
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return false;
        }

        return client.getNpcs().stream()
                .anyMatch(npc -> npc.getId() == CRAZY_ARCHAEOLOGIST_ID &&
                        npc.getWorldLocation().distanceTo(localPlayer.getWorldLocation()) < 20);
    }

    private void sendAlert()
    {
        if (config.enableNotification())
        {
            notifier.notify("Crazy Archaeologist special attack!");
        }

        if (config.enableSound())
        {
            client.playSoundEffect(SoundEffectID.GE_INCREMENT_PLOP);
        }
    }

    @Provides
    CrazyArchaeologistConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(CrazyArchaeologistConfig.class);
    }
}