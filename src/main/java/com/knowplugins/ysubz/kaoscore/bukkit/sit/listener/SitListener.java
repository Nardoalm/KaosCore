package com.knowplugins.ysubz.kaoscore.bukkit.sit.listener;

import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.sit.SitManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spigotmc.event.entity.EntityDismountEvent;

public class SitListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.ArmorStand) {
            if (SitManager.SPAWNING_SEAT.get()) {
                event.setCancelled(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;

        SitManager sitManager = MainManager.getSitManager();
        if (sitManager == null) return;

        Player carrier = event.getPlayer();
        Player target = (Player) event.getRightClicked();

        if (carrier.getItemInHand() != null && carrier.getItemInHand().getType() != Material.AIR) {
            return;
        }

        boolean success = carrier.isSneaking() ? sitManager.ride(carrier, target) : sitManager.carry(carrier, target);
        if (success) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        SitManager sitManager = MainManager.getSitManager();
        if (sitManager == null) return;

        Player player = event.getPlayer();

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            if (sitManager.launch(player)) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        SitManager sitManager = MainManager.getSitManager();
        if (sitManager != null && sitManager.launch((Player) event.getDamager())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        SitManager sitManager = MainManager.getSitManager();
        if (sitManager != null) {
            sitManager.handleSeatDismount((Player) event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) return;

        SitManager sitManager = MainManager.getSitManager();
        if (sitManager != null) sitManager.cleanup(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        SitManager sitManager = MainManager.getSitManager();
        if (sitManager != null) sitManager.cleanup(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        SitManager sitManager = MainManager.getSitManager();
        if (sitManager != null) sitManager.cleanup(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        SitManager sitManager = MainManager.getSitManager();
        if (sitManager != null) sitManager.cleanup(event.getEntity());
    }
}