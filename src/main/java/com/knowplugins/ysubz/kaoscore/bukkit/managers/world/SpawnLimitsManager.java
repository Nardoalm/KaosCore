package com.knowplugins.ysubz.kaoscore.bukkit.managers.world;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.integration.practice.KaosPracticeBridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnLimitsManager {

    private static final long RETURN_COOLDOWN_MS = 1500L;

    private final KaosCore plugin;
    private final KaosPracticeBridge practiceBridge;
    private final Map<UUID, Long> lastReturn;

    private BukkitTask task;

    public SpawnLimitsManager(KaosCore plugin) {
        this.plugin = plugin;
        this.practiceBridge = new KaosPracticeBridge();
        this.lastReturn = new ConcurrentHashMap<>();
        reload();
    }

    public void reload() {
        stop();

        if (plugin == null) {
            return;
        }

        if (!plugin.getConfig().getBoolean("spawn-limits.enabled", false)) {
            return;
        }

        int intervalTicks = Math.max(1, plugin.getConfig().getInt("spawn-limits.check-interval-ticks", 10));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        lastReturn.clear();
    }

    private void tick() {
        if (plugin == null || !plugin.isEnabled()) {
            stop();
            return;
        }

        if (!plugin.getConfig().getBoolean("spawn-limits.enabled", false)) {
            stop();
            return;
        }

        Location spawn = resolveSpawnLocation();
        if (spawn == null || spawn.getWorld() == null) {
            return;
        }

        boolean onlySpawnWorld = plugin.getConfig().getBoolean("spawn-limits.only-spawn-world", true);
        String bypassPerm = safe(plugin.getConfig().getString("spawn-limits.bypass-permission"));

        int voidLevel = plugin.getConfig().getInt("spawn-limits.void-level-y", -10);
        double maxDistance = plugin.getConfig().getDouble("spawn-limits.max-distance-from-spawn", 250.0D);
        double maxDistanceSq = maxDistance > 0.0D ? (maxDistance * maxDistance) : -1.0D;

        String msgVoid = color(safe(plugin.getConfig().getString("spawn-limits.message-void")));
        String msgRadius = color(safe(plugin.getConfig().getString("spawn-limits.message-radius")));
        Sound sound = parseSound(safe(plugin.getConfig().getString("spawn-limits.sound")));

        World spawnWorld = spawn.getWorld();
        boolean practiceAvailable = practiceBridge.isAvailable();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }

            if (!bypassPerm.trim().isEmpty() && player.hasPermission(bypassPerm)) {
                continue;
            }

            if (practiceAvailable && !practiceBridge.isInLobby(player)) {
                continue;
            }

            Location loc = player.getLocation();
            if (loc == null || loc.getWorld() == null) {
                continue;
            }

            if (onlySpawnWorld && !loc.getWorld().equals(spawnWorld)) {
                continue;
            }

            if (loc.getY() <= voidLevel) {
                returnToSpawn(player, spawn, msgVoid, sound);
                continue;
            }

            if (maxDistanceSq > 0.0D) {
                double dx = loc.getX() - spawn.getX();
                double dz = loc.getZ() - spawn.getZ();
                double distSq = (dx * dx) + (dz * dz);
                if (distSq > maxDistanceSq) {
                    returnToSpawn(player, spawn, msgRadius, sound);
                }
            }
        }
    }

    private void returnToSpawn(Player player, Location spawn, String message, Sound sound) {
        if (player == null || spawn == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Long last = lastReturn.get(player.getUniqueId());
        if (last != null && (now - last) < RETURN_COOLDOWN_MS) {
            return;
        }
        lastReturn.put(player.getUniqueId(), now);

        Location target = spawn.clone();
        player.teleport(target);

        if (!message.trim().isEmpty()) {
            player.sendMessage(message);
        }

        if (sound != null) {
            try {
                player.playSound(target, sound, 1.0f, 1.0f);
            } catch (Exception ignored) {
            }
        }
    }

    private Location resolveSpawnLocation() {
        if (plugin == null) {
            return fallbackSpawn();
        }

        try {
            String raw = plugin.getConfig().getString("spawn.location");
            if (raw != null && !raw.trim().isEmpty() && raw.contains(";")) {
                String[] parts = raw.split(";");
                if (parts.length >= 6) {
                    World world = Bukkit.getWorld(parts[0]);
                    if (world != null) {
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        double z = Double.parseDouble(parts[3]);
                        float yaw = Float.parseFloat(parts[4]);
                        float pitch = Float.parseFloat(parts[5]);
                        return new Location(world, x, y, z, yaw, pitch);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return fallbackSpawn();
    }

    private Location fallbackSpawn() {
        try {
            if (!Bukkit.getWorlds().isEmpty() && Bukkit.getWorlds().get(0) != null) {
                return Bukkit.getWorlds().get(0).getSpawnLocation();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Sound parseSound(String raw) {
        String value = safe(raw).trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Sound.valueOf(value.toUpperCase());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', safe(text));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

