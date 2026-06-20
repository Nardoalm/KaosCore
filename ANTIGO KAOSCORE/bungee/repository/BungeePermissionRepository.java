package com.knowplugins.ysubz.kaoscore.bungee.repository;

import com.knowplugins.ysubz.kaoscore.bungee.GroupGrant;
import com.knowplugins.ysubz.kaoscore.bungee.PermissionGroup;
import com.knowplugins.ysubz.kaoscore.bungee.PermissionUserData;
import com.knowplugins.ysubz.kaoscore.bungee.UserPermissionNode;
import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import com.knowplugins.ysubz.kaoscore.bungee.services.core.BungeeDatabase;
import com.knowplugins.ysubz.kaoscore.bungee.util.Texts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class BungeePermissionRepository {

    private final BungeeKaosCore plugin;
    private final BungeeDatabase database;

    BungeePermissionRepository(BungeeKaosCore plugin, BungeeDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    Map<String, PermissionGroup> loadGroups() {
        Map<String, PermissionGroup> groups = new HashMap<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT name, priority, prefix, suffix, is_default FROM kaoscore_groups")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    PermissionGroup group = new PermissionGroup(resultSet.getString("name"));
                    group.setPriority(resultSet.getInt("priority"));
                    group.setPrefix(resultSet.getString("prefix"));
                    group.setSuffix(resultSet.getString("suffix"));
                    group.setDefaultGroup(resultSet.getBoolean("is_default"));
                    groups.put(group.getName(), group);
                }
            }

            try (PreparedStatement perms = connection.prepareStatement("SELECT group_name, permission, value FROM kaoscore_group_permissions");
                 ResultSet resultSet = perms.executeQuery()) {
                while (resultSet.next()) {
                    PermissionGroup group = groups.get(Texts.normalize(resultSet.getString("group_name")));
                    if (group != null) {
                        group.setPermission(resultSet.getString("permission"), resultSet.getBoolean("value"));
                    }
                }
            }

            try (PreparedStatement inherits = connection.prepareStatement("SELECT group_name, inherit_group FROM kaoscore_group_inherits");
                 ResultSet resultSet = inherits.executeQuery()) {
                while (resultSet.next()) {
                    PermissionGroup group = groups.get(Texts.normalize(resultSet.getString("group_name")));
                    if (group != null) {
                        group.addInherit(resultSet.getString("inherit_group"));
                    }
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao carregar grupos: " + exception.getMessage());
        }
        return groups;
    }

    void createGroup(String name, boolean isDefault) throws Exception {
        String groupName = Texts.normalize(name);
        if (groupName.isEmpty()) {
            return;
        }
        try (Connection connection = database.getConnection()) {
            if (isDefault) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("UPDATE kaoscore_groups SET is_default = 0");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT IGNORE INTO kaoscore_groups (name, priority, prefix, suffix, is_default) VALUES (?,?,?,?,?)")) {
                statement.setString(1, groupName);
                statement.setInt(2, 0);
                statement.setString(3, "");
                statement.setString(4, "");
                statement.setBoolean(5, isDefault);
                statement.executeUpdate();
            }
        }
    }

    boolean deleteGroup(String name) {
        String groupName = Texts.normalize(name);
        if (groupName.isEmpty()) {
            return false;
        }
        try (Connection connection = database.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM kaoscore_group_permissions WHERE group_name = ?")) {
                statement.setString(1, groupName);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM kaoscore_group_inherits WHERE group_name = ? OR inherit_group = ?")) {
                statement.setString(1, groupName);
                statement.setString(2, groupName);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM kaoscore_groups WHERE name = ?")) {
                statement.setString(1, groupName);
                return statement.executeUpdate() > 0;
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao deletar grupo: " + exception.getMessage());
            return false;
        }
    }

    boolean setGroupMeta(String name, Integer priority, String prefix, String suffix, Boolean isDefault) {
        String groupName = Texts.normalize(name);
        if (groupName.isEmpty()) {
            return false;
        }
        try (Connection connection = database.getConnection()) {
            if (isDefault != null && isDefault) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("UPDATE kaoscore_groups SET is_default = 0");
                }
            }
            StringBuilder sql = new StringBuilder("UPDATE kaoscore_groups SET ");
            List<Object> values = new ArrayList<>();
            if (priority != null) {
                sql.append("priority = ?,");
                values.add(priority);
            }
            if (prefix != null) {
                sql.append("prefix = ?,");
                values.add(prefix);
            }
            if (suffix != null) {
                sql.append("suffix = ?,");
                values.add(suffix);
            }
            if (isDefault != null) {
                sql.append("is_default = ?,");
                values.add(isDefault ? 1 : 0);
            }
            if (values.isEmpty()) {
                return false;
            }
            sql.setLength(sql.length() - 1);
            sql.append(" WHERE name = ?");
            values.add(groupName);
            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                for (int i = 0; i < values.size(); i++) {
                    statement.setObject(i + 1, values.get(i));
                }
                return statement.executeUpdate() > 0;
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao atualizar grupo: " + exception.getMessage());
            return false;
        }
    }

    boolean setGroupPermission(String groupName, String permission, boolean value) {
        String g = Texts.normalize(groupName);
        String p = Texts.normalize(permission);
        if (g.isEmpty() || p.isEmpty()) {
            return false;
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("REPLACE INTO kaoscore_group_permissions (group_name, permission, value) VALUES (?,?,?)")) {
            statement.setString(1, g);
            statement.setString(2, p);
            statement.setBoolean(3, value);
            return statement.executeUpdate() > 0;
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao setar permissao de grupo: " + exception.getMessage());
            return false;
        }
    }

    boolean removeGroupPermission(String groupName, String permission) {
        String g = Texts.normalize(groupName);
        String p = Texts.normalize(permission);
        if (g.isEmpty() || p.isEmpty()) {
            return false;
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM kaoscore_group_permissions WHERE group_name = ? AND permission = ?")) {
            statement.setString(1, g);
            statement.setString(2, p);
            return statement.executeUpdate() > 0;
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao remover permissao de grupo: " + exception.getMessage());
            return false;
        }
    }

    boolean addInherit(String groupName, String inheritGroup) {
        String g = Texts.normalize(groupName);
        String i = Texts.normalize(inheritGroup);
        if (g.isEmpty() || i.isEmpty() || g.equals(i)) {
            return false;
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT IGNORE INTO kaoscore_group_inherits (group_name, inherit_group) VALUES (?,?)")) {
            statement.setString(1, g);
            statement.setString(2, i);
            return statement.executeUpdate() > 0;
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao adicionar heranca: " + exception.getMessage());
            return false;
        }
    }

    boolean removeInherit(String groupName, String inheritGroup) {
        String g = Texts.normalize(groupName);
        String i = Texts.normalize(inheritGroup);
        if (g.isEmpty() || i.isEmpty()) {
            return false;
        }
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM kaoscore_group_inherits WHERE group_name = ? AND inherit_group = ?")) {
            statement.setString(1, g);
            statement.setString(2, i);
            return statement.executeUpdate() > 0;
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao remover heranca: " + exception.getMessage());
            return false;
        }
    }

    boolean clearDefaultGroups() {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE kaoscore_groups SET is_default = 0");
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao limpar grupos default: " + exception.getMessage());
            return false;
        }
    }

    boolean replaceGroupPermissions(String groupName, Map<String, Boolean> permissions) {
        String g = Texts.normalize(groupName);
        if (g.isEmpty()) {
            return false;
        }
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM kaoscore_group_permissions WHERE group_name = ?")) {
                statement.setString(1, g);
                statement.executeUpdate();
            }
            if (permissions != null && !permissions.isEmpty()) {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO kaoscore_group_permissions (group_name, permission, value) VALUES (?,?,?)")) {
                    for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                        String permission = Texts.normalize(entry.getKey());
                        if (permission.isEmpty()) {
                            continue;
                        }
                        statement.setString(1, g);
                        statement.setString(2, permission);
                        statement.setBoolean(3, entry.getValue() == null || entry.getValue());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
            }
            connection.commit();
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao substituir permissoes do grupo: " + exception.getMessage());
            return false;
        }
    }

    boolean replaceGroupInherits(String groupName, Set<String> inherits) {
        String g = Texts.normalize(groupName);
        if (g.isEmpty()) {
            return false;
        }
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM kaoscore_group_inherits WHERE group_name = ?")) {
                statement.setString(1, g);
                statement.executeUpdate();
            }
            if (inherits != null && !inherits.isEmpty()) {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO kaoscore_group_inherits (group_name, inherit_group) VALUES (?,?)")) {
                    for (String inherit : inherits) {
                        String normalized = Texts.normalize(inherit);
                        if (normalized.isEmpty() || normalized.equals(g)) {
                            continue;
                        }
                        statement.setString(1, g);
                        statement.setString(2, normalized);
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
            }
            connection.commit();
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao substituir herancas do grupo: " + exception.getMessage());
            return false;
        }
    }

    PermissionUserData loadUser(UUID uuid, String name) {
        if (uuid == null) {
            return new PermissionUserData(null, safeName(name), Collections.<GroupGrant>emptyList(), Collections.<UserPermissionNode>emptyList(), System.currentTimeMillis());
        }

        long now = System.currentTimeMillis();
        List<GroupGrant> grants = new ArrayList<>();
        List<UserPermissionNode> nodes = new ArrayList<>();

        try (Connection connection = database.getConnection()) {
            try (PreparedStatement expire = connection.prepareStatement("UPDATE kaoscore_user_grants SET active = 0, removed_at = ?, removed_by_name = ?, removed_reason = ? WHERE active = 1 AND uuid = ? AND expires_at IS NOT NULL AND expires_at > 0 AND expires_at <= ?")) {
                expire.setLong(1, now);
                expire.setString(2, "SYSTEM");
                expire.setString(3, "Grant expirado");
                expire.setString(4, uuid.toString());
                expire.setLong(5, now);
                expire.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement("SELECT id, group_name, created_at, expires_at FROM kaoscore_user_grants WHERE active = 1 AND uuid = ? AND (expires_at IS NULL OR expires_at = 0 OR expires_at > ?)")) {
                statement.setString(1, uuid.toString());
                statement.setLong(2, now);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        long expiresAt = resultSet.getLong("expires_at");
                        if (resultSet.wasNull()) {
                            expiresAt = 0L;
                        }
                        grants.add(new GroupGrant(resultSet.getLong("id"), resultSet.getString("group_name"), resultSet.getLong("created_at"), expiresAt));
                    }
                }
            }

            try (PreparedStatement expire = connection.prepareStatement("DELETE FROM kaoscore_user_permissions WHERE uuid = ? AND expires_at IS NOT NULL AND expires_at > 0 AND expires_at <= ?")) {
                expire.setString(1, uuid.toString());
                expire.setLong(2, now);
                expire.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement("SELECT permission, value, expires_at FROM kaoscore_user_permissions WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        long expiresAt = resultSet.getLong("expires_at");
                        if (resultSet.wasNull()) {
                            expiresAt = 0L;
                        }
                        nodes.add(new UserPermissionNode(resultSet.getString("permission"), resultSet.getBoolean("value"), expiresAt));
                    }
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao carregar permissoes do usuario: " + exception.getMessage());
        }

        return new PermissionUserData(uuid, safeName(name), grants, nodes, now);
    }

    void grant(UUID uuid, String playerName, String groupName, UUID giverUuid, String giverName, long durationMs, String reason) throws Exception {
        long now = System.currentTimeMillis();
        Long expiresAt = durationMs > 0L ? now + durationMs : null;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO kaoscore_user_grants (uuid, player_name, group_name, given_by_uuid, given_by_name, reason, created_at, expires_at, active) VALUES (?,?,?,?,?,?,?,?,1)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, safeName(playerName));
            statement.setString(3, Texts.normalize(groupName));
            if (giverUuid == null) {
                statement.setNull(4, Types.VARCHAR);
            } else {
                statement.setString(4, giverUuid.toString());
            }
            statement.setString(5, safeName(giverName));
            statement.setString(6, Texts.safe(reason));
            statement.setLong(7, now);
            if (expiresAt == null) {
                statement.setNull(8, Types.BIGINT);
            } else {
                statement.setLong(8, expiresAt);
            }
            statement.executeUpdate();
        }
    }

    boolean ungrant(UUID uuid, String groupName, UUID removerUuid, String removerName, String removedReason) {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE kaoscore_user_grants SET active = 0, removed_at = ?, removed_by_uuid = ?, removed_by_name = ?, removed_reason = ? WHERE active = 1 AND uuid = ? AND group_name = ?")) {
            statement.setLong(1, System.currentTimeMillis());
            if (removerUuid == null) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, removerUuid.toString());
            }
            statement.setString(3, safeName(removerName));
            statement.setString(4, Texts.safe(removedReason));
            statement.setString(5, uuid.toString());
            statement.setString(6, Texts.normalize(groupName));
            return statement.executeUpdate() > 0;
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao remover grant: " + exception.getMessage());
            return false;
        }
    }

    private static String safeName(String value) {
        String safe = Texts.safe(value).trim();
        return safe.length() > 16 ? safe.substring(0, 16) : safe;
    }
}


