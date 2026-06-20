package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class ProtectionListener implements Listener {
    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof Player) {
            KaosCore plugin = KaosCore.getInstance();
            if (plugin == null || !plugin.getConfig().getBoolean("features.protections.spawn.enabled", true)) {
                return;
            }
            if (!plugin.getConfig().getBoolean("features.protections.spawn.cancel-combust", true)) {
                return;
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            KaosCore plugin = KaosCore.getInstance();
            if (plugin == null || !plugin.getConfig().getBoolean("features.protections.spawn.enabled", true)) {
                return;
            }
            if (!plugin.getConfig().getBoolean("features.protections.spawn.cancel-damage", true)) {
                return;
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            KaosCore plugin = KaosCore.getInstance();
            if (plugin == null || !plugin.getConfig().getBoolean("features.protections.spawn.enabled", true)) {
                return;
            }
            if (!plugin.getConfig().getBoolean("features.protections.spawn.cancel-hunger", true)) {
                return;
            }
            event.setCancelled(true);
        }
    }
}
