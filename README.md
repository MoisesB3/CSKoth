# CSKoth – King of the Hill

Hold a zone **alone** long enough to capture it, and win points and rewards. KOTHs can be started by hand, run on a daily schedule, or stay on all the time.

**Version 2.0.0.** Runs on Spigot, Paper, Purpur and Arclight, Minecraft **1.20.1 up to the newest 1.21.x** (Java 17 or 21).
**Requires [CopeCore](../CopeCore)** (API 2 or newer). PlaceholderAPI is optional.

## How a capture works

- **One player alone in the zone** captures it. Their time counts up every second.
- **Two or more players** contest it. The time stops but isn't lost.
- **Someone else steps in alone** after the capturer leaves: they start from zero.
- **Nobody in the zone:** the capture is lost (`capture.reset-when-empty`).
- **Spectators and dead players** don't count.
- **When the time is reached,** the player gets the KOTH's points and rewards, plus the global `rewards` from config.yml.

Messages and a **boss bar** go to players within `broadcast-radius` blocks.

### When a KOTH runs
| Started by | Ends |
|---|---|
| `/cskoth start <koth> [minutes]` or the menu | At the first capture (`capture.end-manual-on-capture`), after the minutes if given, or with `/cskoth stop` |
| **Schedule**: every day at its start time (`timezone` in config.yml) | At the first capture, or after its duration |
| **Always on** | Pauses for the respawn delay after each capture, then starts again |

## Setting up

1. Optional: select the zone with the CopeCore wand (`/copecore wand`).
2. Run `/cskoth create <name>`. Without a wand selection you get a box around you (`default-size`, 11×5×11 by default).
3. Run `/cskoth` to open the menu, then **KOTH list → your KOTH**:
   - **Start / Stop**
   - **Settings:** capture time and points.
     - Left click raises the number, right click lowers it, shift-click changes it ×10, and Q lets you type a value.
   - **Rewards:**
     - **Item rewards:** drag the items in.
     - **Command rewards:** add commands in chat, right-click one to remove it. `%player%` and `%koth%` are filled in.
   - **Schedule:** on/off, start time (`18:30`), duration, always on, respawn delay.
   - **Zone:** grow or shrink it (shift = 5 blocks), set it again with the wand or by typing `here` at two corners, and show its outline.
   - **Teleport** to the zone, or **Delete** it (asks first).

## Commands

| Command | Permission |
|---|---|
| `/cskoth` (`/koth`) – opens the menu for admins, the help page for everyone else | – |
| `/cskoth list` – click a coordinate to see the world | cskoth.use |
| `/cskoth next` | cskoth.use |
| `/cskoth points [koth]` – points per player, or your own total plus the top 10 | cskoth.use |
| `/cskoth top` **(new)** | cskoth.use |
| `/cskoth menu` | cskoth.admin |
| `/cskoth create <name>` | cskoth.admin |
| `/cskoth setzone <koth>` **(new)** | cskoth.admin |
| `/cskoth start <koth> [minutes]` (the console works too) | cskoth.admin |
| `/cskoth stop <koth>` | cskoth.admin |
| `/cskoth delete <koth>` | cskoth.admin |
| `/cskoth reload` **(new)** | cskoth.admin |

- `cskoth.use` is given to everyone by default.
- `cskoth.admin` is op by default and includes `cskoth.use`.

## Placeholders

Every placeholder works as `%cskoth_<key>%` (the 1.x names, which need PlaceholderAPI) **and** as `%cope_koth_<key>%` (through CopeCore).

| Key | Value |
|---|---|
| `captures` / `wins`, `contests`, `points` **(new)**, `rank`, `topwins`, `player_in_koth` | The player's stats |
| `active`, `total_active`, `total_koths` | Running KOTHs |
| `next`, `next_time`, `next_countdown` | The next scheduled KOTH |
| `top_<1-10>_name`, `top_<1-10>_captures` **(new)** | Leaderboard |
| `points_<koth>`, `captime_<koth>`, `status_<koth>`, `last_winner_<koth>`, `player_distance_<koth>` | Per KOTH |
| `capturer_<koth>`, `progress_<koth>`, `remaining_<koth>`, `capture_time_left_<koth>`, `duration_left_<koth>` | The current capture |
| `contested_<koth>`, `contested_players_<koth>`, `coordinates_<koth>` | Contest and location |

## Files (`plugins/CSKoth/`)

| File | |
|---|---|
| `config.yml` | Settings. Missing ones are added automatically. |
| `messages.yml` | Every text. MiniMessage or `&` codes. `%koth%`, `{koth}` and `<koth>` all work. |
| `koths.yml` | KOTHs (zone, times, schedule, rewards). Best edited with the menu. |
| `stats.yml` | Captures, contests and points per player, plus points per KOTH. |

## Building
```
cd CopeCore && mvn clean install
cd CSKoth   && mvn clean package     # -> target/CSKoth-2.0.0.jar
```

## Upgrading from 1.x

Back up `plugins/CSKoth`, replace the jar, add CopeCore, and start the server.

| 1.x file | What happens to it |
|---|---|
| `config.yml` | Kept as it is. Same setting names, and new settings are added. |
| `koths.yml` | Read as it is. The corners (`x1..z2`) are rewritten as a zone (`area`) the first time it's saved. |
| `stats.yml` | Read as it is. |
| `messages.yml` | Renamed to `messages.yml.old`. Texts you had changed (codes like `E0004`) are copied to the new keys. |
| `guide.yml`, `placeholders_guide.yml` | No longer used (this README replaces them). You can delete them. |

### Fixed from 1.x
- **The plugin works again.** The last 1.x source never loaded KOTHs, never registered its command, events or schedule, and didn't compile.
- **The menu is admin-only.** It used to open for every player, with create and delete buttons.
- **Points are saved.** They were kept in memory only, so they reset on every restart.
- **Always-on KOTHs now pause for their respawn delay.** They were never stopped, so the delay did nothing.
- **Item rewards that don't fit are dropped at the winner's feet** instead of being lost.
- **Chat setup is thread-safe.** Answers used to edit KOTHs and save files from the chat thread.
- **No crash without PlaceholderAPI.**
- **Menus work no matter what you call them.** They used to be recognised by title, so renaming one broke it, and a chest named "KOTH List" acted like the menu.
- **`/cskoth start` works from the console.**
