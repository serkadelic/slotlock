package com.slotlock;

import java.util.StringJoiner;

final class SlotLocks
{
	static final int INVENTORY_SIZE = 28;
	static final int EMPTY = -1;

	private SlotLocks()
	{
	}

	static int[] parse(String raw)
	{
		int[] locks = emptyLocks();
		if (raw == null || raw.trim().isEmpty())
		{
			return locks;
		}

		String[] entries = raw.split(",");
		for (String entry : entries)
		{
			String[] parts = entry.trim().split(":", 2);
			if (parts.length != 2)
			{
				continue;
			}

			try
			{
				int slot = Integer.parseInt(parts[0]);
				int itemId = Integer.parseInt(parts[1]);
				if (slot >= 0 && slot < INVENTORY_SIZE && itemId > 0)
				{
					locks[slot] = itemId;
				}
			}
			catch (NumberFormatException ignored)
			{
			}
		}

		return locks;
	}

	static String serialize(int[] locks)
	{
		StringJoiner joiner = new StringJoiner(",");
		for (int slot = 0; slot < Math.min(locks.length, INVENTORY_SIZE); slot++)
		{
			if (locks[slot] > 0)
			{
				joiner.add(slot + ":" + locks[slot]);
			}
		}
		return joiner.toString();
	}

	static int[] emptyLocks()
	{
		int[] locks = new int[INVENTORY_SIZE];
		for (int i = 0; i < locks.length; i++)
		{
			locks[i] = EMPTY;
		}
		return locks;
	}
}
