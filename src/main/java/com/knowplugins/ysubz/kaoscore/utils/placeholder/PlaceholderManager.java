package com.knowplugins.ysubz.kaoscore.utils.placeholder;

import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.managers.player.VanishManager;
import org.bukkit.entity.Player;

public class PlaceholderManager {

    public static String replacePlaceholders(String message, Player player) {
        if (message == null || player == null) {
            return message;
        }

        String playerNick = getPlayerNick(player);
        boolean isVanished = VanishManager.isVanished(player);

        message = message.replace("%kaoscore_player-nick%", playerNick);
        message = message.replace("%kaoscore_player-isvanish%", String.valueOf(isVanished));

        return message;
    }

    private static String getPlayerNick(Player player) {
        if (player == null) {
            return "Unknown";
        }

        DisguiseManager disguiseManager = Managers.getDisguiseManager();
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
            DisguiseManager disguiseManager = Managers.getDisguiseManager();
            if (disguiseManager != null) {
                String disguiseNick = disguiseManager.getDisguiseNick(player);
                if (disguiseNick != null) {
                    return disguiseNick;
                }
            }
        }

        return player.getName();
    }
}

