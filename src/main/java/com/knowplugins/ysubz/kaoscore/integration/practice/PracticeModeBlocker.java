package com.knowplugins.ysubz.kaoscore.integration.practice;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class PracticeModeBlocker {

    private static final String DEFAULT_BLOCK_MESSAGE = "&cEsta funcao nao pode ser usada durante partidas do Practice.";
    private static final KaosPracticeBridge PRACTICE_BRIDGE = new KaosPracticeBridge();

    private PracticeModeBlocker() {
    }

    public static boolean isBlocked(Player player) {
        if (player == null) {
            return false;
        }

        try {
            if (!PRACTICE_BRIDGE.isAvailable()) {
                return false;
            }

            KaosPracticeBridge.PracticeState state = PRACTICE_BRIDGE.getState(player);
            return state.isInMatch() || state.isSpectating() || PRACTICE_BRIDGE.isQueueing(player);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean check(Player player, boolean sendMessage) {
        boolean blocked = isBlocked(player);
        if (blocked && sendMessage && player != null) {
            player.sendMessage(getBlockMessage());
        }
        return blocked;
    }

    public static String getBlockMessage() {
        KaosCore plugin = KaosCore.getInstance();
        String message = plugin == null
                ? DEFAULT_BLOCK_MESSAGE
                : plugin.getConfig().getString("practice-blocks.message", DEFAULT_BLOCK_MESSAGE);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
