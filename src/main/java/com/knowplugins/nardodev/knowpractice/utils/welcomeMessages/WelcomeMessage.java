package com.knowplugins.nardodev.knowpractice.utils.welcomeMessages;

import me.clip.placeholderapi.PlaceholderAPI;
import com.knowplugins.nardodev.knowpractice.KnowPractice;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class WelcomeMessage {

    private static final String coloredLine =
            "§c§m          §6§m          §e§m          §a§m          §b§m          ";
    private static final String middle = " §6entrou no lobby!";

    public static void sendWelcomeMessage(Player player) {
        if (player.hasPermission("nardocore.member.welcomeMessage")) {

            Bukkit.getScheduler().runTaskLater(KnowPractice.getInstance(), () -> {

                String playerTag = PlaceholderAPI.setPlaceholders(
                        player,
                        "%leaftags_tag_prefix%"
                );

                player.getServer().broadcastMessage(coloredLine);
                player.getServer().broadcastMessage("");
                player.getServer().broadcastMessage(
                        "§6⭐  " + playerTag + player.getName() + middle
                );
                player.getServer().broadcastMessage("");
                player.getServer().broadcastMessage(coloredLine);

            }, 10L);
        }
    }
}
