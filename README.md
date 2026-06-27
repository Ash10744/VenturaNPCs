# VenturaNPCs

A lightweight Spigot/Paper plugin that gives [Citizens](https://www.spigotmc.org/resources/citizens.13811/) NPCs a daily routine. Assign an NPC a set of time-of-day locations, and it will walk between them on its own as the in-game day progresses — at home in the morning, at the shop by midday, back home at night.

Pathing uses Citizens' own navigator, so movement is dynamic and obeys the world geometry. Where a spot genuinely can't be walked to, the NPC teleports as a fallback so its routine never silently breaks.

---

## Features

- Per-NPC schedule of any number of `time -> location` points
- NPCs walk to the correct location automatically as world time changes, and stay there until the next scheduled time
- Friendly time input: `8pm`, `06:30`, `noon`, `midnight`, `dawn`, `dusk`, or raw ticks
- Clickable in-chat NPC list and a full button-driven editor (`/vnpc manage`)
- Teleport fallback when no walkable path exists (large drops, walls, cross-world)
- Automatic cleanup of schedules when an NPC is deleted, plus a manual prune
- Toggleable console logging for debugging pathfinding
- Flat-file storage (`npcs.yml`), no database required

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Server      | Spigot or Paper 1.16 - latest |
| Java        | 8 or newer |
| Citizens    | A build matching your server version |

Citizens is a hard dependency — VenturaNPCs will not enable without it.

VenturaNPCs is built against the 1.16 API and compiled to Java 8 bytecode, which loads on every later server (including Java 21 ones), so a single jar covers the whole range. It only uses stable Bukkit and Citizens API with no version-specific code. The one thing that changes per server version is Citizens itself — install the Citizens build made for your Minecraft version.

---

## Installation

1. Install [Citizens](https://www.spigotmc.org/resources/citizens.13811/) on your server.
2. Drop `VenturaNPCs.jar` into your `plugins/` folder.
3. Restart the server. A `plugins/VenturaNPCs/npcs.yml` file is created automatically.

---

## Quick start

1. Create an NPC with Citizens: `/npc create Greeter`
2. Stand where the NPC should be in the morning and run `/vnpc setday`
3. Walk to where it should be at night and run `/vnpc setnight`
4. Add any extra points you like, e.g. stand at the market and run `/vnpc settime noon`

The NPC now walks between those points as the day cycles. To test instantly without waiting, use `/vnpc send <time>` or the `[Sync Location]` button in `/vnpc manage`.

Commands act on your selected Citizens NPC (left-click it, or `/npc select`). You can also target one directly by id, e.g. `/vnpc settime 8pm 17`.

---

## Commands

All commands are under `/vnpc` (alias: `/venturanpc`).

| Command | Description |
|---------|-------------|
| `/vnpc settime <time> [id]` | Set the NPC's location for a time of day to where you are standing |
| `/vnpc setday [id]` | Shortcut for `settime` at dawn (6:00am) |
| `/vnpc setnight [id]` | Shortcut for `settime` at dusk (7:00pm) |
| `/vnpc deltime <time> [id]` | Remove the entry at a given time |
| `/vnpc info [id]` | List all of an NPC's scheduled times and locations |
| `/vnpc send <time\|now> [id]` | Walk the NPC to its location for that time right now |
| `/vnpc manage [id]` | Open the clickable editor for an NPC |
| `/vnpc list` | Clickable list of all scheduled NPCs |
| `/vnpc clear [id]` | Remove an NPC's entire schedule |
| `/vnpc prune` | Remove schedules for NPCs that no longer exist |
| `/vnpc log [on\|off]` | Toggle pathfinding logs in the console |
| `/vnpc reload` | Reload `npcs.yml` from disk |
| `/vnpc help` | Show the in-game help menu |

---

## Time formats

Anywhere a `<time>` is expected, you can use:

| Input | Meaning |
|-------|---------|
| `8pm`, `7:30am` | 12-hour clock |
| `20:00`, `06:30` | 24-hour clock |
| `dawn`, `noon`, `dusk`, `night`, `midnight` | Named times |
| `0` - `23999` | Raw Minecraft ticks |

Minecraft time runs on a 24,000-tick day where tick 0 is 6:00am, 6000 is noon, 12000 is 6:00pm and 18000 is midnight. So `8pm` is stored as tick 14000.

---

## The manage menu

`/vnpc manage <id>` (or clicking an NPC in `/vnpc list`) opens an in-chat editor:

- A `[Sync Location]` button that sends the NPC to wherever it should currently be
- One row per scheduled time showing the location, with `[Go]` (walk there now), `[Set Location]` (move that entry to your position), and `[X]` (delete)
- An add row with quick presets, a `[Custom]` button to type any time, and `[Clear All]`
- A `[Refresh]` button to redraw the menu after an edit

The buttons simply run the normal commands, so anything the menu does can also be typed by hand.

---

## How scheduling works

Each NPC holds a sorted list of `time -> location` entries. A task checks the world time every few seconds and moves the NPC to the **most recent** scheduled time that has passed, wrapping around midnight. With entries at 6:00am, noon and 8:00pm, the NPC is at its 8:00pm spot from 8:00pm right through the night until 6:00am.

Movement uses Citizens' A* pathfinder with an extended range so the NPC can find routes to spots farther away. If no walkable path exists within a few seconds — for example a sheer cliff, a blocked route, or a different world — the NPC teleports to the target instead, so its routine stays correct even where walking is impossible.

For NPCs to walk between two points, those points must be connected by ground the pathfinder can traverse. Large vertical gaps need stairs or a ramp; otherwise the teleport fallback takes over.

---

## Storage format

Schedules are saved to `plugins/VenturaNPCs/npcs.yml`, keyed by Citizens NPC id and by tick:

```yaml
npcs:
  17:
    schedule:
      0:
        world: world
        x: 315.5
        y: 80.0
        z: 801.5
        yaw: 90.0
        pitch: 0.0
      14000:
        world: world
        x: 290.5
        y: 55.0
        z: 755.5
        yaw: -90.0
        pitch: 0.0
```

This file is rewritten automatically whenever a schedule changes. Older `day`/`night` entries from earlier versions are migrated into the `schedule` format on load. Avoid hand-editing the file while the server is running, as in-game changes will overwrite it.

---

## Cleanup

Stale entries are handled in two ways:

- When an NPC is removed in Citizens, its schedule is deleted from `npcs.yml` automatically.
- `/vnpc prune` sweeps out any schedules whose NPC no longer exists — useful for tidying a file that predates the automatic cleanup.

---

## Permissions

| Node | Default | Description |
|------|---------|-------------|
| `venturaNPCs.use` | op | Access to all `/vnpc` commands |

---

## Building from source

VenturaNPCs is built with Maven and compiled to Java 8 bytecode for maximum server compatibility.

```bash
mvn clean package
```

Build with a modern JDK (e.g. JDK 21). The compiler targets Java 8 output, but it still needs to read the Citizens dependency, which ships as newer bytecode — an older JDK 8 toolchain cannot read it. The Java 8 *target* only affects the jar that is produced, which is what makes it load on everything from a 1.16 (Java 8) server to a 1.21 (Java 21) server.

The plugin compiles against the 1.16.5 API so it cannot accidentally use anything newer than 1.16 provides:

```xml
<dependency>
    <groupId>org.spigotmc</groupId>
    <artifactId>spigot-api</artifactId>
    <version>1.16.5-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

The Citizens dependency is resolved from the Citizens Maven repository and used at `provided` scope (it is never bundled into the jar):

```xml
<repository>
    <id>citizens-repo</id>
    <url>https://maven.citizensnpcs.co/repo</url>
</repository>

<dependency>
    <groupId>net.citizensnpcs</groupId>
    <artifactId>citizens-main</artifactId>
    <version>2.0.35-SNAPSHOT</version>
    <type>jar</type>
    <scope>provided</scope>
    <exclusions>
        <exclusion>
            <groupId>*</groupId>
            <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

Set the `citizens-main` version to one available in the Citizens repository. Only the long-standing Citizens API is used, so the build version does not need to match every server you deploy to — but each server still needs the Citizens build made for its own Minecraft version. The compiled jar appears in `target/`.

---

## License

See the LICENSE file in this repository.

---

## Author

Created by Ash10744.
