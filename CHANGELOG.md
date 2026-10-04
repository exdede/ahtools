# Changelog

## 1.0.0

First public release.

- Builds for Minecraft 1.21.11, 26.1.x, 26.2 and 26.3.
- Hover tooltips on every setting and button, in English and Polish.
- Ban risk warning before the seller or the spammer first runs. Neither starts until it is accepted, from the GUI or from a key binding.
- Polish translation.
- Settings, presets, statistics and the ledger are written atomically and carry a schemaVersion. A file written by a newer version is never overwritten, and an unreadable one is copied aside before it is replaced.
- The seller only sells slots holding the preset's expected item, and checks the slot again in the tick it sends, so a scroll of the mouse wheel can no longer list the wrong item. A preset with no expected item shows a red warning.
- Whispers and player chat can no longer stop, wake or fool the seller, and can no longer add fake sales to the statistics.
- The seller understands "You cannot sell air." and the server's command cooldown instead of timing out and stopping.
- Long preset delays no longer send a duplicate command, and nothing is sent after the seller stops.
- "You earned $N from auction got delivered N items" now counts as revenue.
- A preset can no longer be overwritten by adding or renaming another one to the same name.
- Automation stops on a world change (server switch, restart limbo), and a seller that stops on an error says so in chat.
- Expected item ignores case and surrounding spaces.
