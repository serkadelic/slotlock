# SlotLock

SlotLock is a RuneLite plugin prototype for keeping important inventory items in consistent visual positions while allowing unlocked items to flow around reserved slots.

## Prototype behaviour

- Shift + right-click an inventory item and choose **Lock to SlotLock slot**.
- The item is assigned to the visual slot where it currently appears.
- Locked slots remain reserved when their assigned item disappears.
- Unlocked items flow through the remaining visual slots.
- Shift + right-click a locked item and choose **Unlock SlotLock slot** to release it.
- SlotLock only changes client-side widget positioning. It does not click, drag, use, bank, drop, equip, or otherwise interact with items for the player.

## Current scope

The first proof of concept matches exact item IDs. Dose/charge families such as Ambrosia, Nectar, Tears, brews and restores will be added after the visual-remapping behaviour is validated in the RuneLite development client.

## Development

SlotLock follows the standard RuneLite external-plugin structure and targets Java 11. Open the project in IntelliJ IDEA, import the Gradle project, and run the `run` Gradle task to launch a development RuneLite client.
