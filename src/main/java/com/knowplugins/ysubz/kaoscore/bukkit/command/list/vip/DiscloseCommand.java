package com.knowplugins.ysubz.kaoscore.bukkit.command.list.vip;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DiscloseCommand extends Commands {

    private static final ConcurrentMap<UUID, Long> LAST_USE = new ConcurrentHashMap<>();

    public DiscloseCommand() {
        super("disclose", "disc");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        Player player = (Player) sender;
        KaosCore plugin = KaosCore.getInstance();
        if (plugin == null) {
            return;
        }

        if (!plugin.getConfig().getBoolean("divulgar.enabled", true)) {
            player.sendMessage("§cThis command is currently disabled.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        String permission = safe(plugin.getConfig().getString("divulgar.permission"));
        if (!permission.trim().isEmpty() && !player.hasPermission(permission)) {
            player.sendMessage("§cYou do not have permission to use this command.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        if (args == null || args.length < 1) {
            player.sendMessage("§cUsage: /disclose <link>");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        String link = join(args, 0);
        link = sanitizeLink(link);
        if (link.isEmpty()) {
            player.sendMessage("§cInvalid link.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        String bypassPerm = safe(plugin.getConfig().getString("divulgar.bypass-permission"));
        boolean bypassCooldown = !bypassPerm.trim().isEmpty() && player.hasPermission(bypassPerm);
        int cooldownSeconds = Math.max(0, plugin.getConfig().getInt("divulgar.cooldown-seconds", 60));
        if (!bypassCooldown && cooldownSeconds > 0) {
            long now = System.currentTimeMillis();
            Long last = LAST_USE.get(player.getUniqueId());
            if (last != null) {
                long remainingMs = (last + (cooldownSeconds * 1000L)) - now;
                if (remainingMs > 0) {
                    long remaining = Math.max(1, (remainingMs + 999) / 1000);
                    player.sendMessage("§cPlease wait §f" + remaining + "s §cto advertise again.");
                    player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                    return;
                }
            }
            LAST_USE.put(player.getUniqueId(), now);
        }

        List<String> lines = plugin.getConfig().getStringList("divulgar.broadcast");
        if (lines == null || lines.isEmpty()) {
            lines = java.util.Arrays.asList(
                    "",
                    "&6&lSERVER ➔ &e{player} &7advertised:",
                    " &b{link}",
                    ""
            );
        }

        String playerName = safe(player.getName());
        for (String raw : lines) {
            String line = safe(raw)
                    .replace("{player}", playerName)
                    .replace("{link}", link);
            String colored = ChatColor.translateAlternateColorCodes('&', line);
            for (String split : colored.split("\n")) {
                Bukkit.broadcastMessage(split);
            }
        }

        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
    }

    private String join(String[] args, int start) {
        if (args == null || args.length <= start) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString().trim();
    }

    private String sanitizeLink(String input) {
        String value = safe(input).trim();
        if (value.isEmpty()) {
            return "";
        }

        value = value.replaceAll("(?i)§[0-9A-FK-OR]", "");
        value = value.replace('\n', ' ').replace('\r', ' ').trim();

        if (value.contains(" ")) {
            value = value.replace(" ", "");
        }

        if (value.length() > 120) {
            value = value.substring(0, 120);
        }

        return value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}