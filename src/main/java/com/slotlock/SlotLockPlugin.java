package com.slotlock;

import com.google.inject.Provides;
import java.util.Arrays;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "SlotLock",
	description = "Lock inventory items to preferred visual slots while other items flow around them",
	tags = {"inventory", "layout", "slots", "toa", "raid"},
	enabledByDefault = false
)
public class SlotLockPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(SlotLockPlugin.class);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private SlotLockConfig config;

	private int[] physicalToVisual = identityLayout();

	@Provides
	SlotLockConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SlotLockConfig.class);
	}

	@Override
	protected void startUp()
	{
		clientThread.invokeLater(this::refreshLayout);
		log.debug("SlotLock started");
	}

	@Override
	protected void shutDown()
	{
		clientThread.invoke(this::clearForcedPositions);
		physicalToVisual = identityLayout();
		log.debug("SlotLock stopped");
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INV)
		{
			refreshLayout();
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() != ScriptID.INVENTORY_DRAWITEM)
		{
			return;
		}

		Widget itemWidget = client.getScriptActiveWidget();
		if (itemWidget != null && itemWidget.getParentId() == InterfaceID.Inventory.ITEMS)
		{
			positionWidget(itemWidget);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!SlotLockConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("resetLocks".equals(event.getKey()) && Boolean.parseBoolean(event.getNewValue()))
		{
			configManager.unsetConfiguration(SlotLockConfig.GROUP, "lockedSlots");
			configManager.setConfiguration(SlotLockConfig.GROUP, "resetLocks", false);
		}

		clientThread.invokeLater(this::refreshLayout);
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (config.requireShift() && !client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}

		MenuEntry[] entries = event.getMenuEntries();
		for (int index = entries.length - 1; index >= 0; index--)
		{
			MenuEntry entry = entries[index];
			Widget widget = entry.getWidget();

			if (widget == null
				|| widget.getParentId() != InterfaceID.Inventory.ITEMS
				|| widget.getItemId() <= 0
				|| !"Examine".equals(entry.getOption()))
			{
				continue;
			}

			int physicalSlot = widget.getIndex();
			if (physicalSlot < 0 || physicalSlot >= SlotLocks.INVENTORY_SIZE)
			{
				continue;
			}

			int visualSlot = visualSlotForPhysical(physicalSlot);
			int itemId = widget.getItemId();
			int[] locks = SlotLocks.parse(config.lockedSlots());
			boolean lockedHere = locks[visualSlot] == itemId;

			client.createMenuEntry(index)
				.setOption(lockedHere ? "Unlock SlotLock slot" : "Lock to SlotLock slot")
				.setTarget(entry.getTarget() + " <col=9f9f9f>(slot " + (visualSlot + 1) + ")</col>")
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
					int[] updatedLocks = SlotLocks.parse(config.lockedSlots());
					updatedLocks[visualSlot] = lockedHere ? SlotLocks.EMPTY : itemId;
					configManager.setConfiguration(
						SlotLockConfig.GROUP,
						"lockedSlots",
						SlotLocks.serialize(updatedLocks)
					);
					refreshLayout();
				});

			client.createMenuEntry(index)
				.setOption("Clear all SlotLock locks")
				.setTarget("")
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
					configManager.unsetConfiguration(SlotLockConfig.GROUP, "lockedSlots");
					refreshLayout();
				});

			break;
		}
	}

	private void refreshLayout()
	{
		int[] inventoryIds = currentInventoryIds();
		int[] locks = SlotLocks.parse(config.lockedSlots());
		physicalToVisual = SlotLayout.buildPhysicalToVisual(inventoryIds, locks, physicalToVisual);
		applyLayoutToCurrentWidgets();
	}

	private int[] currentInventoryIds()
	{
		int[] ids = new int[SlotLocks.INVENTORY_SIZE];
		Arrays.fill(ids, SlotLocks.EMPTY);

		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return ids;
		}

		Item[] items = inventory.getItems();
		for (int slot = 0; slot < Math.min(items.length, ids.length); slot++)
		{
			if (items[slot] != null)
			{
				ids[slot] = items[slot].getId();
			}
		}

		return ids;
	}

	private void applyLayoutToCurrentWidgets()
	{
		Widget inventoryWidget = client.getWidget(InterfaceID.Inventory.ITEMS);
		if (inventoryWidget == null)
		{
			return;
		}

		for (Widget child : inventoryWidget.getDynamicChildren())
		{
			if (child != null)
			{
				positionWidget(child);
			}
		}
	}

	private void positionWidget(Widget widget)
	{
		int physicalSlot = widget.getIndex();
		if (physicalSlot < 0 || physicalSlot >= SlotLocks.INVENTORY_SIZE)
		{
			return;
		}

		int visualSlot = visualSlotForPhysical(physicalSlot);
		if (visualSlot < 0)
		{
			widget.setForcedPosition(-1, -1);
			return;
		}

		if (visualSlot == physicalSlot)
		{
			widget.setForcedPosition(-1, -1);
			return;
		}

		Widget parent = widget.getParent();
		Widget targetSlotWidget = parent != null ? parent.getChild(visualSlot) : null;
		if (targetSlotWidget == null)
		{
			// Never guess coordinates. If RuneLite does not expose the target slot,
			// leave the real widget in its normal position.
			widget.setForcedPosition(-1, -1);
			return;
		}

		widget.setForcedPosition(
			targetSlotWidget.getOriginalX(),
			targetSlotWidget.getOriginalY()
		);
	}

	private int visualSlotForPhysical(int physicalSlot)
	{
		if (physicalSlot < 0 || physicalSlot >= physicalToVisual.length)
		{
			return physicalSlot;
		}

		int visual = physicalToVisual[physicalSlot];
		return visual >= 0 ? visual : physicalSlot;
	}

	private void clearForcedPositions()
	{
		Widget inventoryWidget = client.getWidget(InterfaceID.Inventory.ITEMS);
		if (inventoryWidget == null)
		{
			return;
		}

		for (Widget child : inventoryWidget.getDynamicChildren())
		{
			if (child != null)
			{
				child.setForcedPosition(-1, -1);
			}
		}
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
