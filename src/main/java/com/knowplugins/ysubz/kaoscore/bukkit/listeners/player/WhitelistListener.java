package com.knowplugins.ysubz.kaoscore.bukkit.listeners.player;

import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.world.WhitelistManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class WhitelistListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        WhitelistManager whitelist = Managers.getWhitelistManager();
        if (whitelist == null || !whitelist.isEnabled()) {
            return;
        }

        if (event.getPlayer() != null) {
            if (event.getPlayer().isOp() || event.getPlayer().hasPermission("kaoscore.whitelist.bypass") || event.getPlayer().hasPermission("kaoscore.whitelist.bypass")) {
                return;
            }

            if (whitelist.isAllowed(event.getPlayer().getName(), event.getPlayer().getUniqueId())) {
                return;
            }
        }

        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, whitelist.getKickMessage());
    }
}


