package net.bov.main;

import net.bov.main.Commands.MainCommand;
import net.bov.main.Libs.Libs;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class VenturaNPCs extends JavaPlugin {
    private static final int RESOURCE_ID = 136543;

    private static VenturaNPCs instance;
    private final MainCommand mainCommand = new MainCommand();
    private NpcScheduleManager scheduleManager;
    private UpdateChecker updateChecker;

    public static VenturaNPCs getInstance() {
        return instance;
    }

    public NpcScheduleManager getScheduleManager() {
        return this.scheduleManager;
    }

    public UpdateChecker getUpdateChecker() {
        return this.updateChecker;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("Citizens") == null) {
            getLogger().severe("Citizens not found - VenturaNPCs requires Citizens to run. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.scheduleManager = new NpcScheduleManager(this);
        this.scheduleManager.load();
        this.scheduleManager.start();

        PluginCommand cmd = getCommand("vnpc");
        if (cmd != null) {
            cmd.setExecutor(this.mainCommand);
            cmd.setTabCompleter(this.mainCommand);
        } else {
            getLogger().severe("Command 'vnpc' is not defined in plugin.yml!");
        }

        getServer().getPluginManager().registerEvents(this.mainCommand, this);

        if (getConfig().getBoolean("update-notifications", true)) {
            this.updateChecker = new UpdateChecker(this, RESOURCE_ID);
            this.updateChecker.check();
        }

        Libs.debugMsg(ChatColor.GOLD + "[]-" + ChatColor.GOLD + "[]-----------------------------------------------------[]");
        Libs.debugMsg(ChatColor.GOLD + "[]" + ChatColor.GREEN + " VenturaNPCs has been Enabled");
        Libs.debugMsg(ChatColor.GOLD + "[]-" + ChatColor.GOLD + "[]-----------------------------------------------------[]");
    }

    @Override
    public void onDisable() {
        if (this.scheduleManager != null) {
            this.scheduleManager.stop();
            this.scheduleManager.save();
        }
    }
}