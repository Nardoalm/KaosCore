package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.SitManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SitListener implements Listener {
    private final Set<UUID> processingTeleport = new HashSet<>();

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) {
            return;
        }

        SitManager sitManager = Managers.getSitManager();
        if (sitManager == null) {
            return;
        }

        if (sitManager.carry(event.getPlayer(), (Player) event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        SitManager sitManager = Managers.getSitManager();
        if (sitManager == null) {
            return;
        }

        if (sitManager.launch(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        SitManager sitManager = Managers.getSitManager();
        if (sitManager != null) {
            sitManager.handleSeatDismount((Player) event.getEntity(), event.getDismounted());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (processingTeleport.contains(player.getUniqueId())) {
            return;
        }

        SitManager sitManager = Managers.getSitManager();
        if (sitManager != null) {
            processingTeleport.add(player.getUniqueId());
            try {
                sitManager.cleanup(player);
            } finally {
                processingTeleport.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }

        SitManager sitManager = Managers.getSitManager();
        if (sitManager != null) {
            sitManager.unsitIfSitting(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        SitManager sitManager = Managers.getSitManager();
        if (sitManager != null) {
            sitManager.cleanup(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        SitManager sitManager = Managers.getSitManager();
        if (sitManager != null) {
            sitManager.cleanup(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        SitManager sitManager = Managers.getSitManager();
        if (sitManager != null) {
            sitManager.cleanup((Player) event.getEntity());
        }
    }
}
