package com.slotlock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
	}

	@Test
	public void missingLockedItemLeavesItsCellReserved()
	{
		int[] inventory = emptyInventory();
		inventory[0] = 200;
		inventory[1] = 300;

		int[] locks = SlotLocks.emptyLocks();
		locks[0] = 100;

		int[] layout = SlotLayout.buildPhysicalToVisual(inventory, locks);

		assertEquals(1, layout[0]);
		assertEquals(2, layout[1]);
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
