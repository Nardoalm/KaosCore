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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VanishRepository {

    private final KaosCore plugin;
    private final DatabaseManager databaseManager;

    public VanishRepository(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    private boolean isMongo() {
        return databaseManager.getType().equalsIgnoreCase("mongodb");
    }

    private MongoCollection<Document> getCollection() {
        return databaseManager.getMongoDatabase().getCollection("vanishes");
    }

    public CompletableFuture<Boolean> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (isMongo()) {
                try {
                    Document doc = getCollection().find(new Document("uuid", uuid.toString())).first();
                    if (doc != null) {
                        return doc.getBoolean("is_vanished", false);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao carregar vanish do MongoDB: " + e.getMessage());
                }
                return false;
            }

            Connection conn = null;
            try {
                conn = databaseManager.getConnection();
                if (conn == null || conn.isClosed()) {
                    plugin.getLogger().warning("DB connection was closed, attempting reconnect...");
                    databaseManager.connect();
                    conn = databaseManager.getConnection();
                    if (conn == null || conn.isClosed()) {
                        plugin.getLogger().warning("Failed to obtain DB connection for vanish load");
                        return false;
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("SELECT is_vanished FROM vanishes WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getBoolean("is_vanished");
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar vanish do DB: " + e.getMessage());
                e.printStackTrace();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                try {
                    if (conn != null && !conn.isClosed()) conn.close();
                } catch (Exception ignored) {}
            }
            return false;
        });
    }

    public void saveAsync(UUID uuid, boolean isVanished) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isMongo()) {
                try {
                    Document doc = new Document("uuid", uuid.toString()).append("is_vanished", isVanished);
                    getCollection().replaceOne(new Document("uuid", uuid.toString()), doc, new ReplaceOptions().upsert(true));
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao salvar vanish no MongoDB: " + e.getMessage());
                }
                return;
            }

            try (Connection conn = databaseManager.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("REPLACE INTO vanishes (uuid,is_vanished) VALUES (?,?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setBoolean(2, isVanished);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao salvar vanish no DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}