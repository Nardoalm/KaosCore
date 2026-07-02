package com.knowplugins.ysubz.kaoscore.bukkit.listener.entity;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

public class EntityListener implements Listener {

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        KaosCore plugin = KaosCore.getInstance();
        if (plugin != null && !plugin.getConfig().getBoolean("features.protections.entities.clear-drops", true)) {
            return;
        }
        e.getDrops().clear();
        e.setDroppedExp(0);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event){
        KaosCore plugin = KaosCore.getInstance();
        if (plugin != null && !plugin.getConfig().getBoolean("features.protections.entities.cancel-spawn", true)) {
            return;
        }
        event.setCancelled(true);
    }
}
