package com.knowplugins.ysubz.kaoscore.managers.plugin.repository;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DatabaseManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
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

    private boolean isMongo() {
        return databaseManager.getType().equalsIgnoreCase("mongodb");
    }

    private MongoCollection<Document> getCollection() {
        return databaseManager.getMongoDatabase().getCollection("player_tags");
    }

    public Map<UUID, String> loadAll() {
        Map<UUID, String> result = new HashMap<>();

        if (isMongo()) {
            try {
                for (Document doc : getCollection().find()) {
                    try {
                        UUID uuid = UUID.fromString(doc.getString("uuid"));
                        String tagId = doc.getString("tag_id");
                        if (tagId != null && !tagId.trim().isEmpty()) {
                            result.put(uuid, tagId);
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar tags selecionadas do MongoDB: " + e.getMessage());
            }
            return result;
        }

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
            if (isMongo()) {
                try {
                    Document doc = new Document("uuid", uuid.toString()).append("tag_id", tagId);
                    getCollection().replaceOne(new Document("uuid", uuid.toString()), doc, new ReplaceOptions().upsert(true));
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao salvar tag selecionada no MongoDB: " + e.getMessage());
                }
                return;
            }

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

    public void deleteAsync(UUID uuid) {
        if (uuid == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isMongo()) {
                try {
                    getCollection().deleteOne(new Document("uuid", uuid.toString()));
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao remover tag selecionada no MongoDB: " + e.getMessage());
                }
                return;
            }

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM player_tags WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao remover tag selecionada no DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}