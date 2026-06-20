package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public final class TargetResult {

    private final UUID uuid;
    private final String name;
    private final ProxiedPlayer player;

    public TargetResult(UUID uuid, String name, ProxiedPlayer player) {
        this.uuid = uuid;
        this.name = name == null ? "" : name;
        this.player = player;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public ProxiedPlayer getPlayer() {
        return player;
    }

    public static TargetResult resolve(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new TargetResult(null, "", null);
        }

        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(input);
        if (player != null) {
            return new TargetResult(player.getUniqueId(), player.getName(), player);
        }

        return new TargetResult(null, input.trim(), null);
    }
}


