package com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.repository;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.database.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.SkinData;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class SkinCacheRepository {

    private final KaosCore plugin;
    private final DatabaseManager databaseManager;

    public SkinCacheRepository(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    private boolean isMongo() {
        return databaseManager.getType().equalsIgnoreCase("mongodb");
    }

    private MongoCollection<Document> getCollection() {
        return databaseManager.getMongoDatabase().getCollection("skin_cache");
    }

    public CompletableFuture<SkinData> load(String nick) {
        String clean = normalizeNick(nick);
        if (clean.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> loadSync(clean));
    }

    public SkinData loadSync(String nick) {
        String clean = normalizeNick(nick);
        if (clean.isEmpty()) {
            return null;
        }

        if (isMongo()) {
            try {
                Document doc = getCollection().find(new Document("nick", clean)).first();
                if (doc != null) {
                    return new SkinData(clean, doc.getString("skin_value"), doc.getString("skin_signature"));
                }
            } catch (Exception e) {
                if (plugin != null) {
                    plugin.getLogger().warning("[KaosCore] Erro ao carregar skin cache do MongoDB: " + e.getMessage());
                }
            }
            return null;
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT skin_value, skin_signature FROM kaoscore_skin_cache WHERE nick = ?")) {
            ps.setString(1, clean);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String value = rs.getString("skin_value");
                    String signature = rs.getString("skin_signature");
                    return new SkinData(clean, value, signature);
                }
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("[KaosCore] Erro ao carregar skin cache: " + e.getMessage());
            }
            e.printStackTrace();
        }
        return null;
    }

    public void saveAsync(String nick, String value, String signature) {
        String clean = normalizeNick(nick);
        if (clean.isEmpty() || value == null || value.trim().isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isMongo()) {
                try {
                    Document updateDoc = new Document("skin_value", value)
                            .append("skin_signature", signature);
                    getCollection().updateOne(
                            new Document("nick", clean),
                            new Document("$set", updateDoc),
                            new UpdateOptions().upsert(true)
                    );
                } catch (Exception e) {
                    if (plugin != null) {
                        plugin.getLogger().warning("[KaosCore] Erro ao salvar skin cache no MongoDB: " + e.getMessage());
                    }
                }
                return;
            }

            try (Connection conn = databaseManager.getConnection()) {
                String query = "INSERT INTO kaoscore_skin_cache (nick, skin_value, skin_signature) VALUES (?,?,?) " +
                        "ON DUPLICATE KEY UPDATE skin_value = VALUES(skin_value), skin_signature = VALUES(skin_signature)";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, clean);
                    ps.setString(2, value);
                    ps.setString(3, signature);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                if (plugin != null) {
                    plugin.getLogger().warning("[KaosCore] Erro ao salvar skin cache: " + e.getMessage());
                }
                e.printStackTrace();
            }
        });
    }

    public List<SkinData> loadAnyPoolSync(int limit) {
        int lim = limit <= 0 ? 64 : Math.min(limit, 512);
        List<SkinData> out = new ArrayList<>();

        if (isMongo()) {
            try {
                for (Document doc : getCollection().find().limit(lim)) {
                    String nick = doc.getString("nick");
                    String value = doc.getString("skin_value");
                    String signature = doc.getString("skin_signature");
                    if (nick != null && value != null && !value.trim().isEmpty()) {
                        out.add(new SkinData(nick, value, signature));
                    }
                }
            } catch (Exception e) {
                if (plugin != null) {
                    plugin.getLogger().warning("[KaosCore] Erro ao carregar pool de skins do MongoDB: " + e.getMessage());
                }
            }
            return out;
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT nick, skin_value, skin_signature FROM kaoscore_skin_cache LIMIT " + lim)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nick = rs.getString("nick");
                    String value = rs.getString("skin_value");
                    String signature = rs.getString("skin_signature");
                    if (nick != null && value != null && !value.trim().isEmpty()) {
                        out.add(new SkinData(nick, value, signature));
                    }
                }
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("[KaosCore] Erro ao carregar pool de skins do cache: " + e.getMessage());
            }
        }

        return out;
    }

    private String normalizeNick(String nick) {
        if (nick == null) {
            return "";
        }
        String s = nick.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return "";
        }
        if (s.length() > 16) {
            s = s.substring(0, 16);
        }
        return s;
    }
}