package com.knowplugins.ysubz.kaoscore.listeners.world;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.world.BuildManager;
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
            KaosCore plugin = KaosCore.getInstance();
            boolean pvpAllowedByConfig = plugin == null || plugin.getConfig().getBoolean("features.protections.pvp.enabled", true);
            if (!pvpAllowedByConfig || !pvp) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        KaosCore plugin = KaosCore.getInstance();
        if (plugin != null) {
            if (!plugin.getConfig().getBoolean("features.protections.world.enabled", true)) {
                return;
            }
            if (!plugin.getConfig().getBoolean("features.protections.world.cancel-block-break", true)) {
                return;
            }
        }

        Player player = event.getPlayer();
        if (!BuildManager.isBuilder(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        KaosCore plugin = KaosCore.getInstance();
        if (plugin != null) {
            if (!plugin.getConfig().getBoolean("features.protections.world.enabled", true)) {
                return;
            }
            if (!plugin.getConfig().getBoolean("features.protections.world.cancel-block-place", true)) {
                return;
            }
        }

        Player player = event.getPlayer();
        if (!BuildManager.isBuilder(player)) {
            event.setCancelled(true);
        }
    }
}
