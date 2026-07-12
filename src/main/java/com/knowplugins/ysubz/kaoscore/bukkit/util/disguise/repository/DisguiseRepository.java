package com.knowplugins.ysubz.kaoscore.bukkit.util.disguise.repository;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.database.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.Logger;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DisguiseRepository {

    private final KaosCore plugin;
    private final DatabaseManager databaseManager;

    public DisguiseRepository(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    private boolean isMongo() {
        return databaseManager.getType().equalsIgnoreCase("mongodb");
    }

    private MongoCollection<Document> getCollection() {
        return databaseManager.getMongoDatabase().getCollection("disguises");
    }

    public CompletableFuture<String> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (isMongo()) {
                try {
                    Document doc = getCollection().find(new Document("uuid", uuid.toString())).first();
                    if (doc != null && doc.getBoolean("is_disguised", false)) {
                        return doc.getString("disguise_meta");
                    }
                } catch (Exception e) {
                    Logger.warning("Erro ao carregar disguise do MongoDB: " + e.getMessage());
                }
                return null;
            }

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT is_disguised, disguise_meta FROM disguises WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getBoolean("is_disguised")) {
                            return rs.getString("disguise_meta");
                        }
                    }
                }
            } catch (Exception e) {
                Logger.warning("Erro ao carregar disguise do DB: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
    }

    public CompletableFuture<Map<UUID, String>> loadAllActive() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, String> result = new HashMap<>();
            if (isMongo()) {
                try {
                    for (Document doc : getCollection().find(new Document("is_disguised", true))) {
                        try {
                            UUID uuid = UUID.fromString(doc.getString("uuid"));
                            String meta = doc.getString("disguise_meta");
                            result.put(uuid, meta);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    Logger.warning("Erro ao carregar disguises ativos do MongoDB: " + e.getMessage());
                }
                return result;
            }

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT uuid, disguise_meta FROM disguises WHERE is_disguised = 1")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            String meta = rs.getString("disguise_meta");
                            result.put(uuid, meta);
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                Logger.warning("Erro ao carregar disguises ativos do DB: " + e.getMessage());
                e.printStackTrace();
            }
            return result;
        });
    }

    public void saveAsync(UUID uuid, boolean isDisguised, String type, String meta) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isMongo()) {
                try {
                    Document updateDoc = new Document("is_disguised", isDisguised)
                            .append("disguise_type", type)
                            .append("disguise_meta", meta);

                    getCollection().updateOne(
                            new Document("uuid", uuid.toString()),
                            new Document("$set", updateDoc),
                            new UpdateOptions().upsert(true)
                    );
                } catch (Exception e) {
                    Logger.warning("Erro ao salvar disguise no MongoDB: " + e.getMessage());
                }
                return;
            }

            try (Connection conn = databaseManager.getConnection()) {
                String query = "INSERT INTO disguises (uuid, is_disguised, disguise_type, disguise_meta) VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE is_disguised = VALUES(is_disguised), disguise_type = VALUES(disguise_type), disguise_meta = VALUES(disguise_meta)";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, uuid.toString());
                    ps.setBoolean(2, isDisguised);
                    ps.setString(3, type);
                    ps.setString(4, meta);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                Logger.warning("Erro ao salvar disguise no DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}