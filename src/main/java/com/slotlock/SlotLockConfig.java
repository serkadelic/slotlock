package com.slotlock;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SlotLockConfig.GROUP)
public interface SlotLockConfig extends Config
{
	String GROUP = "slotlock";

	@ConfigItem(
		keyName = "requireShift",
		name = "Shift for menu options",
		description = "Only show SlotLock inventory menu options while Shift is held"
	)
	default boolean requireShift()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lockedSlots",
		name = "Locked slots",
		description = "Internal SlotLock preset data",
		hidden = true
	)
	default String lockedSlots()
	{
		return "";
	}
}
