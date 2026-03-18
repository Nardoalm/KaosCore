package com.knowplugins.ysubz.kaoscore.utils.welcomeMessages;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.managers.player.WelcomeMessageManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class WelcomeMessage {

    public static void sendWelcomeMessage(Player player) {
        if (player == null || !player.hasPermission("kaoscore.member.welcomeMessage")) {
            return;
        }

        KaosCore plugin = KaosCore.getInstance();
        if (plugin == null || !plugin.getConfig().getBoolean("features.welcome-messages.enabled", true)) {
            return;
        }

        if (DisguiseManager.isDisguised(player)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            String playerTag = "";
            try {
                playerTag = PlaceholderAPI.setPlaceholders(player, "%kaoscore_tag_prefix%");
            } catch (Exception ignored) {
            }

            String playerText = "§6⭐  " + playerTag + player.getName();

            WelcomeMessageManager manager = Managers.getWelcomeMessageManager();
            WelcomeMessageManager.Template template = manager == null ? null : manager.resolveSelected(player);
            String raw = template == null ? "{player} §6entrou no lobby!" : template.getText();
            String message = raw.contains("{player}") ? raw.replace("{player}", playerText) : (playerText + " " + raw);

            player.getServer().broadcastMessage(message);

        }, 10L);
    }
}

