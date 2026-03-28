package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class BungeeConnectionListener implements Listener {

    private final BungeeKaosCore plugin;

    public BungeeConnectionListener(BungeeKaosCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        PendingConnection connection = event.getConnection();
        if (connection == null) {
            return;
        }

        String name = connection.getName();
        if (plugin.getWhitelist().isEnabled() && !plugin.getWhitelist().isAllowed(name, connection.getUniqueId())) {
            event.setCancelled(true);
            event.setCancelReason(plugin.getWhitelist().getKickMessage());
            return;
        }

        PunishmentEntry ban = plugin.getPunishments().getActiveBan(connection.getUniqueId(), name);
        if (ban != null) {
            event.setCancelled(true);
            event.setCancelReason(plugin.getPunishmentMessages().buildBanScreen(ban));
        }
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }

        InetSocketAddress address = player.getAddress();
        InetAddress host = address == null ? null : address.getAddress();
        String ip = host == null ? "" : host.getHostAddress();
        plugin.getPlayerAddresses().track(player.getUniqueId(), player.getName(), ip);
    }
}


