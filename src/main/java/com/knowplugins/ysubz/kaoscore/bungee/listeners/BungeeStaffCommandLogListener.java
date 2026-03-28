package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class BungeeStaffCommandLogListener implements Listener {

    private final BungeeKaosCore plugin;

    public BungeeStaffCommandLogListener(BungeeKaosCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStaffCommand(ChatEvent event) {
        if (!event.isCommand()) {
            return;
        }
        if (!(event.getSender() instanceof CommandSender)) {
            return;
        }

        String message = Texts.safe(event.getMessage()).trim();
        if (message.isEmpty() || !plugin.getWebhookManager().shouldLogStaffCommand(message)) {
            return;
        }

        plugin.getWebhookManager().logStaffCommand((CommandSender) event.getSender(), message);
    }
}


