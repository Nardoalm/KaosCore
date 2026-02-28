package com.knowplugins.nardodev.knowpractice.listeners.player;

import com.knowplugins.nardodev.knowpractice.cmd.commands.SkinCommand;
import com.knowplugins.nardodev.knowpractice.managers.player.DisguiseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        SkinCommand.ORIGINAL_SKINS.remove(player);

        DisguiseManager disguiseManager = new DisguiseManager();
        if (DisguiseManager.isDisguised(player)) {
            disguiseManager.removeDisguise(player);
        }
    }
}