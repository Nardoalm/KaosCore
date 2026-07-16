package com.knowplugins.ysubz.kaoscore.bukkit.whitelist;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.util.Logger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class WhitelistManager {

    private final KaosCore plugin;
    private final File file;

    private boolean enabled;
    private String kickMessage;
    private final Set<String> allowedNames;
    private final Set<UUID> allowedUuids;

    public WhitelistManager(KaosCore plugin) {
        this.plugin = plugin;
        this.file = plugin == null ? null : new File(plugin.getDataFolder(), "whitelist.yml");
        this.allowedNames = new HashSet<>();
        this.allowedUuids = new HashSet<>();
        load();
    }

    public void load() {
        allowedNames.clear();
        allowedUuids.clear();

        if (plugin == null || file == null) {
            enabled = false;
            kickMessage = "§cServer in maintenance.";
            return;
        }

        if (!file.exists()) {
            try {
                plugin.saveResource("whitelist.yml", false);
            } catch (Exception ignored) {
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        enabled = config.getBoolean("enabled", false);

        List<String> messageLines = config.getStringList("kick-message");
        if (messageLines == null || messageLines.isEmpty()) {
            String single = config.getString("kick-message", "&cServer in maintenance.");
            messageLines = Collections.singletonList(single);
        }
        kickMessage = color(String.join("\n", messageLines));

        List<String> players = config.getStringList("players");
        if (players == null || players.isEmpty()) {
            // Backward compatibility if someone uses "names".
            players = config.getStringList("names");
        }
        if (players != null) {
            for (String name : players) {
                String normalized = normalize(name);
                if (!normalized.isEmpty()) {
                    allowedNames.add(normalized);
                }
            }
        }

        List<String> uuids = config.getStringList("uuids");
        if (uuids != null) {
            for (String raw : uuids) {
                try {
                    if (raw != null && !raw.trim().isEmpty()) {
                        allowedUuids.add(UUID.fromString(raw.trim()));
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void save() {
        if (plugin == null || file == null) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("enabled", enabled);
        config.set("kick-message", toLines(kickMessage));

        List<String> players = new ArrayList<>(allowedNames);
        players.sort(String.CASE_INSENSITIVE_ORDER);
        config.set("players", players);

        List<String> uuids = new ArrayList<>();
        for (UUID id : allowedUuids) {
            if (id != null) {
                uuids.add(id.toString());
            }
        }
        uuids.sort(String.CASE_INSENSITIVE_ORDER);
        config.set("uuids", uuids);

        try {
            config.save(file);
        } catch (IOException e) {
            Logger.warning("Nao foi possivel salvar whitelist.yml: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public String getKickMessage() {
        return kickMessage == null ? "§cServer in maintenance." : kickMessage;
    }

    public boolean isAllowed(String playerName, UUID uuid) {
        if (playerName != null && allowedNames.contains(normalize(playerName))) {
            return true;
        }
        return uuid != null && allowedUuids.contains(uuid);
    }

    public boolean isAllowed(String playerName){
        if(playerName == null || !allowedNames.contains(normalize(playerName))){
            return false;
        }

        return true;
    }

    public Set<String> getAllowedNames() {
        return allowedNames;
    }

    public boolean add(Player player) {
        if (player == null) {
            return false;
        }

        boolean changed = false;
        String normalized = normalize(player.getName());
        if (!normalized.isEmpty()) {
            changed |= allowedNames.add(normalized);
        }
        changed |= allowedUuids.add(player.getUniqueId());

        if (changed) {
            save();
        }
        return changed;
    }

    public boolean addName(String name) {
        String normalized = normalize(name);
        if (normalized.isEmpty()) {
            return false;
        }
        boolean changed = allowedNames.add(normalized);
        if (changed) {
            save();
        }
        return changed;
    }

    public boolean removeName(String name) {
        String normalized = normalize(name);
        if (normalized.isEmpty()) {
            return false;
        }
        boolean changed = allowedNames.remove(normalized);
        if (changed) {
            save();
        }
        return changed;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private List<String> toLines(String text) {
        String value = text == null ? "" : text;
        if (value.contains("\n")) {
            String[] parts = value.split("\n");
            List<String> list = new ArrayList<>();
            Collections.addAll(list, parts);
            return list;
        }
        return Collections.singletonList(value);
    }
}

