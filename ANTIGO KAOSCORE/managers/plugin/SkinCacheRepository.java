package com.knowplugins.ysubz.kaoscore.managers.plugin;

import com.knowplugins.ysubz.kaoscore.KaosCore;
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

    public CompletableFuture<SkinData> load(String nick) {
        String clean = normalizeNick(nick);
        if (clean.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            return loadSync(clean);
        });
    }

    public SkinData loadSync(String nick) {
        String clean = normalizeNick(nick);
        if (clean.isEmpty()) {
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
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("REPLACE INTO kaoscore_skin_cache (nick, skin_value, skin_signature) VALUES (?,?,?)")) {
                ps.setString(1, clean);
                ps.setString(2, value);
                ps.setString(3, signature);
                ps.executeUpdate();
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
