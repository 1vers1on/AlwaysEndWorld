package net.ellie.alwaysend;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class AlwaysEndPlugin extends JavaPlugin {

    private EndWorld endWorld;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        endWorld = new EndWorld(this);
        getServer().getPluginManager().registerEvents(new EndListener(this, endWorld), this);
        getLogger().info("AlwaysEnd enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AlwaysEnd disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("alwaysend")) return false;
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            endWorld = new EndWorld(this);
            sender.sendMessage("AlwaysEnd config reloaded.");
            return true;
        }
        sender.sendMessage("Usage: /alwaysend reload");
        return true;
    }
}
