package com.knowplugins.ysubz.kaoscore.managers.plugin;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SkinRepository {
    private final KaosCore plugin;
    private final DatabaseManager databaseManager;

    public SkinRepository(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    private boolean isMongo() {
        return databaseManager.getType().equalsIgnoreCase("mongodb");
    }

    private MongoCollection<Document> getCollection() {
        return databaseManager.getMongoDatabase().getCollection("skins");
    }

    public CompletableFuture<SkinData> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (isMongo()) {
                try {
                    Document doc = getCollection().find(new Document("uuid", uuid.toString())).first();
                    if (doc != null) {
                        return new SkinData(doc.getString("skin_name"), doc.getString("skin_value"), doc.getString("skin_signature"));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao carregar skin do MongoDB: " + e.getMessage());
                }
                return null;
            }

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT skin_name, skin_value, skin_signature FROM skins WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String name = rs.getString("skin_name");
                        String value = rs.getString("skin_value");
                        String signature = rs.getString("skin_signature");
                        return new SkinData(name, value, signature);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar skin do DB: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
    }

    public void saveAsync(UUID uuid, String skinName, String value, String signature) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isMongo()) {
                try {
                    Document doc = new Document("uuid", uuid.toString())
                            .append("skin_name", skinName)
                            .append("skin_value", value)
                            .append("skin_signature", signature)
                            .append("fetched_at", System.currentTimeMillis());
                    getCollection().replaceOne(new Document("uuid", uuid.toString()), doc, new ReplaceOptions().upsert(true));
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao salvar skin no MongoDB: " + e.getMessage());
                }
                return;
            }

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("REPLACE INTO skins (uuid, skin_name, skin_value, skin_signature, fetched_at) VALUES (?,?,?,?,CURRENT_TIMESTAMP)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, skinName);
                ps.setString(3, value);
                ps.setString(4, signature);
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao salvar skin no DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void deleteAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isMongo()) {
                try {
                    getCollection().deleteOne(new Document("uuid", uuid.toString()));
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao deletar skin do MongoDB: " + e.getMessage());
                }
                return;
            }

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM skins WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao deletar skin do DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}