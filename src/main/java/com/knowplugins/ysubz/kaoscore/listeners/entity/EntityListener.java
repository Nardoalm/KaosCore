package com.knowplugins.ysubz.kaoscore.listeners.entity;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

public class EntityListener implements Listener {

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        e.getDrops().clear();
        e.setDroppedExp(0);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event){
        event.setCancelled(true);
    }
}