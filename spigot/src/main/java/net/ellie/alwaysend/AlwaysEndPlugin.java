package net.ellie.alwaysend;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class AlwaysEndPlugin extends JavaPlugin {

    private EndWorld endWorld;
    private BukkitTask cleanupTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        endWorld = new EndWorld(this);
        getServer().getPluginManager().registerEvents(new EndListener(this, endWorld), this);
        startCleanupTask();
        getLogger().info("AlwaysEnd enabled.");
    }

    /** Safety net: keep the dragon fight suppressed even if the origin chunks ever load. */
    private void startCleanupTask() {
        if (cleanupTask != null) cleanupTask.cancel();
        cleanupTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (endWorld != null) endWorld.neutralizeDragonFight();
        }, 20L, 40L);
    }

    @Override
    public void onDisable() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        getLogger().info("AlwaysEnd disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("alwaysend")) return false;
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            endWorld = new EndWorld(this);
            startCleanupTask();
            sender.sendMessage("AlwaysEnd config reloaded.");
            return true;
        }
        sender.sendMessage("Usage: /alwaysend reload");
        return true;
    }
}
