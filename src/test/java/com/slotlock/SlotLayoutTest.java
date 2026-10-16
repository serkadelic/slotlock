package com.slotlock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SlotLayoutTest
{
	@Test
	public void lockedItemsClaimPreferredCellsAndOtherItemsFlow()
	{
		int[] inventory = emptyInventory();
		inventory[0] = 100;
		inventory[1] = 200;
		inventory[2] = 300;

		int[] locks = SlotLocks.emptyLocks();
		locks[0] = 100;
		locks[4] = 200;

		int[] layout = SlotLayout.buildPhysicalToVisual(inventory, locks);

		assertEquals(0, layout[0]);
		assertEquals(4, layout[1]);
		assertEquals(1, layout[2]);
		assertPermutation(layout);
	}

	@Test
	public void missingLockedItemLeavesItsCellReservedWithEmptyPhysicalWidget()
	{
		int[] inventory = emptyInventory();
		inventory[0] = 200;
		inventory[1] = 300;

		int[] locks = SlotLocks.emptyLocks();
		locks[0] = 100;

		int[] layout = SlotLayout.buildPhysicalToVisual(inventory, locks);

		assertEquals(1, layout[0]);
		assertEquals(2, layout[1]);
		assertEquals(0, layout[2]);
		assertPermutation(layout);
	}

	@Test
	public void duplicateLockedItemsCanOccupyMultiplePreferredCells()
	{
		int[] inventory = emptyInventory();
		inventory[0] = 100;
		inventory[1] = 100;
		inventory[2] = 200;

		int[] locks = SlotLocks.emptyLocks();
		locks[0] = 100;
		locks[4] = 100;

		int[] layout = SlotLayout.buildPhysicalToVisual(inventory, locks);

		assertEquals(0, layout[0]);
		assertEquals(4, layout[1]);
		assertEquals(1, layout[2]);
		assertPermutation(layout);
	}

	@Test
	public void survivingDuplicateStaysInItsPreviousLockedCell()
	{
		int[] inventory = emptyInventory();
		inventory[5] = 100;
		inventory[9] = 100;

		int[] locks = SlotLocks.emptyLocks();
		locks[0] = 100;
		locks[4] = 100;

		int[] previous = SlotLayout.buildPhysicalToVisual(inventory, locks);
		assertEquals(0, previous[5]);
		assertEquals(4, previous[9]);

		// The first copy is consumed. The surviving physical widget should remain
		// associated with visual slot 5 (zero-based visual index 4), not jump to slot 1.
		inventory[5] = SlotLocks.EMPTY;
		int[] updated = SlotLayout.buildPhysicalToVisual(inventory, locks, previous);

		assertEquals(4, updated[9]);
		assertEquals(0, updated[5]);
		assertPermutation(updated);
	}

	@Test
	public void allTwentyEightPhysicalWidgetsAlwaysMapToUniqueVisualCells()
	{
		int[] inventory = emptyInventory();
		inventory[0] = 100;
		inventory[1] = 200;
		inventory[5] = 300;
		inventory[19] = 400;

		int[] locks = SlotLocks.emptyLocks();
		locks[0] = 999;
		locks[4] = 300;
		locks[8] = 888;
		locks[12] = 100;

		int[] layout = SlotLayout.buildPhysicalToVisual(inventory, locks);

		assertPermutation(layout);
	}

	@Test
	public void fullInventoryFallsBackWithoutHidingAnyItem()
	{
		int[] inventory = new int[SlotLocks.INVENTORY_SIZE];
		for (int i = 0; i < inventory.length; i++)
		{
			inventory[i] = 1000 + i;
		}

		int[] locks = SlotLocks.emptyLocks();
		locks[0] = 999;

		int[] layout = SlotLayout.buildPhysicalToVisual(inventory, locks);

		assertPermutation(layout);
	}

	private static void assertPermutation(int[] layout)
	{
		assertEquals(SlotLocks.INVENTORY_SIZE, layout.length);

		boolean[] seen = new boolean[SlotLocks.INVENTORY_SIZE];
		for (int visual : layout)
		{
			assertTrue(visual >= 0 && visual < SlotLocks.INVENTORY_SIZE);
			assertTrue("visual slot mapped more than once: " + visual, !seen[visual]);
			seen[visual] = true;
		}

		for (int visual = 0; visual < seen.length; visual++)
		{
			assertTrue("visual slot not mapped: " + visual, seen[visual]);
		}
	}

	private static int[] emptyInventory()
	{
		int[] inventory = new int[SlotLocks.INVENTORY_SIZE];
		for (int i = 0; i < inventory.length; i++)
		{
			inventory[i] = SlotLocks.EMPTY;
		}
		return inventory;
	}
}
