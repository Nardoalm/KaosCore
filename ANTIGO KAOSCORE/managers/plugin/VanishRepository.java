package com.knowplugins.ysubz.kaoscore.managers.plugin;

import com.knowplugins.ysubz.kaoscore.KaosCore;
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

    public CompletableFuture<Boolean> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
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
