package com.knowplugins.ysubz.kaoscore.managers.plugin.repository;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentType;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PunishmentRepository {

    private final KaosCore plugin;
    private final DatabaseManager databaseManager;

    public PunishmentRepository(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    private boolean isMongo() {
        return databaseManager.getType().equalsIgnoreCase("mongodb");
    }

    private MongoCollection<Document> getCollection() {
        return databaseManager.getMongoDatabase().getCollection("punishments");
    }

    public List<PunishmentEntry> loadActive() {
        List<PunishmentEntry> result = new ArrayList<>();

        if (isMongo()) {
            try {
                long now = System.currentTimeMillis();
                Bson filter = Filters.and(
                        Filters.eq("active", true),
                        Filters.or(Filters.eq("expires_at", 0L), Filters.gt("expires_at", now))
                );
                for (Document doc : getCollection().find(filter)) {
                    PunishmentEntry entry = mapMongo(doc);
                    if (entry != null) {
                        result.add(entry);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar punicoes ativas do MongoDB: " + e.getMessage());
            }
            return result;
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, target_uuid, target_name, punishment_type, reason, created_at, expires_at, punisher_uuid, punisher_name, source, active, removed_at, removed_by_uuid, removed_by_name, removed_reason FROM punishments WHERE active = 1 AND (expires_at IS NULL OR expires_at = 0 OR expires_at > ?)")) {
            ps.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PunishmentEntry entry = map(rs);
                    if (entry != null) {
                        result.add(entry);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao carregar punicoes ativas do DB: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    public void createAsync(PunishmentEntry entry) {
        if (entry == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isMongo()) {
                try {
                    long nextId = System.currentTimeMillis();
                    Document doc = new Document("id", nextId)
                            .append("target_uuid", entry.getTargetUuid() != null ? entry.getTargetUuid().toString() : null)
                            .append("target_name", entry.getTargetName())
                            .append("punishment_type", entry.getType().name())
                            .append("reason", entry.getReason())
                            .append("created_at", entry.getCreatedAt())
                            .append("expires_at", entry.isPermanent() ? 0L : entry.getExpiresAt())
                            .append("punisher_uuid", entry.getPunisherUuid() != null ? entry.getPunisherUuid().toString() : null)
                            .append("punisher_name", entry.getPunisherName())
                            .append("source", entry.getSource())
                            .append("active", entry.isActive());
                    getCollection().insertOne(doc);
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao salvar punicao no MongoDB: " + e.getMessage());
                }
                return;
            }

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO punishments (target_uuid, target_name, punishment_type, reason, created_at, expires_at, punisher_uuid, punisher_name, source, active) VALUES (?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                setUuid(ps, 1, entry.getTargetUuid());
                ps.setString(2, entry.getTargetName());
                ps.setString(3, entry.getType().name());
                ps.setString(4, entry.getReason());
                ps.setLong(5, entry.getCreatedAt());
                if (entry.isPermanent()) {
                    ps.setNull(6, Types.BIGINT);
                } else {
                    ps.setLong(6, entry.getExpiresAt());
                }
                setUuid(ps, 7, entry.getPunisherUuid());
                ps.setString(8, entry.getPunisherName());
                ps.setString(9, entry.getSource());
                ps.setBoolean(10, entry.isActive());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao salvar punicao no DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void revokeAsync(PunishmentType type, UUID targetUuid, String targetName, UUID removedByUuid, String removedByName, String removedReason) {
        if (type == null) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isMongo()) {
                try {
                    List<Bson> filters = new ArrayList<>();
                    filters.add(Filters.eq("active", true));
                    filters.add(Filters.eq("punishment_type", type.name()));

                    if (targetUuid != null && targetName != null && !targetName.trim().isEmpty()) {
                        filters.add(Filters.or(Filters.eq("target_uuid", targetUuid.toString()), Filters.eq("target_name", targetName.toLowerCase())));
                    } else if (targetUuid != null) {
                        filters.add(Filters.eq("target_uuid", targetUuid.toString()));
                    } else if (targetName != null && !targetName.trim().isEmpty()) {
                        filters.add(Filters.eq("target_name", targetName.toLowerCase()));
                    } else {
                        return;
                    }

                    Bson update = Updates.combine(
                            Updates.set("active", false),
                            Updates.set("removed_at", System.currentTimeMillis()),
                            Updates.set("removed_by_uuid", removedByUuid != null ? removedByUuid.toString() : null),
                            Updates.set("removed_by_name", removedByName),
                            Updates.set("removed_reason", removedReason)
                    );

                    getCollection().updateMany(Filters.and(filters), update);
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao revogar punicao no MongoDB: " + e.getMessage());
                }
                return;
            }

            String sql = buildRevokeSql(targetUuid, targetName);
            if (sql == null) {
                return;
            }

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                ps.setLong(index++, System.currentTimeMillis());
                setUuid(ps, index++, removedByUuid);
                ps.setString(index++, removedByName);
                ps.setString(index++, removedReason);
                ps.setString(index++, type.name());
                if (targetUuid != null) {
                    ps.setString(index++, targetUuid.toString());
                }
                if (targetName != null && !targetName.trim().isEmpty()) {
                    ps.setString(index, targetName.toLowerCase());
                }
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao revogar punicao no DB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void expireAsync(PunishmentType type, UUID targetUuid, String targetName) {
        revokeAsync(type, targetUuid, targetName, null, "SYSTEM", "Punicao expirada");
    }

    public CompletableFuture<List<PunishmentEntry>> loadHistory(UUID targetUuid, String targetName, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PunishmentEntry> result = new ArrayList<>();
            int resolvedLimit = limit <= 0 ? 10 : limit;

            if (isMongo()) {
                try {
                    List<Bson> filters = new ArrayList<>();
                    if (targetUuid != null) filters.add(Filters.eq("target_uuid", targetUuid.toString()));
                    if (targetName != null && !targetName.trim().isEmpty()) filters.add(Filters.eq("target_name", targetName.toLowerCase()));

                    if (filters.isEmpty()) return result;

                    Bson finalFilter = filters.size() == 2 ? Filters.or(filters) : filters.get(0);

                    for (Document doc : getCollection().find(finalFilter).sort(Sorts.descending("created_at")).limit(resolvedLimit)) {
                        PunishmentEntry entry = mapMongo(doc);
                        if (entry != null) {
                            result.add(entry);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao carregar histórico de punicoes do MongoDB: " + e.getMessage());
                }
                return result;
            }

            String sql = buildHistorySql(targetUuid, targetName);
            if (sql == null) {
                return result;
            }

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                if (targetUuid != null) {
                    ps.setString(index++, targetUuid.toString());
                }
                if (targetName != null && !targetName.trim().isEmpty()) {
                    ps.setString(index++, targetName.toLowerCase());
                }
                ps.setInt(index, resolvedLimit);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PunishmentEntry entry = map(rs);
                        if (entry != null) {
                            result.add(entry);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar histórico de punicoes do DB: " + e.getMessage());
                e.printStackTrace();
            }

            return result;
        });
    }

    private String buildRevokeSql(UUID targetUuid, String targetName) {
        boolean hasUuid = targetUuid != null;
        boolean hasName = targetName != null && !targetName.trim().isEmpty();

        if (!hasUuid && !hasName) {
            return null;
        }

        if (hasUuid && hasName) {
            return "UPDATE punishments SET active = 0, removed_at = ?, removed_by_uuid = ?, removed_by_name = ?, removed_reason = ? WHERE active = 1 AND punishment_type = ? AND (target_uuid = ? OR LOWER(target_name) = ?)";
        }

        if (hasUuid) {
            return "UPDATE punishments SET active = 0, removed_at = ?, removed_by_uuid = ?, removed_by_name = ?, removed_reason = ? WHERE active = 1 AND punishment_type = ? AND target_uuid = ?";
        }

        return "UPDATE punishments SET active = 0, removed_at = ?, removed_by_uuid = ?, removed_by_name = ?, removed_reason = ? WHERE active = 1 AND punishment_type = ? AND LOWER(target_name) = ?";
    }

    private String buildHistorySql(UUID targetUuid, String targetName) {
        boolean hasUuid = targetUuid != null;
        boolean hasName = targetName != null && !targetName.trim().isEmpty();

        if (!hasUuid && !hasName) {
            return null;
        }

        if (hasUuid && hasName) {
            return "SELECT id, target_uuid, target_name, punishment_type, reason, created_at, expires_at, punisher_uuid, punisher_name, source, active, removed_at, removed_by_uuid, removed_by_name, removed_reason FROM punishments WHERE target_uuid = ? OR LOWER(target_name) = ? ORDER BY created_at DESC LIMIT ?";
        }

        if (hasUuid) {
            return "SELECT id, target_uuid, target_name, punishment_type, reason, created_at, expires_at, punisher_uuid, punisher_name, source, active, removed_at, removed_by_uuid, removed_by_name, removed_reason FROM punishments WHERE target_uuid = ? ORDER BY created_at DESC LIMIT ?";
        }

        return "SELECT id, target_uuid, target_name, punishment_type, reason, created_at, expires_at, punisher_uuid, punisher_name, source, active, removed_at, removed_by_uuid, removed_by_name, removed_reason FROM punishments WHERE LOWER(target_name) = ? ORDER BY created_at DESC LIMIT ?";
    }

    private PunishmentEntry map(ResultSet rs) {
        try {
            long id = rs.getLong("id");
            UUID targetUuid = parseUuid(rs.getString("target_uuid"));
            String targetName = rs.getString("target_name");
            PunishmentType type = PunishmentType.valueOf(rs.getString("punishment_type"));
            String reason = rs.getString("reason");
            long createdAt = rs.getLong("created_at");
            long expiresAt = rs.getLong("expires_at");
            if (rs.wasNull()) {
                expiresAt = 0L;
            }
            UUID punisherUuid = parseUuid(rs.getString("punisher_uuid"));
            String punisherName = rs.getString("punisher_name");
            String source = rs.getString("source");
            boolean active = rs.getBoolean("active");
            long removedAt = rs.getLong("removed_at");
            if (rs.wasNull()) {
                removedAt = 0L;
            }
            UUID removedByUuid = parseUuid(rs.getString("removed_by_uuid"));
            String removedByName = rs.getString("removed_by_name");
            String removedReason = rs.getString("removed_reason");
            return new PunishmentEntry(id, targetUuid, targetName, type, reason, createdAt, expiresAt, punisherUuid, punisherName, source, active, removedAt, removedByUuid, removedByName, removedReason);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao mapear punicao do DB: " + e.getMessage());
            return null;
        }
    }

    private PunishmentEntry mapMongo(Document doc) {
        try {
            long id = doc.getLong("id");
            UUID targetUuid = parseUuid(doc.getString("target_uuid"));
            String targetName = doc.getString("target_name");
            PunishmentType type = PunishmentType.valueOf(doc.getString("punishment_type"));
            String reason = doc.getString("reason");
            long createdAt = doc.getLong("created_at");
            long expiresAt = doc.containsKey("expires_at") ? doc.getLong("expires_at") : 0L;
            UUID punisherUuid = parseUuid(doc.getString("punisher_uuid"));
            String punisherName = doc.getString("punisher_name");
            String source = doc.getString("source");
            boolean active = doc.getBoolean("active", false);
            long removedAt = doc.containsKey("removed_at") ? doc.getLong("removed_at") : 0L;
            UUID removedByUuid = parseUuid(doc.getString("removed_by_uuid"));
            String removedByName = doc.getString("removed_by_name");
            String removedReason = doc.getString("removed_reason");
            return new PunishmentEntry(id, targetUuid, targetName, type, reason, createdAt, expiresAt, punisherUuid, punisherName, source, active, removedAt, removedByUuid, removedByName, removedReason);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao mapear punicao do MongoDB: " + e.getMessage());
            return null;
        }
    }

    private void setUuid(PreparedStatement ps, int index, UUID uuid) throws Exception {
        if (uuid == null) {
            ps.setNull(index, Types.VARCHAR);
            return;
        }
        ps.setString(index, uuid.toString());
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (Exception ignored) {
            return null;
        }
    }
}