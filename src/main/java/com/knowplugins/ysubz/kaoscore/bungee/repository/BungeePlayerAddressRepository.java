package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class BungeePlayerAddressRepository {

    private final BungeeKaosCore plugin;
    private final BungeeDatabase database;
    private final Set<String> knownNames = ConcurrentHashMap.newKeySet();

    BungeePlayerAddressRepository(BungeeKaosCore plugin, BungeeDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    void track(UUID uuid, String name, String ip) {
        if (uuid == null || Texts.normalize(name).isEmpty() || Texts.safe(ip).trim().isEmpty()) {
            return;
        }
        knownNames.add(Texts.normalize(name));
        long now = System.currentTimeMillis();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO player_addresses (player_uuid, player_name, ip, first_seen, last_seen) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), last_seen = VALUES(last_seen)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.setString(3, ip);
            statement.setLong(4, now);
            statement.setLong(5, now);
            statement.executeUpdate();
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao registrar IP: " + exception.getMessage());
        }
    }

    boolean hasSeenName(String name) {
        String normalized = Texts.normalize(name);
        if (normalized.isEmpty()) {
            return false;
        }
        if (knownNames.contains(normalized)) {
            return true;
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM player_addresses WHERE LOWER(player_name) = ? LIMIT 1")) {
            statement.setString(1, normalized);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    knownNames.add(normalized);
                    return true;
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao verificar historico de nome: " + exception.getMessage());
        }
        return false;
    }

    UUID findUuidByName(String name) {
        String normalized = Texts.normalize(name);
        if (normalized.isEmpty()) {
            return null;
        }

        ProxiedPlayer online = ProxyServer.getInstance().getPlayer(name);
        if (online != null) {
            return online.getUniqueId();
        }

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT player_uuid FROM player_addresses WHERE LOWER(player_name) = ? ORDER BY last_seen DESC LIMIT 1")) {
            statement.setString(1, normalized);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String value = resultSet.getString("player_uuid");
                    if (value != null && !value.trim().isEmpty()) {
                        return UUID.fromString(value);
                    }
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao resolver UUID por nome: " + exception.getMessage());
        }
        return null;
    }

    List<String> findAlts(String targetName, UUID targetUuid) {
        List<String> result = new ArrayList<>();
        String normalizedName = Texts.normalize(targetName);
        String uuidValue = targetUuid == null ? "" : targetUuid.toString();
        if (normalizedName.isEmpty() && uuidValue.isEmpty()) {
            return result;
        }

        String sql = "SELECT DISTINCT player_name, player_uuid, MAX(last_seen) AS last_seen FROM player_addresses WHERE ip IN (SELECT DISTINCT ip FROM player_addresses WHERE LOWER(player_name) = ? OR player_uuid = ?) GROUP BY player_name, player_uuid ORDER BY last_seen DESC";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedName);
            statement.setString(2, uuidValue);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String name = resultSet.getString("player_name");
                    String playerUuid = resultSet.getString("player_uuid");
                    if (name == null || name.trim().isEmpty()) {
                        continue;
                    }
                    if (!uuidValue.isEmpty() && uuidValue.equalsIgnoreCase(playerUuid)) {
                        continue;
                    }
                    if (!normalizedName.isEmpty() && normalizedName.equalsIgnoreCase(name)) {
                        continue;
                    }
                    if (!result.contains(name)) {
                        result.add(name);
                    }
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao buscar alts: " + exception.getMessage());
        }
        return result;
    }

    String findLatestName(UUID uuid) {
        if (uuid == null) {
            return "Desconhecido";
        }
        ProxiedPlayer online = ProxyServer.getInstance().getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT player_name FROM player_addresses WHERE player_uuid = ? ORDER BY last_seen DESC LIMIT 1")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("player_name");
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao resolver nome por UUID: " + exception.getMessage());
        }
        return "Desconhecido";
    }
}


