package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.permission.PermissionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PermissionListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        PermissionManager manager = Managers.getPermissionManager();
        if (manager == null) {
            return;
        }
        manager.preloadUser(event.getUniqueId(), event.getName());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        PermissionManager manager = Managers.getPermissionManager();
        if (manager == null) {
            return;
        }
        Player player = event.getPlayer();
        manager.handleLogin(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        PermissionManager manager = Managers.getPermissionManager();
        if (manager == null) {
            return;
        }
        Player player = event.getPlayer();
        manager.handleJoin(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        PermissionManager manager = Managers.getPermissionManager();
        if (manager == null) {
            return;
        }
        if (event.getPlayer() != null) {
            manager.unloadUser(event.getPlayer().getUniqueId());
        }
    }
}

