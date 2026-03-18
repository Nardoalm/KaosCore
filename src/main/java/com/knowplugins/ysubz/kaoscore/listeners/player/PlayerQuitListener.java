package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        KaosCore plugin = KaosCore.getInstance();
        if (plugin != null && plugin.getTagManager() != null) {
            plugin.getTagManager().removeTagEntries(player);
        }

        if (Managers.getNpcManager() != null) {
            Managers.getNpcManager().cleanup(player);
        }

        DisguiseManager disguiseManager = new DisguiseManager();
        if (DisguiseManager.isDisguised(player)) {
            disguiseManager.removeDisguise(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
    }
}
