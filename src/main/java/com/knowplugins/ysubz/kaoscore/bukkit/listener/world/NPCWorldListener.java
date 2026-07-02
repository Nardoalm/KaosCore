package com.knowplugins.ysubz.kaoscore.bukkit.listener.world;

import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.world.NPCManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class NPCWorldListener implements Listener {

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        NPCManager npcManager = MainManager.getNpcManager();
        if (npcManager != null && player.isOnline()) {
            npcManager.despawnAll(player);
            npcManager.spawnAll(player);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        NPCManager npcManager = MainManager.getNpcManager();
        if (npcManager != null && player.isOnline()) {
            npcManager.despawnAll(player);
            npcManager.spawnAll(player);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        NPCManager npcManager = MainManager.getNpcManager();
        if (npcManager != null && player.isOnline()) {
            npcManager.despawnAll(player);
            npcManager.spawnAll(player);
        }
    }
}