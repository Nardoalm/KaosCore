package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bukkit.util.Logger;
import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BungeePunishmentService {

    private static final String REPLACED_REASON = "Substituida por nova punicao";

    private final BungeeKaosCore plugin;
    private final BungeeDatabase database;
    private final BungeeDiscordWebhookManager webhookManager;

    public BungeePunishmentService(BungeeKaosCore plugin, BungeeDatabase database, BungeeDiscordWebhookManager webhookManager) {
        this.plugin = plugin;
        this.database = database;
        this.webhookManager = webhookManager;
    }

    public PunishmentEntry getActiveBan(UUID uuid, String name) {
        return getActive(PunishmentType.BAN, uuid, name);
    }

    public PunishmentEntry getActiveMute(UUID uuid, String name) {
        return getActive(PunishmentType.MUTE, uuid, name);
    }

    public PunishmentEntry punish(PunishmentType type, UUID targetUuid, String targetName, UUID punisherUuid, String punisherName, String reason, long expiresAt, String source) {
        if (type == null || Texts.safe(targetName).trim().isEmpty()) {
            return null;
        }
        clearActive(type, targetUuid, targetName, punisherUuid, punisherName, REPLACED_REASON);
        long createdAt = System.currentTimeMillis();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO punishments (target_uuid,target_name,punishment_type,reason,created_at,expires_at,punisher_uuid,punisher_name,source,active) VALUES (?,?,?,?,?,?,?,?,?,1)", Statement.RETURN_GENERATED_KEYS)) {
            if (targetUuid == null) {
                statement.setNull(1, Types.VARCHAR);
            } else {
                statement.setString(1, targetUuid.toString());
            }
            statement.setString(2, targetName);
            statement.setString(3, type.name());
            statement.setString(4, Texts.safe(reason));
            statement.setLong(5, createdAt);
            if (expiresAt <= 0L) {
                statement.setNull(6, Types.BIGINT);
            } else {
                statement.setLong(6, expiresAt);
            }
            if (punisherUuid == null) {
                statement.setNull(7, Types.VARCHAR);
            } else {
                statement.setString(7, punisherUuid.toString());
            }
            statement.setString(8, Texts.safe(punisherName));
            statement.setString(9, Texts.safe(source));
            statement.executeUpdate();
            long id = 0L;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getLong(1);
                }
            }
            PunishmentEntry entry = new PunishmentEntry(id, targetUuid, targetName, type, Texts.safe(reason), createdAt, expiresAt, punisherUuid, Texts.safe(punisherName), Texts.safe(source), true, 0L, null, null, null);
            webhookManager.logPunishmentApplied(entry);
            return entry;
        } catch (Exception exception) {
            Logger.warning("[KaosCore] Falha ao registrar punicao: " + exception.getMessage());
            return null;
        }
    }

    public PunishmentEntry warn(UUID targetUuid, String targetName, UUID punisherUuid, String punisherName, String reason, String source) {
        return punish(PunishmentType.WARN, targetUuid, targetName, punisherUuid, punisherName, reason, 0L, source);
    }

    public boolean clearActive(PunishmentType type, UUID targetUuid, String targetName, UUID removedByUuid, String removedByName, String removedReason) {
        PunishmentEntry active = getActive(type, targetUuid, targetName);
        if (active == null) {
            return false;
        }
        active.deactivate(removedByUuid, removedByName, removedReason);
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE punishments SET active = 0, removed_at = ?, removed_by_uuid = ?, removed_by_name = ?, removed_reason = ? WHERE id = ?")) {
            statement.setLong(1, active.getRemovedAt());
            if (removedByUuid == null) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, removedByUuid.toString());
            }
            statement.setString(3, Texts.safe(removedByName));
            statement.setString(4, Texts.safe(removedReason));
            statement.setLong(5, active.getId());
            boolean result = statement.executeUpdate() > 0;
            if (result) {
                webhookManager.logPunishmentRevoked(active, REPLACED_REASON.equalsIgnoreCase(Texts.safe(removedReason)));
            }
            return result;
        } catch (Exception exception) {
            Logger.warning("[KaosCore] Falha ao revogar punicao: " + exception.getMessage());
            return false;
        }
    }

    public List<PunishmentEntry> getHistory(UUID targetUuid, String targetName, int limit) {
        List<PunishmentEntry> result = new ArrayList<>();
        try (Connection connection = database.getConnection()) {
            PreparedStatement statement;
            if (targetUuid != null) {
                statement = connection.prepareStatement("SELECT * FROM punishments WHERE target_uuid = ? OR LOWER(target_name) = ? ORDER BY created_at DESC LIMIT ?");
                statement.setString(1, targetUuid.toString());
                statement.setString(2, Texts.normalize(targetName));
                statement.setInt(3, limit);
            } else {
                statement = connection.prepareStatement("SELECT * FROM punishments WHERE LOWER(target_name) = ? ORDER BY created_at DESC LIMIT ?");
                statement.setString(1, Texts.normalize(targetName));
                statement.setInt(2, limit);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(map(resultSet));
                }
            }
            statement.close();
        } catch (Exception exception) {
            Logger.warning("[KaosCore] Falha ao consultar historico: " + exception.getMessage());
        }
        return result;
    }

    private PunishmentEntry getActive(PunishmentType type, UUID uuid, String name) {
        try (Connection connection = database.getConnection()) {
            PreparedStatement statement;
            if (uuid != null) {
                statement = connection.prepareStatement("SELECT * FROM punishments WHERE active = 1 AND punishment_type = ? AND (target_uuid = ? OR LOWER(target_name) = ?) ORDER BY created_at DESC LIMIT 1");
                statement.setString(1, type.name());
                statement.setString(2, uuid.toString());
                statement.setString(3, Texts.normalize(name));
            } else {
                statement = connection.prepareStatement("SELECT * FROM punishments WHERE active = 1 AND punishment_type = ? AND LOWER(target_name) = ? ORDER BY created_at DESC LIMIT 1");
                statement.setString(1, type.name());
                statement.setString(2, Texts.normalize(name));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    statement.close();
                    return null;
                }
                PunishmentEntry entry = map(resultSet);
                statement.close();
                if (entry.isExpired()) {
                    clearActive(type, entry.getTargetUuid(), entry.getTargetName(), null, "SYSTEM", "Punicao expirada");
                    webhookManager.logPunishmentExpired(entry);
                    return null;
                }
                return entry.isActive() ? entry : null;
            }
        } catch (Exception exception) {
            Logger.warning("[KaosCore] Falha ao consultar punicao ativa: " + exception.getMessage());
            return null;
        }
    }

    private PunishmentEntry map(ResultSet rs) throws Exception {
        String targetUuidValue = rs.getString("target_uuid");
        String punisherUuidValue = rs.getString("punisher_uuid");
        String removedByUuidValue = rs.getString("removed_by_uuid");
        long expiresAt = rs.getLong("expires_at");
        if (rs.wasNull()) {
            expiresAt = 0L;
        }
        return new PunishmentEntry(
                rs.getLong("id"),
                targetUuidValue == null || targetUuidValue.trim().isEmpty() ? null : UUID.fromString(targetUuidValue),
                rs.getString("target_name"),
                PunishmentType.valueOf(rs.getString("punishment_type")),
                rs.getString("reason"),
                rs.getLong("created_at"),
                expiresAt,
                punisherUuidValue == null || punisherUuidValue.trim().isEmpty() ? null : UUID.fromString(punisherUuidValue),
                rs.getString("punisher_name"),
                rs.getString("source"),
                rs.getBoolean("active"),
                rs.getLong("removed_at"),
                removedByUuidValue == null || removedByUuidValue.trim().isEmpty() ? null : UUID.fromString(removedByUuidValue),
                rs.getString("removed_by_name"),
                rs.getString("removed_reason")
        );
    }
}

