<img width="800" height="345" alt="image" src="https://github.com/user-attachments/assets/c9fbd36c-a7f7-4889-8510-e8f088e82a04" />


# VenturaNPCs

Give your [Citizens](https://www.spigotmc.org/resources/citizens.13811/) NPCs a daily routine. Set where an NPC should be at different times of day, and it walks there on its own as time passes — opening the shop in the morning, heading home at night, wherever you want them.

---

## Requirements

- **Spigot or Paper 1.16 or newer**
- The **[Citizens](https://www.spigotmc.org/resources/citizens.13811/)** plugin (install the build made for your Minecraft version)

Citizens is required — VenturaNPCs won't start without it.

---

## Installation

1. Install Citizens on your server.
2. Drop `VenturaNPCs.jar` into your `plugins` folder.
3. Restart the server.

---

## Getting started

1. Make an NPC with Citizens: `/npc create Greeter`
2. Select it — left-click it, or use `/npc select`
3. Stand where it should be in the morning and run `/vnpc setday`
4. Stand where it should be at night and run `/vnpc setnight`
5. Manage the schedule and times easier with `/npc manage`

---

## Setting times

Stand where you want the NPC to be, then run `/vnpc settime <time>`. You can write the time however feels natural:

- `8pm`, `7:30am`
- `20:00`, `06:30`
- `noon`, `midnight`, `dawn`, `dusk`

For example, stand at the market and run `/vnpc settime noon`, and the NPC will head there every day at midday.

---

## Editing an NPC

Run `/vnpc list` to see all your scheduled NPCs, then click one to open the editor.

---

## Commands

| Command | What it does |
|---------|--------------|
| `/vnpc manage` | Open a button menu to set and edit an NPC's times — the easiest way |
| `/vnpc list` | See and edit all your scheduled NPCs |
| `/vnpc setday` | Set where the NPC stands in the morning |
| `/vnpc setnight` | Set where the NPC stands at night |
| `/vnpc settime <time>` | Set where the NPC stands at a specific time |
| `/vnpc deltime <time>` | Remove a time |
| `/vnpc info` | List an NPC's times and locations |
| `/vnpc send <time\|now>` | Make the NPC walk to a spot right now |
| `/vnpc clear` | Clear an NPC's whole routine |
| `/vnpc help` | Show the in-game help menu |

Commands act on the NPC you have selected, or you can add its id on the end, for example `/vnpc settime 8pm 17`.

---

## Good to know

NPCs walk using Citizens' pathfinding, so there needs to be a route they can actually walk between their locations. If two spots are cut off from each other — a cliff, a wall, no path — the NPC will simply teleport so it still ends up in the right place. Adding stairs or a clear path between locations keeps them walking instead.

When an NPC is deleted, its routine is cleaned up automatically.


---
<img width="700" height="452" alt="image" src="https://github.com/user-attachments/assets/4e7292f1-8021-42d7-8d94-c75599bef3fe" />


Need Support, Assistance or Help?
Join the discord below or submit a report at my github!

https://discord.gg/rRAXRbaJxz OR https://github.com/Ash10744/VenturaNPCs/issues


---

## Author

Created by Ash10744.
