package com.knowplugins.ysubz.kaoscore.managers.plugin;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerWelcomeMessageRepository {

    private final KaosCore plugin;
    private final DatabaseManager databaseManager;

    public PlayerWelcomeMessageRepository(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public Map<UUID, String> loadAll() {
        Map<UUID, String> result = new HashMap<>();

        if (databaseManager == null) {
            return result;
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid, message_id FROM player_welcome_messages")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String messageId = rs.getString("message_id");
                        if (messageId != null && !messageId.trim().isEmpty()) {
                            result.put(uuid, messageId);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("Erro ao carregar welcome messages selecionadas do DB: " + e.getMessage());
            }
            e.printStackTrace();
        }

        return result;
    }

    public void saveAsync(UUID uuid, String messageId) {
        if (uuid == null || messageId == null || databaseManager == null || plugin == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("REPLACE INTO player_welcome_messages (uuid, message_id) VALUES (?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, messageId);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao salvar welcome message selecionada no DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}

