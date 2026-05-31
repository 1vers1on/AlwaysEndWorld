package net.ellie.alwaysend;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
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
            configure(existing);
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
        configure(w);

        plugin.getLogger().info("Void end world '" + name + "' created.");
        return w;
    }

    /** Settings that must be (re)applied whether the world is freshly created or loaded from disk. */
    private void configure(World w) {
        // Put the world spawn well away from the dragon-fight structures at the origin (0,0).
        w.setSpawnLocation(playerSpacing, 100, 0);
        // Don't pin the origin chunks in memory. If (0,0) never loads, the vanilla dragon
        // fight there never ticks — so no dragon, no boss bar, no portal/pillars get placed.
        try {
            w.setGameRule(GameRule.SPAWN_CHUNK_RADIUS, 0);
        } catch (Throwable ignored) {
            w.setKeepSpawnInMemory(false);
        }
        neutralizeDragonFight(w);
    }

    private void applyGameRules(World w) {
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, plugin.getConfig().getBoolean("game-rules.daylight-cycle", false));
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, plugin.getConfig().getBoolean("game-rules.weather-cycle", false));
        w.setGameRule(GameRule.DO_MOB_SPAWNING, plugin.getConfig().getBoolean("game-rules.mob-spawning", false));
        w.setGameRule(GameRule.DO_FIRE_TICK, plugin.getConfig().getBoolean("game-rules.fire-tick", false));
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, plugin.getConfig().getBoolean("game-rules.announce-advancements", false));
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, plugin.getConfig().getBoolean("game-rules.immediate-respawn", true));
    }

    /** Public entry point for the repeating safety-net task. */
    public void neutralizeDragonFight() {
        neutralizeDragonFight(world);
    }

    /**
     * Shuts down every part of the vanilla End dragon fight: marks it finished so nothing
     * respawns, hides/clears the boss bar, removes the dragon and its crystals, and clears
     * the portal structures at the origin if those chunks happen to be loaded.
     */
    private void neutralizeDragonFight(World w) {
        if (w == null) return;

        DragonBattle battle = w.getEnderDragonBattle();
        if (battle != null) {
            // Tell the fight the dragon is already dead so it never (re)spawns it.
            try {
                battle.setPreviouslyKilled(true);
            } catch (Throwable ignored) {
                // API not present on this server — the spawn cancel + removals below still cover us.
            }
            if (!plugin.getConfig().getBoolean("dragon-boss-bar", false)) {
                battle.getBossBar().setVisible(false);
                battle.getBossBar().removeAll();
            }
            EnderDragon current = battle.getEnderDragon();
            if (current != null) current.remove();
        }

        // Catch anything that spawned before the plugin loaded or slipped through.
        for (EnderDragon dragon : w.getEntitiesByClass(EnderDragon.class)) dragon.remove();
        for (EnderCrystal crystal : w.getEntitiesByClass(EnderCrystal.class)) crystal.remove();

        // The fight only places blocks once the origin chunk is loaded; clear them when it is.
        if (w.isChunkLoaded(0, 0)) removeFightStructures(w);
    }

    /** Clears the exit-portal fountain and central bedrock the fight builds around (0, ~60-78, 0). */
    private void removeFightStructures(World w) {
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                for (int y = 55; y <= 78; y++) {
                    var block = w.getBlockAt(x, y, z);
                    switch (block.getType()) {
                        case END_PORTAL, BEDROCK, DRAGON_EGG, OBSIDIAN, IRON_BARS, END_STONE ->
                            block.setType(Material.AIR, false);
                        default -> { }
                    }
                }
            }
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
        placeBarrier(slot);
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
        if (slot != null) {
            removeBarrier(slot);
            freeSlots.add(slot);
        }
    }

    private void placeBarrier(int slot) {
        if (world == null) return;
        Location loc = slotToLocation(slot);
        world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ()).setType(Material.BARRIER);
    }

    private void removeBarrier(int slot) {
        if (world == null) return;
        Location loc = slotToLocation(slot);
        world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ()).setType(Material.AIR);
    }

    private Location slotToLocation(int slot) {
        // +1 so slot 0 is never at the origin (0,0), where the dragon-fight structures live.
        return new Location(world, (double) (slot + 1) * playerSpacing, 100.0, 0.5, 0f, 0f);
    }

    public World getWorld() {
        return world;
    }
}
