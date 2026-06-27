package net.bov.main.Libs;

import net.bov.main.VenturaNPCs;
import net.bov.main.NpcScheduleManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Libs {
    public static String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String NewLine = "\n";
    public String Prefix = "&8[&6VNPC&8] ";
    public String cmdstarter = format("◊ ");
    public String spacer = format(" &8- ");
    public String CommandDivider = format("&8&m---------------------------------------------|>");
    public String NoPermission = format("&8[] &cYou do not have permission to use this command");
    public String console = format("&cYou must be online to run this command");
    public String Version() {return VenturaNPCs.getInstance().getDescription().getVersion();}
    public void send(CommandSender sender, String msg) {
        sender.sendMessage(String.format(msg));
    }
    public static void debugMsg(String msg) {
        Bukkit.getServer().getConsoleSender().sendMessage(msg);
    }

    public static class ChatComponent {
        TextComponent component;

        public String format(String input) {return input.length() == 0 ? "Empty" : ChatColor.translateAlternateColorCodes('&', input);}
        public ChatComponent(String message) {
            this.component = new TextComponent(this.format(message));
        }
        public TextComponent getComponent() {
            return this.component;
        }
        public void addHoverMessage(String message) {this.component.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder(this.format(message))).create()));}
        public void addClickEvent(String command, ClickEvent.Action action) {this.component.setClickEvent(new ClickEvent(action, command));}
    }

    public void FormatHelp(CommandSender sender) {
        ChatComponent header = new ChatComponent("&f&nAvailable Commands:&r &7[] = Required, &7<> = Optional\n\n&7&oHover / Click for Additional Information\n");
        sender.spigot().sendMessage(header.getComponent());
    }

    public void Commands(CommandSender sender) {
        helpLine(sender, "/vnpc &fsettime <time> <id>", "Set a location for a time of day (e.g. 8pm, 06:30, noon).\nStand where you want the NPC, then run it.\nUses your selected NPC, or the id you pass.", "/vnpc settime ");
        helpLine(sender, "/vnpc &fsetday <id>", "Shortcut: set the NPC's location for dawn (6:00am).", "/vnpc setday");
        helpLine(sender, "/vnpc &fsetnight <id>", "Shortcut: set the NPC's location for dusk (7:00pm).", "/vnpc setnight");
        helpLine(sender, "/vnpc &fdeltime <time> <id>", "Remove the entry at a given time.", "/vnpc deltime ");
        helpLine(sender, "/vnpc &finfo <id>", "Show all of an NPC's scheduled times and locations.", "/vnpc info");
        helpLine(sender, "/vnpc &fsend <time> <id>", "Walk the NPC to its location for that time now (testing).", "/vnpc send ");
        helpLine(sender, "/vnpc &fmanage <id>", "Open a clickable editor: walk, move, delete or add\ntimes with buttons.", "/vnpc manage ");
        helpLine(sender, "/vnpc &fclear <id>", "Remove an NPC's whole schedule.", "/vnpc clear");
        helpLine(sender, "/vnpc &flist", "Clickable list of scheduled NPCs - click one to manage it.", "/vnpc list");
        helpLine(sender, "/vnpc &fprune", "Remove schedules for NPCs that no longer exist.", "/vnpc prune");
        helpLine(sender, "/vnpc &flog [on|off]", "Toggle pathfinding logs in the console.", "/vnpc log");
        helpLine(sender, "/vnpc &freload", "Reload npcs.yml from disk.", "/vnpc reload");
    }

    private void helpLine(CommandSender sender, String command, String description, String suggest) {
        ChatComponent msg = new ChatComponent("&6" + this.cmdstarter + "&7" + command);
        msg.addHoverMessage("&6" + command + "\n\n&7" + description);
        msg.addClickEvent(suggest, ClickEvent.Action.SUGGEST_COMMAND);
        sender.spigot().sendMessage(msg.getComponent());
    }

    private TextComponent button(String label, String hover, String command, ClickEvent.Action action) {
        TextComponent b = new TextComponent(format(label));
        b.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(format(hover)).create()));
        b.setClickEvent(new ClickEvent(action, command));
        return b;
    }

    public void ScheduleList(CommandSender sender, Set<Integer> ids) {
        if (ids.isEmpty()) {
            sender.sendMessage(format(Prefix + "&7No NPCs are scheduled yet. Use &e/vnpc settime <time>&7."));
            return;
        }
        TextComponent line = new TextComponent(format(Prefix + "&6Scheduled NPCs &7(" + ids.size() + "): "));
        boolean first = true;
        for (int id : ids) {
            if (!first) {
                line.addExtra(new TextComponent(format("&8, ")));
            }
            first = false;
            line.addExtra(button("&e[" + id + "]",
                    "&6NPC " + id + "\n \n&7Click to manage this NPC's\n&7times and locations.",
                    "/vnpc manage " + id, ClickEvent.Action.RUN_COMMAND));
        }
        sender.spigot().sendMessage(line);
    }

    public void ManageMenu(CommandSender sender, int id, String name, TreeMap<Integer, Location> sched) {
        send(sender, CommandDivider);

        TextComponent title = new TextComponent(format("&6NPC &e" + id + " &7(" + name + ") &6Schedule   "));
        title.addExtra(button("&8[&aSync Location&8]\n",
                "&7Sync this NPC to its needed location",
                "/vnpc send now " + id, ClickEvent.Action.RUN_COMMAND));
        sender.spigot().sendMessage(title);

        if (sched == null || sched.isEmpty()) {
            sender.sendMessage(format("&7  No Set Times"));
        } else {
            for (Map.Entry<Integer, Location> e : sched.entrySet()) {
                int ticks = e.getKey();
                String clock = NpcScheduleManager.ticksToClock(ticks);
                Location l = e.getValue();
                String where = (l == null || l.getWorld() == null) ? "&cunset"
                        : "&f" + l.getWorld().getName() + " &7" + Math.round(l.getX()) + ","
                        + Math.round(l.getY()) + "," + Math.round(l.getZ());
                TextComponent row = new TextComponent(format("&e" + clock + "  &7" + where + "  "));
                row.addExtra(button("&8[&aGo&8] ", "&7Walk the NPC here now.",
                        "/vnpc send " + ticks + " " + id, ClickEvent.Action.RUN_COMMAND));
                row.addExtra(button("&8[&bSet Location&8] ", "&7Move this entry to where\n&7you are standing now.",
                        "/vnpc settime " + ticks + " " + id, ClickEvent.Action.RUN_COMMAND));
                row.addExtra(button("&8[&cX&8]", "&7Delete this entry.",
                        "/vnpc deltime " + ticks + " " + id, ClickEvent.Action.RUN_COMMAND));
                sender.spigot().sendMessage(row);
            }
        }

        TextComponent add = new TextComponent(format("\n&7Add Location: \n"));
        for (String p : new String[]{"6am", "noon", "6pm", "8pm", "12am"}) {
            add.addExtra(button("&8[&a" + p + "&8] ", "&7Add a &e" + p + " &7entry at your\n&7current position.",
                    "/vnpc settime " + p + " " + id, ClickEvent.Action.RUN_COMMAND));
        }
        add.addExtra(button("&8[&eCustom&8] ", "&7Type your own time, e.g. 7:30pm.",
                "/vnpc settime ", ClickEvent.Action.SUGGEST_COMMAND));
        add.addExtra(button("&8[&4Clear All&8]", "&7Remove this NPC's whole schedule.",
                "/vnpc clear " + id, ClickEvent.Action.RUN_COMMAND));
        sender.spigot().sendMessage(add);

        TextComponent foot = new TextComponent(format("&7"));
        foot.addExtra(button("\n&8[&6Refresh&8]", "&7Reopen this menu to see changes.",
                "/vnpc manage " + id, ClickEvent.Action.RUN_COMMAND));
        sender.spigot().sendMessage(foot);

        send(sender, CommandDivider);
    }

    public void PluginInformation(CommandSender sender) {
        TextComponent msg = new TextComponent(format("&a" + this.cmdstarter + "&b[Spigot] "));
        msg.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new Content[]{new Text(format("&6VenturaNPCs Spigot Page \n\n&7Website URL: \n&6" + this.cmdstarter + "&e&nhttps://www.spigotmc.org/resources/venturanpcs-citizens-pathfinding-addon.136543/"))}));
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/venturanpcs-citizens-pathfinding-addon.136543/"));
        TextComponent msg2 = new TextComponent(format("&6[VenturaNPCs] "));
        msg2.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new Content[]{new Text(format("&6VenturaNPCs Help Page \n\n&7Click Command: \n&6" + this.cmdstarter + "&e&n/vnpc"))}));
        msg2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vnpc"));
        TextComponent msg3 = new TextComponent(format("&f[GitHub] "));
        msg3.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new Content[]{new Text(format("&6VenturaNPCs GitHub Page \n\n&7Website URL: \n&6" + this.cmdstarter + "&e&nhttps://github.com/Ash10744/VenturaNPCs"))}));
        msg3.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/Ash10744/VenturaNPCs"));
        msg.addExtra(msg2);
        msg2.addExtra(msg3);;
        sender.spigot().sendMessage(msg);
    }

    public void MCTitle(CommandSender sender) {
        String Version = this.Version();
        Libs.ChatComponent msg = new Libs.ChatComponent("&6VenturaNPCs-" + Version + this.spacer + "&eHelp Menu" + this.spacer + "&6Author: &eAsh10744 ");
        msg.addHoverMessage("&7Additional Supporters: \n&6" + this.cmdstarter + "&e\n&6" + this.cmdstarter + "&e");
        sender.spigot().sendMessage(msg.getComponent());
    }
}