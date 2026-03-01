package com.knowplugins.nardodev.knowpractice.managers.plugin;

import com.knowplugins.nardodev.knowpractice.KnowPractice;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SkinRepository {
    private final KnowPractice plugin;
    private final DatabaseManager databaseManager;

    public SkinRepository(KnowPractice plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<SkinData> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
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
