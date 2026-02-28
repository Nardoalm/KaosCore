package com.knowplugins.nardodev.knowpractice.listeners.world;

import com.knowplugins.nardodev.knowpractice.managers.world.BuildManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class WorldProtectionListener implements Listener {
    private static boolean pvp = true;

    public WorldProtectionListener(){
    }

    public static boolean getPvp(){
        return pvp;
    }

    public static void setPvp(boolean status) {
        pvp = status;
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            if (!pvp) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!BuildManager.isBuilder(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!BuildManager.isBuilder(player)) {
            event.setCancelled(true);
        }
    }
}