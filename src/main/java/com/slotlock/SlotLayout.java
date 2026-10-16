package com.slotlock;

import java.util.Arrays;

final class SlotLayout
{
	private SlotLayout()
	{
	}

	static int[] buildPhysicalToVisual(int[] inventoryItemIds, int[] lockedVisualSlots)
	{
		int[] physicalToVisual = new int[SlotLocks.INVENTORY_SIZE];
		Arrays.fill(physicalToVisual, SlotLocks.EMPTY);

		boolean[] usedPhysical = new boolean[SlotLocks.INVENTORY_SIZE];
		boolean[] usedVisual = new boolean[SlotLocks.INVENTORY_SIZE];

		for (int visual = 0; visual < SlotLocks.INVENTORY_SIZE; visual++)
		{
			int lockedItemId = itemAt(lockedVisualSlots, visual);
			if (lockedItemId <= 0)
			{
				continue;
			}

			for (int physical = 0; physical < SlotLocks.INVENTORY_SIZE; physical++)
			{
				if (!usedPhysical[physical] && itemAt(inventoryItemIds, physical) == lockedItemId)
				{
					physicalToVisual[physical] = visual;
					usedPhysical[physical] = true;
					usedVisual[visual] = true;
					break;
				}
			}
		}

		for (int physical = 0; physical < SlotLocks.INVENTORY_SIZE; physical++)
		{
			if (usedPhysical[physical] || itemAt(inventoryItemIds, physical) <= 0)
			{
				continue;
			}

			int visual = nextAvailableFlowSlot(usedVisual, lockedVisualSlots);
			if (visual == SlotLocks.EMPTY)
			{
				break;
			}

			physicalToVisual[physical] = visual;
			usedPhysical[physical] = true;
			usedVisual[visual] = true;
		}

		for (int physical = 0; physical < SlotLocks.INVENTORY_SIZE; physical++)
		{
			if (usedPhysical[physical] || itemAt(inventoryItemIds, physical) <= 0)
			{
				continue;
			}

			int visual = nextUnusedSlot(usedVisual);
			if (visual == SlotLocks.EMPTY)
			{
				break;
			}

			physicalToVisual[physical] = visual;
			usedPhysical[physical] = true;
			usedVisual[visual] = true;
		}

		return physicalToVisual;
	}

	private static int nextAvailableFlowSlot(boolean[] usedVisual, int[] lockedVisualSlots)
	{
		for (int visual = 0; visual < SlotLocks.INVENTORY_SIZE; visual++)
		{
			if (!usedVisual[visual] && itemAt(lockedVisualSlots, visual) <= 0)
			{
				return visual;
			}
		}
		return SlotLocks.EMPTY;
	}

	private static int nextUnusedSlot(boolean[] usedVisual)
	{
		for (int visual = 0; visual < SlotLocks.INVENTORY_SIZE; visual++)
		{
			if (!usedVisual[visual])
			{
				return visual;
			}
		}
		return SlotLocks.EMPTY;
	}

	private static int itemAt(int[] values, int index)
	{
		return values != null && index >= 0 && index < values.length ? values[index] : SlotLocks.EMPTY;
	}
}
