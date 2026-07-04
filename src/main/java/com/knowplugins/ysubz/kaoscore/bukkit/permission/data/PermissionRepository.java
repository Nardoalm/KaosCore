package com.knowplugins.ysubz.kaoscore.bukkit.permission.data;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.database.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.permission.GroupGrant;
import com.knowplugins.ysubz.kaoscore.bukkit.permission.PermissionGroup;
import com.knowplugins.ysubz.kaoscore.bukkit.permission.UserPermissionNode;
import com.knowplugins.ysubz.kaoscore.bukkit.util.Logger;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PermissionRepository {

    private final KaosCore plugin;
    private final DatabaseManager databaseManager;

    public PermissionRepository(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    private boolean isMongo() {
        return databaseManager.getType().equalsIgnoreCase("mongodb");
    }

    private MongoCollection<Document> getCollection(String name) {
        return databaseManager.getMongoDatabase().getCollection(name);
    }

    public Map<String, PermissionGroup> loadGroups() {
        Map<String, PermissionGroup> groups = new HashMap<>();

        if (isMongo()) {
            try {
                for (Document doc : getCollection("kaoscore_groups").find()) {
                    String name = normalize(doc.getString("name"));
                    if (name.isEmpty()) continue;

                    PermissionGroup group = new PermissionGroup(name);
                    group.setPriority(doc.getInteger("priority", 0));
                    group.setPrefix(doc.getString("prefix"));
                    group.setSuffix(doc.getString("suffix"));
                    group.setDefaultGroup(doc.getBoolean("is_default", false));
                    groups.put(name, group);
                }

                for (Document doc : getCollection("kaoscore_group_permissions").find()) {
                    String groupName = normalize(doc.getString("group_name"));
                    PermissionGroup group = groups.get(groupName);
                    if (group == null) continue;

                    String perm = normalize(doc.getString("permission"));
                    if (perm.isEmpty()) continue;

                    group.setPermission(perm, doc.getBoolean("value", true));
                }

                for (Document doc : getCollection("kaoscore_group_inherits").find()) {
                    String groupName = normalize(doc.getString("group_name"));
                    PermissionGroup group = groups.get(groupName);
                    if (group == null) continue;

                    String inherit = normalize(doc.getString("inherit_group"));
                    if (!inherit.isEmpty()) {
                        group.addInherit(inherit);
                    }
                }
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao carregar grupos do MongoDB: " + e.getMessage());
                }
            }
            return groups;
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name, priority, prefix, suffix, is_default FROM kaoscore_groups")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = normalize(rs.getString("name"));
                    if (name.isEmpty()) {
                        continue;
                    }
                    PermissionGroup group = new PermissionGroup(name);
                    group.setPriority(rs.getInt("priority"));
                    group.setPrefix(rs.getString("prefix"));
                    group.setSuffix(rs.getString("suffix"));
                    group.setDefaultGroup(rs.getBoolean("is_default"));
                    groups.put(name, group);
                }
            }

            try (PreparedStatement gp = conn.prepareStatement("SELECT group_name, permission, value FROM kaoscore_group_permissions")) {
                try (ResultSet rs = gp.executeQuery()) {
                    while (rs.next()) {
                        String groupName = normalize(rs.getString("group_name"));
                        PermissionGroup group = groups.get(groupName);
                        if (group == null) {
                            continue;
                        }
                        String perm = normalize(rs.getString("permission"));
                        if (perm.isEmpty()) {
                            continue;
                        }
                        group.setPermission(perm, rs.getBoolean("value"));
                    }
                }
            }

            try (PreparedStatement gi = conn.prepareStatement("SELECT group_name, inherit_group FROM kaoscore_group_inherits")) {
                try (ResultSet rs = gi.executeQuery()) {
                    while (rs.next()) {
                        String groupName = normalize(rs.getString("group_name"));
                        PermissionGroup group = groups.get(groupName);
                        if (group == null) {
                            continue;
                        }
                        String inherit = normalize(rs.getString("inherit_group"));
                        if (!inherit.isEmpty()) {
                            group.addInherit(inherit);
                        }
                    }
                }
            }

        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao carregar grupos do DB: " + e.getMessage());
            }
            e.printStackTrace();
        }

        return groups;
    }

    public void createGroup(String name, boolean isDefault) throws Exception {
        String groupName = normalize(name);
        if (groupName.isEmpty()) {
            return;
        }

        if (isMongo()) {
            if (isDefault) {
                getCollection("kaoscore_groups").updateMany(new Document(), Updates.set("is_default", false));
            }
            Document existing = getCollection("kaoscore_groups").find(new Document("name", groupName)).first();
            if (existing == null) {
                Document doc = new Document("name", groupName)
                        .append("priority", 0)
                        .append("prefix", "")
                        .append("suffix", "")
                        .append("is_default", isDefault);
                getCollection("kaoscore_groups").insertOne(doc);
            }
            return;
        }

        try (Connection conn = databaseManager.getConnection()) {
            if (isDefault) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("UPDATE kaoscore_groups SET is_default = 0");
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO kaoscore_groups (name, priority, prefix, suffix, is_default) VALUES (?,?,?,?,?)")) {
                ps.setString(1, groupName);
                ps.setInt(2, 0);
                ps.setString(3, "");
                ps.setString(4, "");
                ps.setBoolean(5, isDefault);
                ps.executeUpdate();
            }
        }
    }

    public boolean deleteGroup(String name) {
        String groupName = normalize(name);
        if (groupName.isEmpty()) {
            return false;
        }

        if (isMongo()) {
            try {
                getCollection("kaoscore_group_permissions").deleteMany(new Document("group_name", groupName));
                getCollection("kaoscore_group_inherits").deleteMany(Filters.or(
                        Filters.eq("group_name", groupName),
                        Filters.eq("inherit_group", groupName)
                ));
                getCollection("kaoscore_user_grants").updateMany(
                        Filters.and(Filters.eq("active", true), Filters.eq("group_name", groupName)),
                        Updates.combine(
                                Updates.set("active", false),
                                Updates.set("removed_at", System.currentTimeMillis()),
                                Updates.set("removed_by_name", "SYSTEM"),
                                Updates.set("removed_reason", "Grupo removido")
                        )
                );
                return getCollection("kaoscore_groups").deleteOne(new Document("name", groupName)).getDeletedCount() > 0;
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao deletar grupo no MongoDB: " + e.getMessage());
                }
                return false;
            }
        }

        try (Connection conn = databaseManager.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM kaoscore_group_permissions WHERE group_name = ?")) {
                ps.setString(1, groupName);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM kaoscore_group_inherits WHERE group_name = ? OR inherit_group = ?")) {
                ps.setString(1, groupName);
                ps.setString(2, groupName);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE kaoscore_user_grants SET active = 0, removed_at = ?, removed_by_name = ?, removed_reason = ? WHERE active = 1 AND group_name = ?")) {
                ps.setLong(1, System.currentTimeMillis());
                ps.setString(2, "SYSTEM");
                ps.setString(3, "Grupo removido");
                ps.setString(4, groupName);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM kaoscore_groups WHERE name = ?")) {
                ps.setString(1, groupName);
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao deletar grupo: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean setGroupMeta(String name, Integer priority, String prefix, String suffix, Boolean isDefault) {
        String groupName = normalize(name);
        if (groupName.isEmpty()) {
            return false;
        }

        if (isMongo()) {
            try {
                if (isDefault != null && isDefault) {
                    getCollection("kaoscore_groups").updateMany(new Document(), Updates.set("is_default", false));
                }
                List<Bson> updates = new ArrayList<>();
                if (priority != null) updates.add(Updates.set("priority", priority));
                if (prefix != null) updates.add(Updates.set("prefix", prefix));
                if (suffix != null) updates.add(Updates.set("suffix", suffix));
                if (isDefault != null) updates.add(Updates.set("is_default", isDefault));

                if (updates.isEmpty()) return false;

                return getCollection("kaoscore_groups").updateOne(
                        new Document("name", groupName),
                        Updates.combine(updates)
                ).getMatchedCount() > 0;
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao atualizar grupo no MongoDB: " + e.getMessage());
                }
                return false;
            }
        }

        try (Connection conn = databaseManager.getConnection()) {
            if (isDefault != null && isDefault) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("UPDATE kaoscore_groups SET is_default = 0");
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

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < values.size(); i++) {
                    Object v = values.get(i);
                    if (v instanceof Integer) {
                        ps.setInt(i + 1, (Integer) v);
                    } else if (v instanceof String) {
                        ps.setString(i + 1, (String) v);
                    } else {
                        ps.setObject(i + 1, v);
                    }
                }
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao atualizar grupo: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean setGroupPermission(String groupName, String permission, boolean value) {
        String g = normalize(groupName);
        String p = normalize(permission);
        if (g.isEmpty() || p.isEmpty()) {
            return false;
        }

        if (isMongo()) {
            try {
                Document query = new Document("group_name", g).append("permission", p);
                Document doc = new Document("group_name", g).append("permission", p).append("value", value);
                getCollection("kaoscore_group_permissions").replaceOne(query, doc, new ReplaceOptions().upsert(true));
                return true;
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao setar perm de grupo no MongoDB: " + e.getMessage());
                }
                return false;
            }
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("REPLACE INTO kaoscore_group_permissions (group_name, permission, value) VALUES (?,?,?)")) {
            ps.setString(1, g);
            ps.setString(2, p);
            ps.setBoolean(3, value);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao setar perm de grupo: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeGroupPermission(String groupName, String permission) {
        String g = normalize(groupName);
        String p = normalize(permission);
        if (g.isEmpty() || p.isEmpty()) {
            return false;
        }

        if (isMongo()) {
            try {
                return getCollection("kaoscore_group_permissions").deleteOne(
                        new Document("group_name", g).append("permission", p)
                ).getDeletedCount() > 0;
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao remover perm de grupo no MongoDB: " + e.getMessage());
                }
                return false;
            }
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM kaoscore_group_permissions WHERE group_name = ? AND permission = ?")) {
            ps.setString(1, g);
            ps.setString(2, p);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao remover perm de grupo: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean addInherit(String groupName, String inheritGroup) {
        String g = normalize(groupName);
        String i = normalize(inheritGroup);
        if (g.isEmpty() || i.isEmpty() || g.equals(i)) {
            return false;
        }

        if (isMongo()) {
            try {
                Document query = new Document("group_name", g).append("inherit_group", i);
                Document existing = getCollection("kaoscore_group_inherits").find(query).first();
                if (existing == null) {
                    getCollection("kaoscore_group_inherits").insertOne(query);
                }
                return true;
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao adicionar heranca no MongoDB: " + e.getMessage());
                }
                return false;
            }
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO kaoscore_group_inherits (group_name, inherit_group) VALUES (?,?)")) {
            ps.setString(1, g);
            ps.setString(2, i);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao adicionar heranca: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeInherit(String groupName, String inheritGroup) {
        String g = normalize(groupName);
        String i = normalize(inheritGroup);
        if (g.isEmpty() || i.isEmpty()) {
            return false;
        }

        if (isMongo()) {
            try {
                return getCollection("kaoscore_group_inherits").deleteOne(
                        new Document("group_name", g).append("inherit_group", i)
                ).getDeletedCount() > 0;
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao remover heranca no MongoDB: " + e.getMessage());
                }
                return false;
            }
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM kaoscore_group_inherits WHERE group_name = ? AND inherit_group = ?")) {
            ps.setString(1, g);
            ps.setString(2, i);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao remover heranca: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean clearDefaultGroups() {
        if (isMongo()) {
            try {
                getCollection("kaoscore_groups").updateMany(new Document(), Updates.set("is_default", false));
                return true;
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao limpar default groups no MongoDB: " + e.getMessage());
                }
                return false;
            }
        }

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE kaoscore_groups SET is_default = 0");
            return true;
        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao limpar default groups: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean replaceGroupPermissions(String groupName, Map<String, Boolean> permissions) {
        String g = normalize(groupName);
        if (g.isEmpty()) {
            return false;
        }

        if (isMongo()) {
            try {
                getCollection("kaoscore_group_permissions").deleteMany(new Document("group_name", g));
                if (permissions != null && !permissions.isEmpty()) {
                    List<Document> docs = new ArrayList<>();
                    for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                        String p = normalize(entry.getKey());
                        if (p.isEmpty()) continue;
                        docs.add(new Document("group_name", g)
                                .append("permission", p)
                                .append("value", entry.getValue() == null || entry.getValue()));
                    }
                    if (!docs.isEmpty()) {
                        getCollection("kaoscore_group_permissions").insertMany(docs);
                    }
                }
                return true;
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao substituir perms do grupo no MongoDB: " + e.getMessage());
                }
                return false;
            }
        }

        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement del = conn.prepareStatement("DELETE FROM kaoscore_group_permissions WHERE group_name = ?")) {
                del.setString(1, g);
                del.executeUpdate();
            }

            if (permissions != null && !permissions.isEmpty()) {
                try (PreparedStatement ins = conn.prepareStatement("INSERT INTO kaoscore_group_permissions (group_name, permission, value) VALUES (?,?,?)")) {
                    for (Map.Entry<String, Boolean> e : permissions.entrySet()) {
                        String p = normalize(e.getKey());
                        if (p.isEmpty()) {
                            continue;
                        }
                        ins.setString(1, g);
                        ins.setString(2, p);
                        ins.setBoolean(3, e.getValue() == null || e.getValue());
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao substituir perms do grupo: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean replaceGroupInherits(String groupName, Set<String> inherits) {
        String g = normalize(groupName);
        if (g.isEmpty()) {
            return false;
        }

        if (isMongo()) {
            try {
                getCollection("kaoscore_group_inherits").deleteMany(new Document("group_name", g));
                if (inherits != null && !inherits.isEmpty()) {
                    List<Document> docs = new ArrayList<>();
                    for (String inherit : inherits) {
                        String i = normalize(inherit);
                        if (i.isEmpty() || i.equals(g)) continue;
                        docs.add(new Document("group_name", g).append("inherit_group", i));
                    }
                    if (!docs.isEmpty()) {
                        getCollection("kaoscore_group_inherits").insertMany(docs);
                    }
                }
                return true;
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao substituir herancas do grupo no MongoDB: " + e.getMessage());
                }
                return false;
            }
        }

        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement del = conn.prepareStatement("DELETE FROM kaoscore_group_inherits WHERE group_name = ?")) {
                del.setString(1, g);
                del.executeUpdate();
            }

            if (inherits != null && !inherits.isEmpty()) {
                try (PreparedStatement ins = conn.prepareStatement("INSERT INTO kaoscore_group_inherits (group_name, inherit_group) VALUES (?,?)")) {
                    for (String inherit : inherits) {
                        String i = normalize(inherit);
                        if (i.isEmpty() || i.equals(g)) {
                            continue;
                        }
                        ins.setString(1, g);
                        ins.setString(2, i);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao substituir herancas do grupo: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    public PermissionUserData loadUser(UUID uuid, String name) {
        if (uuid == null) {
            return new PermissionUserData(null, safeName(name), null, null, System.currentTimeMillis());
        }

        long now = System.currentTimeMillis();
        List<GroupGrant> grants = new ArrayList<>();
        List<UserPermissionNode> nodes = new ArrayList<>();

        if (isMongo()) {
            try {
                Bson grantExpireFilter = Filters.and(
                        Filters.eq("active", true),
                        Filters.eq("uuid", uuid.toString()),
                        Filters.gt("expires_at", 0L),
                        Filters.lte("expires_at", now)
                );
                getCollection("kaoscore_user_grants").updateMany(grantExpireFilter, Updates.combine(
                        Updates.set("active", false),
                        Updates.set("removed_at", now),
                        Updates.set("removed_by_name", "SYSTEM"),
                        Updates.set("removed_reason", "Grant expirado")
                ));

                Bson grantActiveFilter = Filters.and(
                        Filters.eq("active", true),
                        Filters.eq("uuid", uuid.toString()),
                        Filters.or(Filters.eq("expires_at", 0L), Filters.gt("expires_at", now))
                );
                for (Document doc : getCollection("kaoscore_user_grants").find(grantActiveFilter)) {
                    long id = doc.getLong("id");
                    String groupName = normalize(doc.getString("group_name"));
                    long createdAt = doc.getLong("created_at");
                    long expiresAt = doc.containsKey("expires_at") ? doc.getLong("expires_at") : 0L;
                    if (!groupName.isEmpty()) {
                        grants.add(new GroupGrant(id, groupName, createdAt, expiresAt));
                    }
                }

                Bson permExpireFilter = Filters.and(
                        Filters.eq("uuid", uuid.toString()),
                        Filters.gt("expires_at", 0L),
                        Filters.lte("expires_at", now)
                );
                getCollection("kaoscore_user_permissions").deleteMany(permExpireFilter);

                for (Document doc : getCollection("kaoscore_user_permissions").find(new Document("uuid", uuid.toString()))) {
                    String perm = normalize(doc.getString("permission"));
                    boolean value = doc.getBoolean("value", true);
                    long expiresAt = doc.containsKey("expires_at") ? doc.getLong("expires_at") : 0L;
                    if (!perm.isEmpty()) {
                        nodes.add(new UserPermissionNode(perm, value, expiresAt));
                    }
                }

                String safe = safeName(name);
                if (!safe.isEmpty()) {
                    getCollection("kaoscore_user_grants").updateMany(
                            new Document("uuid", uuid.toString()),
                            Updates.set("player_name", safe)
                    );
                }
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao carregar user perms do MongoDB: " + e.getMessage());
                }
            }
            return new PermissionUserData(uuid, safeName(name), grants, nodes, now);
        }

        try (Connection conn = databaseManager.getConnection()) {

            try (PreparedStatement expire = conn.prepareStatement("UPDATE kaoscore_user_grants SET active = 0, removed_at = ?, removed_by_name = ?, removed_reason = ? WHERE active = 1 AND uuid = ? AND expires_at IS NOT NULL AND expires_at > 0 AND expires_at <= ?")) {
                expire.setLong(1, now);
                expire.setString(2, "SYSTEM");
                expire.setString(3, "Grant expirado");
                expire.setString(4, uuid.toString());
                expire.setLong(5, now);
                expire.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT id, group_name, created_at, expires_at FROM kaoscore_user_grants WHERE active = 1 AND uuid = ? AND (expires_at IS NULL OR expires_at = 0 OR expires_at > ?)")) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, now);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        String groupName = normalize(rs.getString("group_name"));
                        long createdAt = rs.getLong("createdAt");
                        long expiresAt = rs.getLong("expires_at");
                        if (rs.wasNull()) {
                            expiresAt = 0L;
                        }
                        if (!groupName.isEmpty()) {
                            grants.add(new GroupGrant(id, groupName, createdAt, expiresAt));
                        }
                    }
                }
            }

            try (PreparedStatement expire = conn.prepareStatement("DELETE FROM kaoscore_user_permissions WHERE uuid = ? AND expires_at IS NOT NULL AND expires_at > 0 AND expires_at <= ?")) {
                expire.setString(1, uuid.toString());
                expire.setLong(2, now);
                expire.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT permission, value, expires_at FROM kaoscore_user_permissions WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String perm = normalize(rs.getString("permission"));
                        boolean value = rs.getBoolean("value");
                        long expiresAt = rs.getLong("expires_at");
                        if (rs.wasNull()) {
                            expiresAt = 0L;
                        }
                        if (!perm.isEmpty()) {
                            nodes.add(new UserPermissionNode(perm, value, expiresAt));
                        }
                    }
                }
            }

            String safe = safeName(name);
            if (!safe.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE kaoscore_user_grants SET player_name = ? WHERE uuid = ?")) {
                    ps.setString(1, safe);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            }

        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao carregar user perms do DB: " + e.getMessage());
            }
            e.printStackTrace();
        }

        return new PermissionUserData(uuid, safeName(name), grants, nodes, now);
    }

    public void grant(UUID uuid, String playerName, String groupName, UUID giverUuid, String giverName, long durationMs, String reason) throws Exception {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid");
        }

        String name = safeName(playerName);
        String group = normalize(groupName);
        if (group.isEmpty()) {
            throw new IllegalArgumentException("group");
        }

        long now = System.currentTimeMillis();
        long expiresAt = durationMs > 0L ? now + durationMs : 0L;

        if (isMongo()) {
            try {
                Document doc = new Document("id", System.currentTimeMillis())
                        .append("uuid", uuid.toString())
                        .append("player_name", name)
                        .append("group_name", group)
                        .append("given_by_uuid", giverUuid != null ? giverUuid.toString() : null)
                        .append("given_by_name", safeName(giverName))
                        .append("reason", reason)
                        .append("created_at", now)
                        .append("expires_at", expiresAt)
                        .append("active", true);
                getCollection("kaoscore_user_grants").insertOne(doc);
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao aplicar grant no MongoDB: " + e.getMessage());
                }
            }
            return;
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO kaoscore_user_grants (uuid, player_name, group_name, given_by_uuid, given_by_name, reason, created_at, expires_at, active) VALUES (?,?,?,?,?,?,?,?,1)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, group);
            if (giverUuid == null) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, giverUuid.toString());
            }
            ps.setString(5, safeName(giverName));
            ps.setString(6, reason);
            ps.setLong(7, now);
            if (expiresAt <= 0L) {
                ps.setNull(8, Types.BIGINT);
            } else {
                ps.setLong(8, expiresAt);
            }
            ps.executeUpdate();
        }
    }

    public boolean ungrant(UUID uuid, String groupName, UUID removerUuid, String removerName, String removedReason) {
        if (uuid == null) {
            return false;
        }
        String group = normalize(groupName);
        if (group.isEmpty()) {
            return false;
        }

        long now = System.currentTimeMillis();

        if (isMongo()) {
            try {
                Bson filter = Filters.and(
                        Filters.eq("active", true),
                        Filters.eq("uuid", uuid.toString()),
                        Filters.eq("group_name", group)
                );
                Bson update = Updates.combine(
                        Updates.set("active", false),
                        Updates.set("removed_at", now),
                        Updates.set("removed_by_uuid", removerUuid != null ? removerUuid.toString() : null),
                        Updates.set("removed_by_name", safeName(removerName)),
                        Updates.set("removed_reason", removedReason)
                );
                return getCollection("kaoscore_user_grants").updateMany(filter, update).getMatchedCount() > 0;
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao ungrant no MongoDB: " + e.getMessage());
                }
                return false;
            }
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE kaoscore_user_grants SET active = 0, removed_at = ?, removed_by_uuid = ?, removed_by_name = ?, removed_reason = ? WHERE active = 1 AND uuid = ? AND group_name = ?")) {
            ps.setLong(1, now);
            if (removerUuid == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, removerUuid.toString());
            }
            ps.setString(3, safeName(removerName));
            ps.setString(4, removedReason);
            ps.setString(5, uuid.toString());
            ps.setString(6, group);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao ungrant: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean setUserPermission(UUID uuid, String permission, boolean value, long durationMs) {
        if (uuid == null) {
            return false;
        }

        String perm = normalize(permission);
        if (perm.isEmpty()) {
            return false;
        }

        long now = System.currentTimeMillis();
        long expiresAt = durationMs > 0L ? now + durationMs : 0L;

        if (isMongo()) {
            try {
                Document query = new Document("uuid", uuid.toString()).append("permission", perm);
                Document doc = new Document("uuid", uuid.toString())
                        .append("permission", perm)
                        .append("value", value)
                        .append("expires_at", expiresAt);
                getCollection("kaoscore_user_permissions").replaceOne(query, doc, new ReplaceOptions().upsert(true));
                return true;
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao setar perm user no MongoDB: " + e.getMessage());
                }
                return false;
            }
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("REPLACE INTO kaoscore_user_permissions (uuid, permission, value, expires_at) VALUES (?,?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, perm);
            ps.setBoolean(3, value);
            if (expiresAt <= 0L) {
                ps.setNull(4, Types.BIGINT);
            } else {
                ps.setLong(4, expiresAt);
            }
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao setar perm user: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeUserPermission(UUID uuid, String permission) {
        if (uuid == null) {
            return false;
        }

        String perm = normalize(permission);
        if (perm.isEmpty()) {
            return false;
        }

        if (isMongo()) {
            try {
                return getCollection("kaoscore_user_permissions").deleteOne(
                        new Document("uuid", uuid.toString()).append("permission", perm)
                ).getDeletedCount() > 0;
            } catch (Exception e) {
                if (plugin != null) {
                    Logger.warning("[KaosCore] Erro ao remover perm user no MongoDB: " + e.getMessage());
                }
                return false;
            }
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM kaoscore_user_permissions WHERE uuid = ? AND permission = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, perm);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            if (plugin != null) {
                Logger.warning("[KaosCore] Erro ao remover perm user: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safeName(String value) {
        if (value == null) {
            return "";
        }
        String s = value.trim();
        if (s.isEmpty()) {
            return "";
        }
        if (s.length() > 16) {
            s = s.substring(0, 16);
        }
        return s;
    }
}