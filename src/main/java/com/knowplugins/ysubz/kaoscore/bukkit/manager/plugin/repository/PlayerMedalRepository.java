package com.knowplugins.ysubz.kaoscore.bukkit.manager.plugin.repository;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.database.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.Logger;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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

    private boolean isMongo() {
        return databaseManager != null && databaseManager.getType().equalsIgnoreCase("mongodb");
    }

    private MongoCollection<Document> getCollection() {
        return databaseManager.getMongoDatabase().getCollection("player_medals");
    }

    public Map<UUID, String> loadAll() {
        Map<UUID, String> result = new HashMap<>();

        if (databaseManager == null) {
            return result;
        }

        if (isMongo()) {
            try {
                for (Document doc : getCollection().find()) {
                    try {
                        UUID uuid = UUID.fromString(doc.getString("uuid"));
                        String medalId = doc.getString("medal_id");
                        if (medalId != null && !medalId.trim().isEmpty()) {
                            result.put(uuid, medalId);
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                Logger.warning("Erro ao carregar medalhas selecionadas do MongoDB: " + e.getMessage());
            }
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
            Logger.warning("Erro ao carregar medalhas selecionadas do DB: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    public void saveAsync(UUID uuid, String medalId) {
        if (uuid == null || medalId == null || databaseManager == null) {
            return;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && DisguiseManager.isDisguised(player)) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isMongo()) {
                try {
                    Document updateDoc = new Document("medal_id", medalId);
                    getCollection().updateOne(
                            new Document("uuid", uuid.toString()),
                            new Document("$set", updateDoc),
                            new UpdateOptions().upsert(true)
                    );
                } catch (Exception e) {
                    Logger.warning("Erro ao salvar medalha selecionada no MongoDB: " + e.getMessage());
                }
                return;
            }

            try (Connection conn = databaseManager.getConnection()) {
                String query = "INSERT INTO player_medals (uuid, medal_id) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE medal_id = VALUES(medal_id)";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, medalId);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                Logger.warning("Erro ao salvar medalha selecionada no DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void deleteAsync(UUID uuid) {
        if (uuid == null || databaseManager == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isMongo()) {
                try {
                    getCollection().deleteOne(new Document("uuid", uuid.toString()));
                } catch (Exception e) {
                    Logger.warning("Erro ao remover medalha selecionada no MongoDB: " + e.getMessage());
                }
                return;
            }

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM player_medals WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                Logger.warning("Erro ao remover medalha selecionada no DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}