package com.knowplugins.ysubz.kaoscore.bukkit.auth.listener;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import org.bukkit.Location;
import org.bukkit.Bukkit;


public class AuthListener implements Listener {

    private final KaosCore plugin;

    public AuthListener(KaosCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getAuthManager().handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getAuthManager().handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (plugin.getAuthManager().isPending(event.getPlayer().getUniqueId())) {
            org.bukkit.Location from = event.getFrom();
            org.bukkit.Location to = event.getTo();
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                org.bukkit.Location newLoc = from.clone();
                newLoc.setYaw(to.getYaw());
                newLoc.setPitch(to.getPitch());
                event.setTo(newLoc);
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (plugin.getAuthManager().isPending(event.getPlayer().getUniqueId())) {
            String message = event.getMessage().trim();
            String cmd = message.split(" ")[0].toLowerCase();
            plugin.getLogger().info("[AuthDebug] Pending player " + event.getPlayer().getName() + " issued command: " + message + " (parsed cmd: " + cmd + ")");
            if (!cmd.equals("/login") && !cmd.equals("/register") && !cmd.equals("/reg")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cYou must authenticate first.");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (plugin.getAuthManager().isPending(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou must authenticate first.");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (plugin.getAuthManager().isPending(event.getEntity().getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (plugin.getAuthManager().isPending(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getAuthManager().isPending(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (plugin.getAuthManager().isPending(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getAuthManager().isPending(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.getAuthManager().isPending(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (plugin.getAuthManager().isPending(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        Player player = event.getPlayer();
        Location loc = null;

        com.knowplugins.ysubz.kaoscore.bukkit.auth.profile.AuthProfile profile = plugin.getAuthManager().getCachedProfile(player.getUniqueId());
        if (profile != null && profile.isPremium()) {
            if (Bukkit.getServer().getPluginManager().getPlugin("HardPractice") != null) {
                try {
                    loc = live.pvphard.practice.HardPractice.getInstance()
                            .getService(live.pvphard.practice.feature.spawn.SpawnService.class)
                            .getLocation();
                } catch (Throwable ignored) {}
            }
        }

        if (loc == null) {
            loc = plugin.getAuthManager().getAuthLocation();
        }

        if (loc == null) {
            try {
                String raw = plugin.getConfig().getString("spawn.location");
                if (raw != null && !raw.trim().isEmpty() && raw.contains(";")) {
                    String[] parts = raw.split(";");
                    if (parts.length >= 6) {
                        org.bukkit.World world = Bukkit.getWorld(parts[0]);
                        if (world != null) {
                            double x = Double.parseDouble(parts[1]);
                            double y = Double.parseDouble(parts[2]);
                            double z = Double.parseDouble(parts[3]);
                            float yaw = Float.parseFloat(parts[4]);
                            float pitch = Float.parseFloat(parts[5]);
                            loc = new Location(world, x, y, z, yaw, pitch);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        if (loc != null) {
            event.setSpawnLocation(loc);
        }
    }
}