package com.knowplugins.ysubz.kaoscore.managers.plugin;

import com.knowplugins.ysubz.kaoscore.KaosCore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerAddressRepository {

    private final KaosCore plugin;
    private final DatabaseManager databaseManager;

    public PlayerAddressRepository(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void track(UUID uuid, String name, String ip) {
        if (uuid == null || name == null || name.trim().isEmpty() || ip == null || ip.trim().isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO player_addresses (player_uuid, player_name, ip, first_seen, last_seen) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), last_seen = VALUES(last_seen)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, ip);
            ps.setLong(4, now);
            ps.setLong(5, now);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao registrar IP do jogador no DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public CompletableFuture<List<String>> findAlts(String targetName, UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> result = new ArrayList<>();
            String normalizedName = targetName == null ? "" : targetName.trim().toLowerCase();
            String uuidValue = targetUuid == null ? "" : targetUuid.toString();

            if (normalizedName.isEmpty() && uuidValue.isEmpty()) {
                return result;
            }

            String sql = "SELECT DISTINCT player_name, player_uuid, MAX(last_seen) AS last_seen FROM player_addresses WHERE ip IN (SELECT DISTINCT ip FROM player_addresses WHERE LOWER(player_name) = ? OR player_uuid = ?) GROUP BY player_name, player_uuid ORDER BY last_seen DESC";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, normalizedName);
                ps.setString(2, uuidValue);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("player_name");
                        String playerUuid = rs.getString("player_uuid");
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
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao buscar alts no DB: " + e.getMessage());
                e.printStackTrace();
            }

            return result;
        });
    }
}
