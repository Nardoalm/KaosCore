package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class BungeeWhitelistService {

    private final BungeeKaosCore plugin;
    private final File file;
    private boolean enabled;
    private String kickMessage;
    private final Set<String> allowedNames = new HashSet<>();
    private final Set<UUID> allowedUuids = new HashSet<>();

    BungeeWhitelistService(BungeeKaosCore plugin, File file, Configuration configuration) {
        this.plugin = plugin;
        this.file = file;
        reload(configuration);
    }

    void reload(Configuration configuration) {
        allowedNames.clear();
        allowedUuids.clear();
        enabled = configuration.getBoolean("enabled", false);
        List<String> lines = configuration.getStringList("kick-message");
        if (lines == null || lines.isEmpty()) {
            lines = Collections.singletonList(configuration.getString("kick-message", "&cServidor em whitelist."));
        }
        kickMessage = Texts.color(String.join("\n", lines));
        for (String name : configuration.getStringList("players")) {
            String normalized = Texts.normalize(name);
            if (!normalized.isEmpty()) {
                allowedNames.add(normalized);
            }
        }
        for (String raw : configuration.getStringList("uuids")) {
            try {
                if (raw != null && !raw.trim().isEmpty()) {
                    allowedUuids.add(UUID.fromString(raw.trim()));
                }
            } catch (Exception ignored) {
            }
        }
    }

    boolean isEnabled() {
        return enabled;
    }

    String getKickMessage() {
        return kickMessage == null ? "§cServidor em whitelist." : kickMessage;
    }

    boolean isAllowed(String playerName, UUID uuid) {
        if (allowedNames.contains(Texts.normalize(playerName))) {
            return true;
        }
        return uuid != null && allowedUuids.contains(uuid);
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    boolean addPlayer(ProxiedPlayer player) {
        if (player == null) {
            return false;
        }
        boolean changed = false;
        String normalized = Texts.normalize(player.getName());
        if (!normalized.isEmpty()) {
            changed |= allowedNames.add(normalized);
        }
        changed |= allowedUuids.add(player.getUniqueId());
        if (changed) {
            save();
        }
        return changed;
    }

    boolean addName(String name) {
        String normalized = Texts.normalize(name);
        if (normalized.isEmpty()) {
            return false;
        }
        boolean changed = allowedNames.add(normalized);
        if (changed) {
            save();
        }
        return changed;
    }

    boolean removeName(String name) {
        String normalized = Texts.normalize(name);
        if (normalized.isEmpty()) {
            return false;
        }
        boolean changed = allowedNames.remove(normalized);
        if (changed) {
            save();
        }
        return changed;
    }

    Set<String> getAllowedNames() {
        return new HashSet<>(allowedNames);
    }

    Set<UUID> getAllowedUuids() {
        return new HashSet<>(allowedUuids);
    }

    private void save() {
        try {
            Configuration configuration = new Configuration();
            configuration.set("enabled", enabled);
            configuration.set("kick-message", new ArrayList<>(Arrays.asList(getKickMessage().split("\n"))));
            configuration.set("players", new ArrayList<>(allowedNames));
            List<String> uuids = new ArrayList<>();
            for (UUID id : allowedUuids) {
                uuids.add(id.toString());
            }
            configuration.set("uuids", uuids);
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao salvar whitelist: " + exception.getMessage());
        }
    }
}

