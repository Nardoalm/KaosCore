package com.knowplugins.ysubz.kaoscore.utils.welcomeMessages;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class WelcomeMessage {
    private static final String middle = " §6entrou no lobby!";

    public static void sendWelcomeMessage(Player player) {
        if (player.hasPermission("nardocore.member.welcomeMessage")) {

            Bukkit.getScheduler().runTaskLater(KaosCore.getInstance(), () -> {

                String playerTag = PlaceholderAPI.setPlaceholders(
                        player,
                        "%leaftags_tag_prefix%"
                );

                player.getServer().broadcastMessage(
                        "§6⭐  " + playerTag + player.getName() + middle
                );

            }, 10L);
        }
    }
}
