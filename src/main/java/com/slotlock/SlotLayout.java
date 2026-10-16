package com.slotlock;

import java.util.Arrays;

final class SlotLayout
{
	private SlotLayout()
	{
	}

	static int[] buildPhysicalToVisual(int[] inventoryItemIds, int[] lockedVisualSlots)
	{
		return buildPhysicalToVisual(inventoryItemIds, lockedVisualSlots, identityLayout());
	}

	static int[] buildPhysicalToVisual(
		int[] inventoryItemIds,
		int[] lockedVisualSlots,
		int[] previousPhysicalToVisual)
	{
		if (!hasAnyLock(lockedVisualSlots))
		{
			return identityLayout();
		}

		int[] physicalToVisual = new int[SlotLocks.INVENTORY_SIZE];
		Arrays.fill(physicalToVisual, SlotLocks.EMPTY);

		boolean[] usedPhysical = new boolean[SlotLocks.INVENTORY_SIZE];
		boolean[] usedVisual = new boolean[SlotLocks.INVENTORY_SIZE];

		/*
		 * Preserve stable ownership for duplicate locked items. If the physical widget
		 * that previously occupied a locked visual cell still contains the correct
		 * item, keep that exact widget assigned there.
		 */
		for (int visual = 0; visual < SlotLocks.INVENTORY_SIZE; visual++)
		{
			int lockedItemId = itemAt(lockedVisualSlots, visual);
			if (lockedItemId <= 0)
			{
				continue;
			}

			int previousPhysical = findPhysicalForVisual(previousPhysicalToVisual, visual);
			if (previousPhysical != SlotLocks.EMPTY
				&& !usedPhysical[previousPhysical]
				&& itemAt(inventoryItemIds, previousPhysical) == lockedItemId)
			{
				assign(physicalToVisual, usedPhysical, usedVisual, previousPhysical, visual);
			}
		}

		/*
		 * Place any remaining present locked items into their preferred visual cells.
		 */
		for (int visual = 0; visual < SlotLocks.INVENTORY_SIZE; visual++)
		{
			int lockedItemId = itemAt(lockedVisualSlots, visual);
			if (lockedItemId <= 0 || usedVisual[visual])
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
		 * widget into that visual cell. This creates a real reserved hole without
		 * leaving another widget underneath it.
		 */
		for (int visual = 0; visual < SlotLocks.INVENTORY_SIZE; visual++)
		{
			if (usedVisual[visual] || itemAt(lockedVisualSlots, visual) <= 0)
			{
				continue;
			}

			int preferredEmpty = findPhysicalForVisual(previousPhysicalToVisual, visual);
			if (preferredEmpty != SlotLocks.EMPTY
				&& !usedPhysical[preferredEmpty]
				&& itemAt(inventoryItemIds, preferredEmpty) <= 0)
			{
				assign(physicalToVisual, usedPhysical, usedVisual, preferredEmpty, visual);
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
		 * there is no empty physical widget available. Never hide an item: overflow
		 * may occupy an otherwise-reserved visual cell.
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
		 * Assign every remaining empty physical widget to every remaining visual cell.
		 * The result is always a true 28-slot permutation.
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

	private static boolean hasAnyLock(int[] lockedVisualSlots)
	{
		if (lockedVisualSlots == null)
		{
			return false;
		}

		for (int visual = 0; visual < Math.min(lockedVisualSlots.length, SlotLocks.INVENTORY_SIZE); visual++)
		{
			if (lockedVisualSlots[visual] > 0)
			{
				return true;
			}
		}
		return false;
	}

	private static int findPhysicalForVisual(int[] physicalToVisual, int visual)
	{
		if (physicalToVisual == null)
		{
			return SlotLocks.EMPTY;
		}

		for (int physical = 0; physical < Math.min(physicalToVisual.length, SlotLocks.INVENTORY_SIZE); physical++)
		{
			if (physicalToVisual[physical] == visual)
			{
				return physical;
			}
		}
		return SlotLocks.EMPTY;
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

	private static int[] identityLayout()
	{
		int[] layout = new int[SlotLocks.INVENTORY_SIZE];
		for (int slot = 0; slot < layout.length; slot++)
		{
			layout[slot] = slot;
		}
		return layout;
	}
}
