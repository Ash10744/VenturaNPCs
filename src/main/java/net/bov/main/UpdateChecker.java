package net.bov.main;

import net.bov.main.Libs.Libs;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UpdateChecker {

    private final VenturaNPCs plugin;
    private final int resourceId;

    private volatile String latestVersion;
    private volatile boolean updateAvailable;

    public UpdateChecker(VenturaNPCs plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void check() {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            String current = this.plugin.getDescription().getVersion();
            try {
                String latest = fetchLatest();
                if (latest == null || latest.isEmpty()) {
                    return;
                }
                this.latestVersion = latest;
                this.updateAvailable = isNewer(latest, current);
                if (this.updateAvailable) {
                    this.plugin.getLogger().info("A new version is available: " + latest
                            + " (you are running " + current + "). Download it at "
                            + "https://www.spigotmc.org/resources/" + this.resourceId);
                } else {
                    this.plugin.getLogger().info("You are running the latest version (" + current + ").");
                }
            } catch (IOException ex) {
                this.plugin.getLogger().warning("Update check failed: " + ex.getMessage());
            }
        });
    }

    private String fetchLatest() throws IOException {
        URL url = URI.create("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId).toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setRequestProperty("User-Agent", "VenturaNPCs");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String line = in.readLine();
            return line == null ? null : line.trim();
        } finally {
            con.disconnect();
        }
    }

    public boolean isUpdateAvailable() {
        return this.updateAvailable;
    }

    public String getLatestVersion() {
        return this.latestVersion;
    }

    public void notifyPlayer(Player player) {
        if (!this.updateAvailable) {
            return;
        }
        String current = this.plugin.getDescription().getVersion();
        TextComponent msg = new TextComponent(Libs.format(
                "&8[&6VNPC&8] &eA new version of VenturaNPCs is available! &7(" + current + " &7-> &a" + this.latestVersion + "&7) "));
        TextComponent link = new TextComponent(Libs.format("&8[&bDownload&8]"));
        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(Libs.format("&7Open the VenturaNPCs Spigot page")).create()));
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                "https://www.spigotmc.org/resources/" + this.resourceId));
        msg.addExtra(link);
        player.spigot().sendMessage(msg);
    }

    static boolean isNewer(String latest, String current) {
        int[] a = parse(latest);
        int[] b = parse(current);
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) {
                return x > y;
            }
        }
        return false;
    }

    private static int[] parse(String version) {
        String cleaned = version.replaceAll("[^0-9.]", "");
        String[] parts = cleaned.split("\\.");
        List<Integer> nums = new ArrayList<>();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            try {
                nums.add(Integer.parseInt(p));
            } catch (NumberFormatException ignored) {
            }
        }
        int[] out = new int[nums.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = nums.get(i);
        }
        return out;
    }
}