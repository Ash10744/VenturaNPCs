package net.bov.main.Commands;

import net.bov.main.NpcScheduleManager;
import net.bov.main.VenturaNPCs;
import net.bov.main.Libs.Libs;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRemoveEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MainCommand implements Listener, CommandExecutor, TabCompleter {
    private static final Libs Libs = new Libs();

    private NpcScheduleManager mgr() {
        return VenturaNPCs.getInstance().getScheduleManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            HelpCommand(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help":
                HelpCommand(sender);
                return true;

            case "settime": {
                Player player = requirePlayer(sender);
                if (player == null) return true;
                if (args.length < 2) {
                    sender.sendMessage(Libs.format(Libs.Prefix + "&cUsage: /vnpc settime <time> [id] &7- e.g. 8pm, 06:30, noon"));
                    return true;
                }
                int ticks = NpcScheduleManager.parseTime(args[1]);
                if (ticks < 0) return invalidTime(sender, args[1]);
                NPC npc = resolveNpc(sender, args, 2);
                if (npc == null) return missingNpc(sender, "settime " + args[1]);
                mgr().setEntry(npc.getId(), ticks, player.getLocation());
                player.sendMessage(Libs.format(Libs.Prefix + "&aNPC &e" + npc.getId() + " &7(" + npc.getName()
                        + ") &awill be at your position at &e" + NpcScheduleManager.ticksToClock(ticks) + "&a."));
                return true;
            }

            case "setday":
                return setPreset(sender, args, 0);

            case "setnight":
                return setPreset(sender, args, 13000);

            case "deltime": {
                if (args.length < 2) {
                    sender.sendMessage(Libs.format(Libs.Prefix + "&cUsage: /vnpc deltime <time> [id]"));
                    return true;
                }
                int ticks = NpcScheduleManager.parseTime(args[1]);
                if (ticks < 0) return invalidTime(sender, args[1]);
                NPC npc = resolveNpc(sender, args, 2);
                if (npc == null) return missingNpc(sender, "deltime " + args[1]);
                boolean removed = mgr().removeEntry(npc.getId(), ticks);
                sender.sendMessage(Libs.format(Libs.Prefix + (removed
                        ? "&aRemoved the &e" + NpcScheduleManager.ticksToClock(ticks) + " &aentry for NPC &e" + npc.getId() + "&a."
                        : "&cNPC &e" + npc.getId() + " &chas no entry at &e" + NpcScheduleManager.ticksToClock(ticks) + "&c.")));
                return true;
            }

            case "info": {
                NPC npc = resolveNpc(sender, args, 1);
                if (npc == null) return missingNpc(sender, "info");
                TreeMap<Integer, Location> sched = mgr().getSchedule(npc.getId());
                sender.sendMessage(Libs.format("&6NPC &e" + npc.getId() + " &7(" + npc.getName() + ") &6schedule:"));
                if (sched == null || sched.isEmpty()) {
                    sender.sendMessage(Libs.format("&7  (no times set - use &e/vnpc settime <time>&7)"));
                    return true;
                }
                for (Map.Entry<Integer, Location> e : sched.entrySet()) {
                    sender.sendMessage(Libs.format("&7  &e" + NpcScheduleManager.ticksToClock(e.getKey()) + " &7-> " + fmt(e.getValue())));
                }
                return true;
            }

            case "send": {
                if (args.length < 2) {
                    sender.sendMessage(Libs.format(Libs.Prefix + "&cUsage: /vnpc send <time|now> [id]"));
                    return true;
                }
                NPC npc = resolveNpc(sender, args, 2);
                if (npc == null) return missingNpc(sender, "send " + args[1]);
                int ticks;
                if (args[1].equalsIgnoreCase("now")) {
                    ticks = mgr().getCurrentTicks(npc.getId());
                } else {
                    ticks = NpcScheduleManager.parseTime(args[1]);
                    if (ticks < 0) return invalidTime(sender, args[1]);
                }
                Location target = mgr().getActiveLocation(npc.getId(), ticks);
                if (target == null) {
                    sender.sendMessage(Libs.format(Libs.Prefix + "&cNPC &e" + npc.getId() + " &chas no schedule set."));
                    return true;
                }
                boolean ok = mgr().pathTo(npc.getId(), target);
                sender.sendMessage(Libs.format(Libs.Prefix + (ok
                        ? "&aSending NPC &e" + npc.getId() + " &ato its &e" + NpcScheduleManager.ticksToClock(ticks) + " &alocation."
                        : "&cCould not path NPC &e" + npc.getId() + "&c.")));
                return true;
            }

            case "clear": {
                NPC npc = resolveNpc(sender, args, 1);
                if (npc == null) return missingNpc(sender, "clear");
                mgr().clear(npc.getId());
                sender.sendMessage(Libs.format(Libs.Prefix + "&aCleared the whole schedule for NPC &e" + npc.getId() + "&a."));
                return true;
            }

            case "list": {
                Libs.ScheduleList(sender, mgr().managedIds());
                return true;
            }

            case "manage": {
                NPC npc = resolveNpc(sender, args, 1);
                if (npc == null) return missingNpc(sender, "manage");
                Libs.ManageMenu(sender, npc.getId(), npc.getName(), mgr().getSchedule(npc.getId()));
                return true;
            }

            case "log": {
                NpcScheduleManager m = mgr();
                boolean state;
                if (args.length >= 2) {
                    String a = args[1].toLowerCase(Locale.ROOT);
                    if (a.equals("on") || a.equals("true")) {
                        state = true;
                    } else if (a.equals("off") || a.equals("false")) {
                        state = false;
                    } else {
                        sender.sendMessage(Libs.format(Libs.Prefix + "&cUsage: /vnpc log [on|off]"));
                        return true;
                    }
                    m.setDebug(state);
                } else {
                    state = m.toggleDebug();
                }
                sender.sendMessage(Libs.format(Libs.Prefix + "&7Pathfinding logs are now "
                        + (state ? "&aON" : "&cOFF") + "&7."));
                return true;
            }

            case "prune": {
                int n = mgr().pruneOrphans();
                sender.sendMessage(Libs.format(Libs.Prefix + (n > 0
                        ? "&aRemoved &e" + n + " &aschedule(s) for NPCs that no longer exist."
                        : "&7No orphaned schedules found.")));
                return true;
            }

            case "reload": {
                mgr().load();
                sender.sendMessage(Libs.format(Libs.Prefix + "&aReloaded npcs.yml."));
                return true;
            }

            default:
                sender.sendMessage(Libs.format(Libs.Prefix + "&cUnknown subcommand. Try &e/vnpc help&c."));
                return true;
        }
    }

    private boolean setPreset(CommandSender sender, String[] args, int ticks) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        NPC npc = resolveNpc(sender, args, 1);
        if (npc == null) return missingNpc(sender, args[0]);
        mgr().setEntry(npc.getId(), ticks, player.getLocation());
        player.sendMessage(Libs.format(Libs.Prefix + "&aNPC &e" + npc.getId() + " &7(" + npc.getName()
                + ") &awill be at your position at &e" + NpcScheduleManager.ticksToClock(ticks) + "&a."));
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        sender.sendMessage(Libs.console);
        return null;
    }

    private boolean invalidTime(CommandSender sender, String input) {
        sender.sendMessage(Libs.format(Libs.Prefix + "&cInvalid time: &f" + input
                + " &7(try 8pm, 06:30, noon, midnight, dawn, dusk, or 0-23999 ticks)"));
        return true;
    }

    private boolean missingNpc(CommandSender sender, String usage) {
        sender.sendMessage(Libs.format(Libs.Prefix + "&cNo NPC selected. Left-click an NPC (or &e/npc select&c), "
                + "or pass an id: &e/vnpc " + usage + " <id>"));
        return true;
    }

    private NPC resolveNpc(CommandSender sender, String[] args, int idIndex) {
        if (args.length > idIndex) {
            try {
                return CitizensAPI.getNPCRegistry().getById(Integer.parseInt(args[idIndex]));
            } catch (NumberFormatException ignored) {
            }
        }
        return CitizensAPI.getDefaultNPCSelector().getSelected(sender);
    }

    private String fmt(Location l) {
        if (l == null || l.getWorld() == null) {
            return "&cunset";
        }
        return "&f" + l.getWorld().getName() + " &7"
                + Math.round(l.getX()) + ", " + Math.round(l.getY()) + ", " + Math.round(l.getZ());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return partial(args[0], List.of("help", "settime", "setday", "setnight", "deltime", "info", "send", "manage", "clear", "list", "prune", "log", "reload"));
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("settime") || sub.equals("deltime")) {
                return partial(args[1], List.of("dawn", "6am", "noon", "12pm", "dusk", "6pm", "8pm", "night", "midnight", "12am"));
            }
            if (sub.equals("send")) {
                return partial(args[1], List.of("now", "dawn", "6am", "noon", "12pm", "dusk", "6pm", "8pm", "night", "midnight", "12am"));
            }
            if (sub.equals("log")) {
                return partial(args[1], List.of("on", "off"));
            }
        }
        return new ArrayList<>();
    }

    private List<String> partial(String token, List<String> options) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(o);
            }
        }
        return out;
    }

    @EventHandler
    public void onNpcRemove(NPCRemoveEvent event) {
        int id = event.getNPC().getId();
        if (mgr() != null && mgr().isManaged(id)) {
            mgr().clear(id);
        }
    }

    public static void HelpCommand(CommandSender sender) {
        Libs.send(sender, Libs.CommandDivider);
        Libs.MCTitle(sender);
        Libs.send(sender, Libs.NewLine);
        Libs.FormatHelp(sender);
        Libs.Commands(sender);
        Libs.send(sender, Libs.NewLine);
        Libs.send(sender, Libs.CommandDivider);
        Libs.PluginInformation(sender);
        Libs.send(sender, Libs.CommandDivider);
    }
}