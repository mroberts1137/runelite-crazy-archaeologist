package com.crazyarchaeologist;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CrazyArchaeologistPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CrazyArchaeologistPlugin.class);
		RuneLite.main(args);
	}
}