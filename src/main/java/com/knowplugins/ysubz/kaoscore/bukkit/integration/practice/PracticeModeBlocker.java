package com.knowplugins.ysubz.kaoscore.bukkit.integration.practice;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class PracticeModeBlocker {

    private static final String DEFAULT_BLOCK_MESSAGE = "&cEsta funcao nao pode ser usada durante partidas do Practice.";
    private static final HardPracticeBridge PRACTICE_BRIDGE = new HardPracticeBridge();

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

            HardPracticeBridge.PracticeState state = PRACTICE_BRIDGE.getState(player);
            return state.isInMatch() || state.isSpectating() || PRACTICE_BRIDGE.isQueueing(player) || state.isInEvent();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isInActiveMatch(Player player) {
        if (player == null) {
            return false;
        }

        try {
            if (!PRACTICE_BRIDGE.isAvailable()) {
                return false;
            }

            HardPracticeBridge.PracticeState state = PRACTICE_BRIDGE.getState(player);
            return state.isInMatch() || state.isInEvent();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean check(Player player, boolean sendMessage) {
        boolean blocked = isBlocked(player);
        if (blocked && sendMessage) {
            player.sendMessage(getBlockMessage());
        }
        return blocked;
    }

    public static boolean checkActiveMatch(Player player, boolean sendMessage) {
        boolean blocked = isInActiveMatch(player);
        if (blocked && sendMessage) {
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
