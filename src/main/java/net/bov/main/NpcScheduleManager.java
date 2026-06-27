package net.bov.main;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NpcScheduleManager {

    private static final long CHECK_INTERVAL = 100L;

    private static final Pattern AMPM = Pattern.compile("^(\\d{1,2})(?::(\\d{2}))?(am|pm)$");
    private static final Pattern HHMM = Pattern.compile("^(\\d{1,2}):(\\d{2})$");

    private final VenturaNPCs plugin;
    private final File file;
    private FileConfiguration config;

    private final Map<Integer, TreeMap<Integer, Location>> schedules = new HashMap<>();
    private final Map<Integer, Integer> lastActive = new HashMap<>();

    private BukkitTask task;

    private boolean debug = false;

    public boolean isDebug() {
        return this.debug;
    }

    public void setDebug(boolean value) {
        this.debug = value;
    }

    public boolean toggleDebug() {
        this.debug = !this.debug;
        return this.debug;
    }

    private void log(String msg) {
        if (this.debug) {
            this.plugin.getLogger().info(msg);
        }
    }

    private void logWarn(String msg) {
        if (this.debug) {
            this.plugin.getLogger().warning(msg);
        }
    }

    public NpcScheduleManager(VenturaNPCs plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npcs.yml");
    }

    public void load() {
        this.schedules.clear();
        this.lastActive.clear();

        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }
        this.config = YamlConfiguration.loadConfiguration(this.file);

        if (this.config.isConfigurationSection("npcs")) {
            Set<String> ids = this.config.getConfigurationSection("npcs").getKeys(false);
            for (String key : ids) {
                int id;
                try {
                    id = Integer.parseInt(key);
                } catch (NumberFormatException ex) {
                    this.plugin.getLogger().warning("Skipping non-numeric NPC id in npcs.yml: " + key);
                    continue;
                }
                TreeMap<Integer, Location> sched = new TreeMap<>();

                String schedPath = "npcs." + key + ".schedule";
                if (this.config.isConfigurationSection(schedPath)) {
                    for (String tk : this.config.getConfigurationSection(schedPath).getKeys(false)) {
                        try {
                            int ticks = Integer.parseInt(tk);
                            Location loc = readLoc(schedPath + "." + tk);
                            if (loc != null) {
                                sched.put(normalize(ticks), loc);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                Location legacyDay = readLoc("npcs." + key + ".day");
                if (legacyDay != null) {
                    sched.putIfAbsent(0, legacyDay);
                }
                Location legacyNight = readLoc("npcs." + key + ".night");
                if (legacyNight != null) {
                    sched.putIfAbsent(13000, legacyNight);
                }

                if (!sched.isEmpty()) {
                    this.schedules.put(id, sched);
                }
            }
        }
        this.plugin.getLogger().info("Loaded schedules for " + this.schedules.size() + " NPC(s).");
    }

    public void save() {
        FileConfiguration out = new YamlConfiguration();
        for (Map.Entry<Integer, TreeMap<Integer, Location>> npc : this.schedules.entrySet()) {
            for (Map.Entry<Integer, Location> e : npc.getValue().entrySet()) {
                writeLoc(out, "npcs." + npc.getKey() + ".schedule." + e.getKey(), e.getValue());
            }
        }
        try {
            out.save(this.file);
        } catch (IOException ex) {
            this.plugin.getLogger().severe("Failed to save npcs.yml: " + ex.getMessage());
        }
    }

    private void writeLoc(FileConfiguration c, String path, Location l) {
        c.set(path + ".world", l.getWorld() == null ? null : l.getWorld().getName());
        c.set(path + ".x", l.getX());
        c.set(path + ".y", l.getY());
        c.set(path + ".z", l.getZ());
        c.set(path + ".yaw", (double) l.getYaw());
        c.set(path + ".pitch", (double) l.getPitch());
    }

    private Location readLoc(String path) {
        if (!this.config.contains(path + ".world")) {
            return null;
        }
        String worldName = this.config.getString(path + ".world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            this.plugin.getLogger().warning("World '" + worldName + "' for " + path + " is not loaded; skipping.");
            return null;
        }
        return new Location(
                world,
                this.config.getDouble(path + ".x"),
                this.config.getDouble(path + ".y"),
                this.config.getDouble(path + ".z"),
                (float) this.config.getDouble(path + ".yaw"),
                (float) this.config.getDouble(path + ".pitch"));
    }

    public void setEntry(int id, int ticks, Location loc) {
        this.schedules.computeIfAbsent(id, k -> new TreeMap<>()).put(normalize(ticks), loc.clone());
        this.lastActive.remove(id);
        save();
    }

    public boolean removeEntry(int id, int ticks) {
        TreeMap<Integer, Location> sched = this.schedules.get(id);
        if (sched == null) {
            return false;
        }
        boolean removed = sched.remove(normalize(ticks)) != null;
        if (sched.isEmpty()) {
            this.schedules.remove(id);
        }
        this.lastActive.remove(id);
        if (removed) {
            save();
        }
        return removed;
    }

    public void clear(int id) {
        this.schedules.remove(id);
        this.lastActive.remove(id);
        save();
    }

    public int pruneOrphans() {
        int removed = 0;
        java.util.Iterator<Integer> it = this.schedules.keySet().iterator();
        while (it.hasNext()) {
            int id = it.next();
            if (CitizensAPI.getNPCRegistry().getById(id) == null) {
                it.remove();
                this.lastActive.remove(id);
                removed++;
            }
        }
        if (removed > 0) {
            save();
        }
        return removed;
    }

    public TreeMap<Integer, Location> getSchedule(int id) {
        return this.schedules.get(id);
    }

    public int getCurrentTicks(int id) {
        TreeMap<Integer, Location> sched = this.schedules.get(id);
        if (sched == null || sched.isEmpty()) {
            return 0;
        }
        World w = sched.firstEntry().getValue().getWorld();
        return w == null ? 0 : (int) (w.getTime() % 24000L);
    }

    public Location getActiveLocation(int id, int ticks) {
        TreeMap<Integer, Location> sched = this.schedules.get(id);
        if (sched == null || sched.isEmpty()) {
            return null;
        }
        Integer key = sched.floorKey(normalize(ticks));
        if (key == null) {
            key = sched.lastKey();
        }
        return sched.get(key);
    }

    public boolean isManaged(int id) {
        TreeMap<Integer, Location> sched = this.schedules.get(id);
        return sched != null && !sched.isEmpty();
    }

    public Set<Integer> managedIds() {
        return new java.util.TreeSet<>(this.schedules.keySet());
    }

    public void start() {
        if (this.task != null) {
            this.task.cancel();
        }
        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, CHECK_INTERVAL, CHECK_INTERVAL);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    private void tick() {
        for (Map.Entry<Integer, TreeMap<Integer, Location>> entry : this.schedules.entrySet()) {
            int id = entry.getKey();
            TreeMap<Integer, Location> sched = entry.getValue();
            if (sched.isEmpty()) {
                continue;
            }

            World world = sched.firstEntry().getValue().getWorld();
            if (world == null) {
                continue;
            }
            int now = (int) (world.getTime() % 24000L);

            Integer key = sched.floorKey(now);
            if (key == null) {
                key = sched.lastKey();
            }

            Integer previous = this.lastActive.get(id);
            if (previous != null && previous.equals(key)) {
                continue;
            }

            pathTo(id, sched.get(key));
            this.lastActive.put(id, key);
        }
    }

    public boolean pathTo(int id, Location target) {
        NPC npc = CitizensAPI.getNPCRegistry().getById(id);
        if (npc == null) {
            logWarn("pathTo: no Citizens NPC with id " + id);
            return false;
        }
        if (!npc.isSpawned()) {
            logWarn("pathTo: NPC " + id + " is not spawned (chunk unloaded?) - skipping.");
            return false;
        }

        npc.getNavigator().getDefaultParameters()
                .useNewPathfinder(true)
                .range(100.0F);
        npc.getNavigator().setTarget(target);

        Location cur = npc.getEntity() == null ? null : npc.getEntity().getLocation();
        String dist = (cur != null && cur.getWorld() != null && cur.getWorld().equals(target.getWorld()))
                ? String.format("%.1f", cur.distance(target)) : "?";
        log("pathTo: NPC " + id + " walking to "
                + target.getWorld().getName() + " " + Math.round(target.getX()) + "," + Math.round(target.getY())
                + "," + Math.round(target.getZ()) + " (distance " + dist + " blocks)");

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            NPC n = CitizensAPI.getNPCRegistry().getById(id);
            if (n == null || !n.isSpawned() || n.getEntity() == null) {
                return;
            }
            Location nowLoc = n.getEntity().getLocation();
            boolean sameWorld = nowLoc.getWorld() != null && nowLoc.getWorld().equals(target.getWorld());
            double d = sameWorld ? nowLoc.distance(target) : Double.MAX_VALUE;
            if (!n.getNavigator().isNavigating() && d > 2.0) {
                n.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
                log("pathTo: NPC " + id + " had no walkable path ("
                        + String.format("%.1f", d) + " blocks away) - teleported to target instead.");
            }
        }, 60L);
        return true;
    }

    public static int normalize(int ticks) {
        int t = ticks % 24000;
        if (t < 0) {
            t += 24000;
        }
        return t;
    }

    public static int parseTime(String input) {
        if (input == null) {
            return -1;
        }
        String s = input.trim().toLowerCase(Locale.ROOT);
        switch (s) {
            case "dawn":
            case "sunrise":
            case "day":
            case "morning":
                return 0;
            case "noon":
            case "midday":
                return 6000;
            case "dusk":
            case "sunset":
            case "evening":
                return 12000;
            case "night":
                return 13000;
            case "midnight":
                return 18000;
            default:
                break;
        }

        Matcher m = AMPM.matcher(s);
        if (m.matches()) {
            int hour = Integer.parseInt(m.group(1));
            int min = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
            if (hour < 1 || hour > 12 || min > 59) {
                return -1;
            }
            if (m.group(3).equals("am")) {
                if (hour == 12) {
                    hour = 0;
                }
            } else if (hour != 12) {
                hour += 12;
            }
            return clockToTicks(hour, min);
        }

        m = HHMM.matcher(s);
        if (m.matches()) {
            int hour = Integer.parseInt(m.group(1));
            int min = Integer.parseInt(m.group(2));
            if (hour > 23 || min > 59) {
                return -1;
            }
            return clockToTicks(hour, min);
        }

        try {
            int t = Integer.parseInt(s);
            if (t >= 0 && t < 24000) {
                return t;
            }
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }

    private static int clockToTicks(int hour24, int minute) {
        int ticks = ((hour24 - 6) * 1000) + (minute * 1000 / 60);
        return normalize(ticks);
    }

    public static String ticksToClock(int ticks) {
        int t = normalize(ticks);
        int clockMin = (int) Math.round(360 + t * 0.06) % 1440;
        int hour24 = clockMin / 60;
        int min = clockMin % 60;
        String ap = hour24 < 12 ? "am" : "pm";
        int h12 = hour24 % 12;
        if (h12 == 0) {
            h12 = 12;
        }
        return String.format("%d:%02d%s", h12, min, ap);
    }
}