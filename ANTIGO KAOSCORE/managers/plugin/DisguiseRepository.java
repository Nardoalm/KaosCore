package com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin;

import com.knowplugins.ysubz.kaoscore.KaosCore;
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

    public CompletableFuture<String> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT is_disguised, disguise_meta FROM disguises WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        boolean isDisguised = rs.getBoolean("is_disguised");
                        if (isDisguised) {
                            return rs.getString("disguise_meta");
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar disguise do DB: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
    }

    public CompletableFuture<Map<UUID, String>> loadAllActive() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, String> result = new HashMap<>();
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
                plugin.getLogger().warning("Erro ao carregar disguises ativos do DB: " + e.getMessage());
                e.printStackTrace();
            }
            return result;
        });
    }

    public void saveAsync(UUID uuid, boolean isDisguised, String type, String meta) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = databaseManager.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("REPLACE INTO disguises (uuid,is_disguised,disguise_type,disguise_meta) VALUES (?,?,?,?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setBoolean(2, isDisguised);
                    ps.setString(3, type);
                    ps.setString(4, meta);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao salvar disguise no DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
