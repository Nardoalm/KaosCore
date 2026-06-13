package com.knowplugins.ysubz.kaoscore.bungee.services;

import com.knowplugins.ysubz.kaoscore.bungee.Texts;
import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BungeeWhitelistService {

    private final BungeeKaosCore plugin;
    private final File file;
    private final Set<String> allowedNames = ConcurrentHashMap.newKeySet();
    private volatile boolean enabled;
    private volatile String kickMessage = "§cServidor em whitelist.";

    public BungeeWhitelistService(BungeeKaosCore plugin, File file, Configuration whitelistConfig) {
        this.plugin = plugin;
        this.file = file;
        reload(whitelistConfig);
    }

    public synchronized void reload(Configuration whitelistConfig) {
        allowedNames.clear();
        if (whitelistConfig != null) {
            this.enabled = whitelistConfig.getBoolean("enabled", false);
            this.kickMessage = Texts.safe(whitelistConfig.getString("kick-message", kickMessage));
            for (String name : whitelistConfig.getStringList("players")) {
                String normalized = Texts.normalize(name);
                if (!normalized.isEmpty()) {
                    allowedNames.add(normalized);
                }
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public boolean addPlayer(ProxiedPlayer player) {
        if (player == null) {
            return false;
        }
        return addName(player.getName());
    }

    public boolean addName(String name) {
        String normalized = Texts.normalize(name);
        if (normalized.isEmpty()) {
            return false;
        }
        return allowedNames.add(normalized);
    }

    public boolean removeName(String name) {
        String normalized = Texts.normalize(name);
        if (normalized.isEmpty()) {
            return false;
        }
        return allowedNames.remove(normalized);
    }

    public List<String> getAllowedNames() {
        return new ArrayList<>(allowedNames);
    }

    public boolean isAllowed(String name, UUID uuid) {
        if (!enabled) {
            return true;
        }
        String normalized = Texts.normalize(name);
        return !normalized.isEmpty() && allowedNames.contains(normalized);
    }
}
