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

		/*
		 * First, place every locked item that is actually present into its preferred
		 * visual cell.
		 */
		for (int visual = 0; visual < SlotLocks.INVENTORY_SIZE; visual++)
		{
			int lockedItemId = itemAt(lockedVisualSlots, visual);
			if (lockedItemId <= 0)
			{
				continue;
			}

			int physical = findMatchingPhysical(inventoryItemIds, usedPhysical, lockedItemId);
			if (physical != SlotLocks.EMPTY)
			{
				assign(physicalToVisual, usedPhysical, usedVisual, physical, visual);
			}
		}

		/*
		 * If a locked item is missing, move a genuinely empty physical inventory
		 * widget into that visual cell. This is what creates a real reserved hole
		 * without leaving another widget underneath it.
		 */
		for (int visual = 0; visual < SlotLocks.INVENTORY_SIZE; visual++)
		{
			if (usedVisual[visual] || itemAt(lockedVisualSlots, visual) <= 0)
			{
				continue;
			}

			int emptyPhysical = findEmptyPhysical(inventoryItemIds, usedPhysical);
			if (emptyPhysical != SlotLocks.EMPTY)
			{
				assign(physicalToVisual, usedPhysical, usedVisual, emptyPhysical, visual);
			}
		}

		/*
		 * Flow every remaining real item through visual cells that are not reserved
		 * by a lock.
		 */
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

			assign(physicalToVisual, usedPhysical, usedVisual, physical, visual);
		}

		/*
		 * A completely full inventory cannot preserve a missing reserved hole because
		 * there is no empty physical widget available. In that edge case, never hide
		 * an item: let overflow occupy any remaining visual cell.
		 */
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

			assign(physicalToVisual, usedPhysical, usedVisual, physical, visual);
		}

		/*
		 * Finally assign every remaining empty physical widget to every remaining
		 * visual cell. This makes the result a true 28-slot permutation: one physical
		 * widget per visual cell, with no overlaps and no unmapped hitboxes.
		 */
		for (int physical = 0; physical < SlotLocks.INVENTORY_SIZE; physical++)
		{
			if (usedPhysical[physical])
			{
				continue;
			}

			int visual = nextUnusedSlot(usedVisual);
			if (visual == SlotLocks.EMPTY)
			{
				break;
			}

			assign(physicalToVisual, usedPhysical, usedVisual, physical, visual);
		}

		return physicalToVisual;
	}

	private static int findMatchingPhysical(int[] inventoryItemIds, boolean[] usedPhysical, int itemId)
	{
		for (int physical = 0; physical < SlotLocks.INVENTORY_SIZE; physical++)
		{
			if (!usedPhysical[physical] && itemAt(inventoryItemIds, physical) == itemId)
			{
				return physical;
			}
		}
		return SlotLocks.EMPTY;
	}

	private static int findEmptyPhysical(int[] inventoryItemIds, boolean[] usedPhysical)
	{
		for (int physical = 0; physical < SlotLocks.INVENTORY_SIZE; physical++)
		{
			if (!usedPhysical[physical] && itemAt(inventoryItemIds, physical) <= 0)
			{
				return physical;
			}
		}
		return SlotLocks.EMPTY;
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

	private static void assign(
		int[] physicalToVisual,
		boolean[] usedPhysical,
		boolean[] usedVisual,
		int physical,
		int visual)
	{
		physicalToVisual[physical] = visual;
		usedPhysical[physical] = true;
		usedVisual[visual] = true;
	}

	private static int itemAt(int[] values, int index)
	{
		return values != null && index >= 0 && index < values.length ? values[index] : SlotLocks.EMPTY;
	}
}
