package com.knowplugins.ysubz.kaoscore.bukkit.listener.player;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.account.AccountController;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.ScoreboardManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        event.setQuitMessage(null);

        KaosCore plugin = KaosCore.getInstance();
        AccountController accounts = plugin == null ? null : plugin.getAccountController();
        if (accounts != null) {
            accounts.markOffline(player);
        }

        if (plugin != null && plugin.getTagManager() != null) {
            plugin.getTagManager().removeTagEntries(player);
        }

        ScoreboardManager.remove(player);

        if (MainManager.getNpcManager() != null) {
            MainManager.getNpcManager().cleanup(player);
        }
    }
}