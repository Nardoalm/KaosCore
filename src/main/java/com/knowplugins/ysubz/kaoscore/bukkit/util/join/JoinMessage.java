package com.knowplugins.ysubz.kaoscore.bukkit.util.join;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.integration.practice.PracticeModeBlocker;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.manager.TagManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.VanishManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.JoinMessageManager;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JoinMessage {
    private static final Set<String> processedJoins = ConcurrentHashMap.newKeySet();

    public static void sendWelcomeMessage(Player player) {
        if (player == null || !player.isOnline() || !player.hasPermission("kaoscore.member.welcomeMessage")) {
            return;
        }

        if (PracticeModeBlocker.check(player, false)) {
            return;
        }

        if (VanishManager.isVanished(player)) {
            return;
        }

        KaosCore plugin = KaosCore.getInstance();
        if (plugin == null || !plugin.getConfig().getBoolean("features.welcome-messages.enabled", true)) {
            return;
        }

        DisguiseManager disguiseManager = MainManager.getDisguiseManager();
        boolean disguised = DisguiseManager.isDisguised(player);

        if (disguised && disguiseManager != null) {
            String currentNick = disguiseManager.getDisguiseNick(player);
            if (currentNick == null || currentNick.trim().isEmpty()) {
                return;
            }
        }

        String uniqueKey = player.getUniqueId().toString() + ":" + (disguised ? "true" : "false");
        if (processedJoins.contains(uniqueKey)) {
            return;
        }
        processedJoins.add(uniqueKey);

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> processedJoins.remove(uniqueKey), 40L);

        TagManager tagManager = plugin.getTagManager();
        String tag = (tagManager != null && tagManager.getTag(player) != null) ? tagManager.getTag(player).getPrefix() : "";

        if (tag != null && tag.startsWith("§7")) {
            return;
        }

        String playerText;
        if (disguised && disguiseManager != null) {
            playerText = "§6§l⭐ " + tag + disguiseManager.getDisguiseNick(player);
        } else {
            playerText = "§6§l⭐ " + tag + player.getName();
        }

        JoinMessageManager manager = MainManager.getJoinMessageManager();
        JoinMessageManager.Template template = manager == null ? null : manager.resolveSelected(player);
        String raw = template == null ? "{player} §6joined the lobby!" : template.getText();
        String message = raw.contains("{player}") ? raw.replace("{player}", playerText) : (playerText + " " + raw);

        player.getServer().broadcastMessage(message);
    }
}