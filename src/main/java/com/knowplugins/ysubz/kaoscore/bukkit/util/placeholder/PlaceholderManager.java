package com.knowplugins.ysubz.kaoscore.bukkit.util.placeholder;

import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.disguise.DisguiseManager;
import org.bukkit.entity.Player;

public class PlaceholderManager {

    public static String replacePlaceholders(String message, Player player) {
        if (message == null || player == null) {
            return message;
        }

        String playerNick = getPlayerNick(player);
        message = message.replace("%kaoscore_player-nick%", playerNick);

        return message;
    }

    private static String getPlayerNick(Player player) {
        if (player == null) {
            return "Unknown";
        }

        DisguiseManager disguiseManager = MainManager.getDisguiseManager();
        if (disguiseManager != null && DisguiseManager.isDisguised(player)) {
            String disguiseNick = disguiseManager.getDisguiseNick(player);
            if (disguiseNick != null) {
                return disguiseNick;
            }
        }

        return player.getName();
    }

    public static String getDisplayName(Player player) {
        if (player == null) {
            return "Unknown";
        }

        if (DisguiseManager.isDisguised(player)) {
            DisguiseManager disguiseManager = MainManager.getDisguiseManager();
            if (disguiseManager != null) {
                String disguiseNick = disguiseManager.getDisguiseNick(player);
                if (disguiseNick != null) {
                    return disguiseNick;
                }
            }
        }

        return player.getName();
    }

    public static String getAccountName(Player player) {
        if (player == null) {
            return "Unknown";
        }

        DisguiseManager disguiseManager = MainManager.getDisguiseManager();
        if (disguiseManager != null) {
            String originalName = disguiseManager.getOriginalName(player);
            if (originalName != null && !originalName.trim().isEmpty()) {
                return originalName;
            }
        }

        return player.getName();
    }
}
