package net.ellie.alwaysend;

import org.bukkit.World;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.PortalCreateEvent;

public class EndListener implements Listener {

    private final AlwaysEndPlugin plugin;
    private final EndWorld endWorld;

    public EndListener(AlwaysEndPlugin plugin, EndWorld endWorld) {
        this.plugin = plugin;
        this.endWorld = endWorld;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        var loc = endWorld.assignPlayerLocation(player);
        if (loc != null) player.teleport(loc);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        endWorld.releasePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World end = endWorld.getWorld();
        if (end != null && !player.getWorld().equals(end)) {
            var loc = endWorld.getPlayerLocation(player);
            if (loc != null) player.teleport(loc);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        var loc = endWorld.getPlayerLocation(event.getPlayer());
        if (loc != null) event.setRespawnLocation(loc);
    }

    /**
     * Stop the dragon fight at the source. The fight's dragon does not reliably fire
     * CreatureSpawnEvent, so cancel the broader EntitySpawnEvent for the dragon and its crystals.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        World end = endWorld.getWorld();
        if (end == null || !event.getEntity().getWorld().equals(end)) return;
        Entity entity = event.getEntity();
        if (entity instanceof EnderDragon || entity instanceof EnderCrystal) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (!event.getWorld().equals(endWorld.getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("lock-movement", false)) return;
        Player player = event.getPlayer();
        World end = endWorld.getWorld();
        if (end == null || !player.getWorld().equals(end)) return;

        // Only act on actual position changes, not look direction
        var from = event.getFrom();
        var to = event.getTo();
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        var loc = endWorld.getPlayerLocation(player);
        if (loc != null) event.setTo(loc);
    }
}
