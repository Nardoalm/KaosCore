package com.knowplugins.ysubz.kaoscore.bukkit.manager.plugin;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

public class AutoBroadcastManager {

    private final KaosCore plugin;
    private final Random random;

    private BukkitTask task;
    private int index;

    public AutoBroadcastManager(KaosCore plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.index = 0;
        reload();
    }

    public void reload() {
        stop();

        if (plugin == null) {
            return;
        }

        if (!plugin.getConfig().getBoolean("autobroadcast.enabled", false)) {
            return;
        }

        List<String> messages = plugin.getConfig().getStringList("autobroadcast.messages");
        if (messages == null || messages.isEmpty()) {
            return;
        }

        int intervalSeconds = Math.max(5, plugin.getConfig().getInt("autobroadcast.interval-seconds", 300));
        long periodTicks = intervalSeconds * 20L;

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::broadcastNext, periodTicks, periodTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void broadcastNext() {
        if (plugin == null || !plugin.isEnabled()) {
            stop();
            return;
        }

        if (!plugin.getConfig().getBoolean("autobroadcast.enabled", false)) {
            stop();
            return;
        }

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        List<String> messages = plugin.getConfig().getStringList("autobroadcast.messages");
        if (messages == null || messages.isEmpty()) {
            return;
        }

        boolean useRandom = plugin.getConfig().getBoolean("autobroadcast.random", false);
        String raw;
        if (useRandom) {
            raw = messages.get(random.nextInt(messages.size()));
        } else {
            if (index >= messages.size()) {
                index = 0;
            }
            raw = messages.get(index++);
        }

        String colored = ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
        for (String line : colored.split("\n")) {
            Bukkit.broadcastMessage(line);
        }
    }
}

