package com.knowplugins.ysubz.kaoscore.managers.plugin;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMedalRepository {

    private final KaosCore plugin;
    private final DatabaseManager databaseManager;

    public PlayerMedalRepository(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public Map<UUID, String> loadAll() {
        Map<UUID, String> result = new HashMap<>();

        if (databaseManager == null) {
            return result;
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid, medal_id FROM player_medals")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String medalId = rs.getString("medal_id");
                        if (medalId != null && !medalId.trim().isEmpty()) {
                            result.put(uuid, medalId);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao carregar medalhas selecionadas do DB: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    public void saveAsync(UUID uuid, String medalId) {
        if (uuid == null || medalId == null || databaseManager == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("REPLACE INTO player_medals (uuid, medal_id) VALUES (?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, medalId);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao salvar medalha selecionada no DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void deleteAsync(UUID uuid) {
        if (uuid == null || databaseManager == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM player_medals WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao remover medalha selecionada no DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
