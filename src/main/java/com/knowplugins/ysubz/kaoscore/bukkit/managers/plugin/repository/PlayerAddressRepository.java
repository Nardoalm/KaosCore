package com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.repository;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.DatabaseManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerAddressRepository {

    private final KaosCore plugin;
    private final DatabaseManager databaseManager;
    private final java.util.Set<String> knownNames = ConcurrentHashMap.newKeySet();

    public PlayerAddressRepository(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    private boolean isMongo() {
        return databaseManager.getType().equalsIgnoreCase("mongodb");
    }

    private MongoCollection<Document> getCollection() {
        return databaseManager.getMongoDatabase().getCollection("player_addresses");
    }

    public void track(UUID uuid, String name, String ip) {
        if (uuid == null || name == null || name.trim().isEmpty() || ip == null || ip.trim().isEmpty()) {
            return;
        }

        String finalName = name;
        DisguiseManager disguiseManager = Managers.getDisguiseManager();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(uuid)) {
                if (DisguiseManager.isDisguised(online) && disguiseManager != null) {
                    String original = disguiseManager.getOriginalName(online);
                    if (original != null && !original.trim().isEmpty()) {
                        finalName = original;
                    }
                }
                break;
            }
        }

        knownNames.add(normalizeName(finalName));
        long now = System.currentTimeMillis();

        if (isMongo()) {
            try {
                Document filter = new Document("player_uuid", uuid.toString()).append("ip", ip);
                Document existing = getCollection().find(filter).first();

                long firstSeen = existing != null && existing.containsKey("first_seen") ? existing.getLong("first_seen") : now;

                Document updateDoc = new Document("player_name", finalName)
                        .append("first_seen", firstSeen)
                        .append("last_seen", now);

                getCollection().updateOne(
                        filter,
                        new Document("$set", updateDoc),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao registrar IP do jogador no MongoDB: " + e.getMessage());
            }
            return;
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO player_addresses (player_uuid, player_name, ip, first_seen, last_seen) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), last_seen = VALUES(last_seen)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, finalName);
            ps.setString(3, ip);
            ps.setLong(4, now);
            ps.setLong(5, now);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao registrar IP do jogador no DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean hasSeenName(String name) {
        String normalizedName = normalizeName(name);
        if (normalizedName.isEmpty()) {
            return false;
        }

        if (knownNames.contains(normalizedName)) {
            return true;
        }

        if (isMongo()) {
            try {
                Document doc = getCollection().find(new Document("player_name", new Document("$regex", "^" + normalizedName + "$").append("$options", "i"))).first();
                if (doc != null) {
                    knownNames.add(normalizedName);
                    return true;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao verificar histórico de nome do jogador no MongoDB: " + e.getMessage());
            }
            return false;
        }

        String sql = "SELECT 1 FROM player_addresses WHERE LOWER(player_name) = ? LIMIT 1";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    knownNames.add(normalizedName);
                    return true;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao verificar histórico de nome do jogador no DB: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public CompletableFuture<List<String>> findAlts(String targetName, UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> result = new ArrayList<>();
            String normalizedName = normalizeName(targetName);
            String uuidValue = targetUuid == null ? "" : targetUuid.toString();

            if (normalizedName.isEmpty() && uuidValue.isEmpty()) {
                return result;
            }

            if (isMongo()) {
                try {
                    Bson selfFilter = uuidValue.isEmpty()
                            ? Filters.regex("player_name", "^" + normalizedName + "$", "i")
                            : Filters.or(Filters.eq("player_uuid", uuidValue), Filters.regex("player_name", "^" + normalizedName + "$", "i"));

                    List<String> ips = new ArrayList<>();
                    for (Document doc : getCollection().find(selfFilter)) {
                        String ip = doc.getString("ip");
                        if (ip != null && !ips.contains(ip)) ips.add(ip);
                    }

                    if (ips.isEmpty()) return result;

                    for (Document doc : getCollection().find(Filters.in("ip", ips)).sort(Sorts.descending("last_seen"))) {
                        String name = doc.getString("player_name");
                        String playerUuid = doc.getString("player_uuid");

                        if (name == null || name.trim().isEmpty()) continue;
                        if (!uuidValue.isEmpty() && uuidValue.equalsIgnoreCase(playerUuid)) continue;
                        if (!normalizedName.isEmpty() && normalizedName.equalsIgnoreCase(name)) continue;

                        if (!result.contains(name)) {
                            result.add(name);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao buscar alts no MongoDB: " + e.getMessage());
                }
                return result;
            }

            String sql = "SELECT DISTINCT player_name, player_uuid, MAX(last_seen) AS last_seen FROM player_addresses WHERE ip IN (SELECT DISTINCT ip FROM player_addresses WHERE LOWER(player_name) = ? OR player_uuid = ?) GROUP BY player_name, player_uuid ORDER BY last_seen DESC";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, normalizedName);
                ps.setString(2, uuidValue);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("player_name");
                        String playerUuid = rs.getString("player_uuid");
                        if (name == null || name.trim().isEmpty()) {
                            continue;
                        }
                        if (!uuidValue.isEmpty() && uuidValue.equalsIgnoreCase(playerUuid)) {
                            continue;
                        }
                        if (!normalizedName.isEmpty() && normalizedName.equalsIgnoreCase(name)) {
                            continue;
                        }
                        if (!result.contains(name)) {
                            result.add(name);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao buscar alts no DB: " + e.getMessage());
                e.printStackTrace();
            }

            return result;
        });
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}