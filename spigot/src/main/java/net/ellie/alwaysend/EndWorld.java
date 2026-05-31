package net.ellie.alwaysend;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

public class EndWorld {

    private final AlwaysEndPlugin plugin;
    private final World world;
    private final int playerSpacing;
    private int nextSlot = 0;
    private final TreeSet<Integer> freeSlots = new TreeSet<>();
    private final Map<UUID, Integer> playerSlots = new HashMap<>();

    public EndWorld(AlwaysEndPlugin plugin) {
        this.plugin = plugin;
        this.playerSpacing = plugin.getConfig().getInt("world.player-spacing", 10000);
        this.world = loadOrCreate();
    }

    private World loadOrCreate() {
        String name = plugin.getConfig().getString("world.name", "alwaysend");

        World existing = Bukkit.getWorld(name);
        if (existing != null) {
            applyDragonBossBar(existing);
            return existing;
        }

        World w = new WorldCreator(name)
            .environment(World.Environment.THE_END)
            .generator(new VoidChunkGenerator())
            .createWorld();

        if (w == null) {
            plugin.getLogger().severe("Failed to create end world — players will not be teleported.");
            return null;
        }

        applyGameRules(w);
        w.setAutoSave(false);
        w.setSpawnLocation(0, 100, 0);
        applyDragonBossBar(w);

        plugin.getLogger().info("Void end world '" + name + "' created.");
        return w;
    }

    private void applyGameRules(World w) {
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, plugin.getConfig().getBoolean("game-rules.daylight-cycle", false));
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, plugin.getConfig().getBoolean("game-rules.weather-cycle", false));
        w.setGameRule(GameRule.DO_MOB_SPAWNING, plugin.getConfig().getBoolean("game-rules.mob-spawning", false));
        w.setGameRule(GameRule.DO_FIRE_TICK, plugin.getConfig().getBoolean("game-rules.fire-tick", false));
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, plugin.getConfig().getBoolean("game-rules.announce-advancements", false));
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, plugin.getConfig().getBoolean("game-rules.immediate-respawn", true));
    }

    private void applyDragonBossBar(World w) {
        DragonBattle battle = w.getEnderDragonBattle();
        if (battle != null) {
            battle.getBossBar().setVisible(plugin.getConfig().getBoolean("dragon-boss-bar", false));
        }
    }

    public Location assignPlayerLocation(Player player) {
        if (world == null) return null;
        int slot;
        if (!freeSlots.isEmpty()) {
            slot = freeSlots.first();
            freeSlots.remove(slot);
        } else {
            slot = nextSlot++;
        }
        playerSlots.put(player.getUniqueId(), slot);
        return slotToLocation(slot);
    }

    public Location getPlayerLocation(Player player) {
        if (world == null) return null;
        Integer slot = playerSlots.get(player.getUniqueId());
        if (slot == null) return assignPlayerLocation(player);
        return slotToLocation(slot);
    }

    public void releasePlayer(Player player) {
        Integer slot = playerSlots.remove(player.getUniqueId());
        if (slot != null) freeSlots.add(slot);
    }

    private Location slotToLocation(int slot) {
        return new Location(world, (double) slot * playerSpacing, 100.0, 0.5, 0f, 0f);
    }

    public World getWorld() {
        return world;
    }
}
