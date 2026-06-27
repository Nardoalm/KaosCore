package com.knowplugins.ysubz.kaoscore.bukkit.listeners.player;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.DiscordWebhookManager;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class StaffLogListener implements Listener {

    private final KaosCore plugin;

    public StaffLogListener(KaosCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }

        DiscordWebhookManager webhookManager = plugin == null ? null : plugin.getWebhookManager();
        if (webhookManager == null) {
            return;
        }

        String message = event.getMessage();
        String label = firstLabel(message);
        if (!webhookManager.shouldLogStaffCommand(label)) {
            return;
        }

        webhookManager.logStaffCommand(event.getPlayer(), message);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        if (event == null || event.getSender() == null) {
            return;
        }

        if (!(event.getSender() instanceof ConsoleCommandSender)) {
            return;
        }

        DiscordWebhookManager webhookManager = plugin == null ? null : plugin.getWebhookManager();
        if (webhookManager == null) {
            return;
        }

        String command = event.getCommand();
        String label = firstLabel(command);
        if (!webhookManager.shouldLogStaffCommand(label)) {
            return;
        }

        String fullLine = "/" + (command == null ? "" : command);
        webhookManager.logStaffCommand(event.getSender(), fullLine);
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

        return text.trim();
    }
}

