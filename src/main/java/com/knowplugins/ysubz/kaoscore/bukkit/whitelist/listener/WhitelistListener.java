package com.knowplugins.ysubz.kaoscore.bukkit.whitelist.listener;

import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.whitelist.WhitelistManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class WhitelistListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        // Resolve original name if disguised
        String checkName = player.getName();
        DisguiseManager disguiseManager = MainManager.getDisguiseManager();
        if (disguiseManager != null) {
            String originalName = disguiseManager.getOriginalName(player);
            if (originalName != null && !originalName.trim().isEmpty()) {
                checkName = originalName;

                // Sync with Spigot's native whitelist if enabled
                if (Bukkit.hasWhitelist() && Bukkit.getOfflinePlayer(originalName).isWhitelisted()) {
                    player.setWhitelisted(true);
                }
            }
        }

        // Check KaosCore Whitelist
        WhitelistManager whitelist = MainManager.getWhitelistManager();
        if (whitelist != null && whitelist.isEnabled()) {
            if (player.isOp() || player.hasPermission("kaoscore.whitelist.bypass")) {
                return;
            }

            if (whitelist.isAllowed(checkName, player.getUniqueId())) {
                return;
            }

            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, whitelist.getKickMessage());
        }
    }
}
