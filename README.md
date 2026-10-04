# AHTools

Auction house tools for DonutSMP, as a client-side Fabric mod.

> **Warning.** The AH Seller and the command spammer send commands to the
> server for you. Automation can break DonutSMP's rules, and it can get your
> account muted or banned. You use them at your own risk. The mod shows this
> warning in game and refuses to start either feature until you accept it.
> The sales tracker and the profit HUD only read chat.

## Features

- **AH Seller** lists the items in your hotbar with `/ah sell <price>`, one
  slot at a time, at a fixed price or a random price from a range. Set the
  preset's expected item and it only ever sells that item.
- **Presets** keep a price, a delay and a cost per item for each thing you sell.
- **Sales tracker** reads the server's sale lines and keeps revenue, cost and
  profit for the session, the day and all time, per preset.
- **Profit HUD** keeps session profit, the hourly rate and the sale count on
  screen.
- **Command spammer** repeats a list of commands in order.

The mod is built for DonutSMP's chat lines and does nothing useful elsewhere.

## Supported versions

| Minecraft | Jar |
| --- | --- |
| 1.21.11 | `ahtools-<version>+1.21.11.jar` |
| 26.1, 26.1.1, 26.1.2 | `ahtools-<version>+26.1.2.jar` |
| 26.2 | `ahtools-<version>+26.2.jar` |
| 26.3 | `ahtools-<version>+26.3.jar` |

## Requirements

- Fabric Loader 0.19.0 or newer
- Fabric API
- Mod Menu (optional, adds a config button)

## Using it

Bind the keys in Options, Controls, AHTools. The GUI key opens a draggable
panel with tabs down the left edge: General, AH Seller, Presets, Statistics and
Spammer. Hover any setting or button for a short explanation. Session profit
shows at the top of the screen; move it from General, Reposition HUD.

## Safety

Every automated message from both features passes through one shared gate with
a hard floor of 20 ticks (one second) between sends. The floor is enforced
where the message is sent, not only where it is configured, so no combination
of settings or hand edited files can send faster.

The seller checks the slot again in the same tick it sends, so an item you
move or swap while it waits is never listed by mistake. It only reacts to
server lines, never to player chat or whispers.

The emergency stop hotkey turns both features off and drops any queued
message. Disconnecting, changing server or world, or returning to the title
screen does the same. Nothing resumes on its own when you reconnect. If the
seller stops itself on an error, it tells you in chat with a sound.

## Configuration

Settings live in `.minecraft/config/ahtools.json`. Presets, statistics and the
listing ledger live beside it in `.minecraft/config/ahtools/`: `presets.json`,
`stats.json` and `ledger.json`.

Every file is written to a temporary file first and then moved into place, so a
crash cannot leave it half written. Each one carries a `schemaVersion`. A file
written by a newer version of the mod is read but never overwritten, and a file
that cannot be read is copied to `<name>.corrupt-<time>.json` before anything
replaces it.

## What the numbers mean

Sale lines carry no quantity, so the mod remembers what it listed and matches
each sale back to its listing to recover the count and the cost. Sales made
before the mod was running, and aggregate payouts collected while you were
offline, count revenue only and appear under "(unattributed)". Money figures
are the server's own rounded display values, so every total is approximate in
the same way chat is.

## Languages

English and Polski. The mod follows the game's language setting.

## Building

Each Minecraft version is its own Gradle project. Build from inside its directory:

| Directory | Minecraft | JDK |
| --- | --- | --- |
| `versions/1.21.11` | 1.21.11 | 21 |
| `versions/26.1` | 26.1.x | 25 |
| `versions/26.2` | 26.2 | 25 |
| `versions/26.3` | 26.3 | 25 |

```bash
cd versions/1.21.11 && ./gradlew build
```

The jar lands in `build/libs/`. Files that do not touch Minecraft must be
identical in every version directory; `scripts/check-drift.sh` checks that, and
CI runs it on every push.

## License

MIT.
