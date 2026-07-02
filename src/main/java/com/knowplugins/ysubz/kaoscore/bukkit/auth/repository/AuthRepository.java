package com.knowplugins.ysubz.kaoscore.bukkit.auth.repository;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.profile.AuthProfile;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.database.DatabaseManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AuthRepository {

    private final KaosCore plugin;

    public AuthRepository(KaosCore plugin) {
        this.plugin = plugin;
    }

    public void createTables() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager db = MainManager.getDatabaseManager();
            if (db == null || db.isMongo()) return;

            try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS kaoscore_auth (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "name VARCHAR(16) NOT NULL, " +
                        "password VARCHAR(64), " +
                        "is_premium TINYINT(1) NOT NULL DEFAULT 0, " +
                        "registered_at BIGINT NOT NULL, " +
                        "last_login BIGINT NOT NULL)");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<AuthProfile> loadProfile(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            AuthProfile profile = new AuthProfile(uuid, name);
            DatabaseManager db = MainManager.getDatabaseManager();

            if (db == null) return profile;

            if (db.isMongo()) {
                MongoCollection<Document> collection = db.getMongoDatabase().getCollection("kaoscore_auth");
                Document doc = collection.find(Filters.eq("name", name)).first();
                if (doc != null) {
                    profile.setPasswordHash(doc.getString("password"));
                    profile.setPremium(doc.getBoolean("is_premium", false));
                }
            } else {
                try (Connection conn = db.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT * FROM kaoscore_auth WHERE name = ?")) {
                    ps.setString(1, name);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            profile.setPasswordHash(rs.getString("password"));
                            profile.setPremium(rs.getBoolean("is_premium"));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return profile;
        });
    }

    public void saveProfile(AuthProfile profile) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            DatabaseManager db = MainManager.getDatabaseManager();
            if (db == null) return;

            long now = System.currentTimeMillis();

            if (db.isMongo()) {
                MongoCollection<Document> collection = db.getMongoDatabase().getCollection("kaoscore_auth");
                Document doc = new Document("uuid", profile.getUuid().toString())
                        .append("name", profile.getName())
                        .append("password", profile.getPasswordHash())
                        .append("is_premium", profile.isPremium())
                        .append("last_login", now);

                collection.replaceOne(Filters.eq("name", profile.getName()), doc, new ReplaceOptions().upsert(true));
            } else {
                try (Connection conn = db.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "INSERT INTO kaoscore_auth (uuid, name, password, is_premium, registered_at, last_login) VALUES (?, ?, ?, ?, ?, ?) " +
                                     "ON DUPLICATE KEY UPDATE password = ?, is_premium = ?, last_login = ?")) {

                    ps.setString(1, profile.getUuid().toString());
                    ps.setString(2, profile.getName());
                    ps.setString(3, profile.getPasswordHash());
                    ps.setBoolean(4, profile.isPremium());
                    ps.setLong(5, now);
                    ps.setLong(6, now);
                    ps.setString(7, profile.getPasswordHash());
                    ps.setBoolean(8, profile.isPremium());
                    ps.setLong(9, now);
                    ps.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}