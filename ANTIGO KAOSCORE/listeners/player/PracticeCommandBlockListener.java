package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.integration.practice.PracticeModeBlocker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PracticeCommandBlockListener implements Listener {

    private static final List<String> DEFAULT_BLOCKED_COMMANDS = Arrays.asList(
            "fly", "voar", "f", "sit", "disguise", "skin", "dinnerbone", "troll",
            "clan", "cc", "friend", "friends", "welcomemessage", "wm", "disclose"
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }

        if (!PracticeModeBlocker.isInActiveMatch(event.getPlayer())) {
            return;
        }

        String label = firstLabel(event.getMessage());
        if (label.isEmpty() || !getBlockedCommands().contains(label)) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(PracticeModeBlocker.getBlockMessage());
    }

    private Set<String> getBlockedCommands() {
        KaosCore plugin = KaosCore.getInstance();
        List<String> configured = plugin == null
                ? DEFAULT_BLOCKED_COMMANDS
                : plugin.getConfig().getStringList("practice-blocks.commands");

        List<String> source = configured == null || configured.isEmpty() ? DEFAULT_BLOCKED_COMMANDS : configured;
        Set<String> commands = new HashSet<>();
        for (String entry : source) {
            String normalized = normalize(entry);
            if (!normalized.isEmpty()) {
                commands.add(normalized);
            }
        }
        return commands;
    }

    private String firstLabel(String commandLine) {
        if (commandLine == null) {
            return "";
        }

        String text = commandLine.trim();
        if (text.startsWith("/")) {
            text = text.substring(1);
        }

        int space = text.indexOf(' ');
        if (space >= 0) {
            text = text.substring(0, space);
        }

        return normalize(text);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
