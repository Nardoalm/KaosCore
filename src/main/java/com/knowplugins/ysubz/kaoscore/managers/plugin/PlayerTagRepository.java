package com.knowplugins.ysubz.kaoscore.managers.plugin;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerTagRepository {

    private final KaosCore plugin;
    private final DatabaseManager databaseManager;

    public PlayerTagRepository(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public Map<UUID, String> loadAll() {
        Map<UUID, String> result = new HashMap<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid, tag_id FROM player_tags")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String tagId = rs.getString("tag_id");
                        if (tagId != null && !tagId.trim().isEmpty()) {
                            result.put(uuid, tagId);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao carregar tags selecionadas do DB: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    public void saveAsync(UUID uuid, String tagId) {
        if (uuid == null || tagId == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("REPLACE INTO player_tags (uuid, tag_id) VALUES (?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, tagId);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao salvar tag selecionada no DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
