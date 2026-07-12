package com.knowplugins.ysubz.kaoscore.bukkit.util.skin.repository;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.database.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.plugin.SkinData;
import com.knowplugins.ysubz.kaoscore.bukkit.util.Logger;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
                    Logger.warning("Erro ao carregar skin do MongoDB: " + e.getMessage());
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
                Logger.warning("Erro ao carregar skin do DB: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
    }

    public void saveAsync(UUID uuid, String skinName, String value, String signature) {
        if (uuid == null) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            if (DisguiseManager.isDisguised(player)) {
                return;
            }

            DisguiseManager disguiseManager = MainManager.getDisguiseManager();
            if (disguiseManager != null) {
                String orig = disguiseManager.getOriginalName(player);
                String fake = disguiseManager.getDisguiseNick(player);

                if (orig != null && !player.getName().equals(orig)) {
                    return;
                }

                if (fake != null && player.getName().equals(fake)) {
                    return;
                }
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isMongo()) {
                try {
                    Document updateDoc = new Document("skin_name", skinName)
                            .append("skin_value", value)
                            .append("skin_signature", signature)
                            .append("fetched_at", System.currentTimeMillis());

                    getCollection().updateOne(
                            new Document("uuid", uuid.toString()),
                            new Document("$set", updateDoc),
                            new UpdateOptions().upsert(true)
                    );
                } catch (Exception e) {
                    Logger.warning("Erro ao salvar skin no MongoDB: " + e.getMessage());
                }
                return;
            }

            try (Connection conn = databaseManager.getConnection()) {
                String query = "INSERT INTO skins (uuid, skin_name, skin_value, skin_signature, fetched_at) VALUES (?,?,?,?,CURRENT_TIMESTAMP) " +
                        "ON DUPLICATE KEY UPDATE skin_name = VALUES(skin_name), skin_value = VALUES(skin_value), skin_signature = VALUES(skin_signature), fetched_at = CURRENT_TIMESTAMP";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, skinName);
                    ps.setString(3, value);
                    ps.setString(4, signature);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                Logger.warning("Erro ao salvar skin no DB: " + e.getMessage());
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
                    Logger.warning("Erro ao deletar skin do MongoDB: " + e.getMessage());
                }
                return;
            }

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM skins WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                Logger.warning("Erro ao deletar skin do DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}