package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.account.AccountController;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.managers.player.ScoreboardManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        KaosCore plugin = KaosCore.getInstance();
        AccountController accounts = plugin == null ? null : plugin.getAccountController();
        if (accounts != null) {
            accounts.markOffline(player);
        }
        if (plugin != null && plugin.getTagManager() != null) {
            plugin.getTagManager().removeTagEntries(player);
        }

        ScoreboardManager.remove(player);

        if (Managers.getNpcManager() != null) {
            Managers.getNpcManager().cleanup(player);
        }

        DisguiseManager disguiseManager = Managers.getDisguiseManager();
        if (disguiseManager != null && DisguiseManager.isDisguised(player)) {
            disguiseManager.removeDisguise(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
    }
}
