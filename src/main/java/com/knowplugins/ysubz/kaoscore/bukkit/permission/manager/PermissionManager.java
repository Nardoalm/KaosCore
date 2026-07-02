package com.knowplugins.ysubz.kaoscore.bukkit.permission.manager;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.database.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.permission.*;
import com.knowplugins.ysubz.kaoscore.bukkit.permission.data.PermissionRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.permission.data.PermissionUserData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PermissionManager {

    private static final String DEFAULT_GROUP_NAME = "default";

    private final KaosCore plugin;
    private final PermissionRepository repository;

    private final ConcurrentMap<String, PermissionGroup> groupsByName;
    private final ConcurrentMap<String, Map<String, Boolean>> resolvedGroupPermissions;
    private final ConcurrentMap<UUID, EffectivePermissions> effectivePermissionsByUuid;

    private volatile String defaultGroupName;
    private volatile Field permissibleField;
    private volatile int expiryTaskId = -1;

    public PermissionManager(KaosCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.repository = new PermissionRepository(plugin, databaseManager);
        this.groupsByName = new ConcurrentHashMap<>();
        this.resolvedGroupPermissions = new ConcurrentHashMap<>();
        this.effectivePermissionsByUuid = new ConcurrentHashMap<>();
        this.defaultGroupName = DEFAULT_GROUP_NAME;
        syncFromPermissionsFile();
        reloadGroups();
        startExpiryWatcher();
    }

    private void syncFromPermissionsFile() {
        if (plugin == null) {
            return;
        }

        try {
            File file = new File(plugin.getDataFolder(), "permissions.yml");
            if (!file.exists()) {
                try {
                    plugin.saveResource("permissions.yml", false);
                } catch (Exception ignored) {
                }
            }

            if (!file.exists()) {
                return;
            }

            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            if (!cfg.getBoolean("sync.enabled", true)) {
                return;
            }

            ConfigurationSection groupsSection = cfg.getConfigurationSection("groups");
            if (groupsSection == null) {
                return;
            }

            String defaultGroup = null;
            for (String key : groupsSection.getKeys(false)) {
                ConfigurationSection g = groupsSection.getConfigurationSection(key);
                if (g != null && g.getBoolean("default", false)) {
                    defaultGroup = key;
                    break;
                }
            }

            if (defaultGroup != null) {
                repository.clearDefaultGroups();
            }

            for (String key : groupsSection.getKeys(false)) {
                if (key == null) {
                    continue;
                }

                ConfigurationSection g = groupsSection.getConfigurationSection(key);
                if (g == null) {
                    continue;
                }

                String name = key.trim();
                if (name.isEmpty()) {
                    continue;
                }

                repository.createGroup(name, false);

                Integer priority = g.contains("priority") ? g.getInt("priority") : 0;
                String prefix = g.getString("prefix", "");
                String suffix = g.getString("suffix", "");
                repository.setGroupMeta(name, priority, prefix, suffix, null);

                List<String> inheritsList = g.getStringList("inherits");
                if ((inheritsList == null || inheritsList.isEmpty()) && g.contains("inherit")) {
                    inheritsList = g.getStringList("inherit");
                }
                Set<String> inherits = new LinkedHashSet<>();
                if (inheritsList != null) {
                    for (String parent : inheritsList) {
                        String n = normalize(parent);
                        if (!n.isEmpty()) {
                            inherits.add(n);
                        }
                    }
                }
                repository.replaceGroupInherits(name, inherits);

                Map<String, Boolean> perms = new LinkedHashMap<>();
                Object raw = g.get("permissions");
                if (raw instanceof ConfigurationSection) {
                    ConfigurationSection ps = (ConfigurationSection) raw;
                    for (String permKey : ps.getKeys(false)) {
                        String perm = normalize(permKey);
                        if (perm.isEmpty()) {
                            continue;
                        }
                        perms.put(perm, ps.getBoolean(permKey, true));
                    }
                } else {
                    List<String> list = g.getStringList("permissions");
                    if (list != null) {
                        for (String line : list) {
                            if (line == null) continue;
                            String trimmed = line.trim();
                            if (trimmed.isEmpty()) continue;
                            boolean value = true;
                            if (trimmed.startsWith("-")) {
                                value = false;
                                trimmed = trimmed.substring(1).trim();
                            }
                            String perm = normalize(trimmed);
                            if (!perm.isEmpty()) {
                                perms.put(perm, value);
                            }
                        }
                    }
                }
                repository.replaceGroupPermissions(name, perms);
            }

            if (defaultGroup != null) {
                repository.setGroupMeta(defaultGroup, null, null, null, true);
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("[KaosCore] Falha ao sincronizar permissions.yml: " + e.getMessage());
            }
        }
    }

    private void startExpiryWatcher() {
        if (plugin == null) {
            return;
        }
        if (expiryTaskId != -1) {
            return;
        }

        expiryTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online == null || !online.isOnline()) {
                    continue;
                }

                UUID uuid = online.getUniqueId();
                if (uuid == null) {
                    continue;
                }

                EffectivePermissions effective = effectivePermissionsByUuid.get(uuid);
                if (effective == null) {
                    continue;
                }

                long nextExpiryAt = effective.getNextExpiryAt();
                if (nextExpiryAt > 0L && nextExpiryAt <= now) {
                    refreshUser(uuid, online.getName());
                }
            }
        }, 20L, 20L).getTaskId();
    }

    public void reloadGroups() {
        groupsByName.clear();
        resolvedGroupPermissions.clear();

        Map<String, PermissionGroup> loaded = repository.loadGroups();
        if (loaded != null) {
            groupsByName.putAll(loaded);
        }

        ensureDefaultGroup();
        refreshAllOnline();
    }

    public void reload() {
        syncFromPermissionsFile();
        reloadGroups();
    }

    public void refreshAllOnline() {
        if (plugin == null) {
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::refreshAllOnline);
            return;
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != null && online.isOnline()) {
                refreshUser(online.getUniqueId(), online.getName());
            }
        }
    }

    private void ensureDefaultGroup() {
        PermissionGroup def = null;
        for (PermissionGroup group : groupsByName.values()) {
            if (group != null && group.isDefaultGroup()) {
                def = group;
                break;
            }
        }

        if (def != null && !def.getName().trim().isEmpty()) {
            defaultGroupName = def.getName();
            return;
        }

        try {
            repository.createGroup(DEFAULT_GROUP_NAME, true);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("[KaosCore] Falha ao criar grupo default: " + e.getMessage());
            }
        }

        Map<String, PermissionGroup> loaded = repository.loadGroups();
        groupsByName.clear();
        if (loaded != null) {
            groupsByName.putAll(loaded);
        }

        for (PermissionGroup group : groupsByName.values()) {
            if (group != null && group.isDefaultGroup()) {
                defaultGroupName = group.getName();
                return;
            }
        }

        defaultGroupName = DEFAULT_GROUP_NAME;
    }

    public PermissionGroup getGroup(String name) {
        String key = normalize(name);
        if (key.isEmpty()) {
            return null;
        }
        return groupsByName.get(key);
    }

    public Set<String> getGroupNames() {
        Set<String> out = new LinkedHashSet<>();
        out.addAll(groupsByName.keySet());
        return out;
    }

    public String getDefaultGroupName() {
        return defaultGroupName;
    }

    public void preloadUser(UUID uuid, String name) {
        PermissionUserData data = repository.loadUser(uuid, name);
        EffectivePermissions effective = computeEffective(data);
        if (uuid != null && effective != null) {
            effectivePermissionsByUuid.put(uuid, effective);
        }
    }

    public void unloadUser(UUID uuid) {
        if (uuid == null) {
            return;
        }
        effectivePermissionsByUuid.remove(uuid);
    }

    public void handleLogin(Player player) {
        inject(player);
    }

    public void handleJoin(Player player) {
        inject(player);

        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (uuid == null) {
            return;
        }

        // Ensure data exists even if AsyncPreLogin didn't run.
        if (!effectivePermissionsByUuid.containsKey(uuid)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                preloadUser(uuid, player.getName());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        try {
                            player.recalculatePermissions();
                        } catch (Exception ignored) {
                        }
                    }
                });
            });
        } else {
            try {
                player.recalculatePermissions();
            } catch (Exception ignored) {
            }
        }
    }

    public void refreshUser(UUID uuid, String name) {
        if (uuid == null) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                preloadUser(uuid, name);
                Bukkit.getScheduler().runTask(plugin, () -> recalcIfOnline(uuid));
            });
            return;
        }

        preloadUser(uuid, name);
        Bukkit.getScheduler().runTask(plugin, () -> recalcIfOnline(uuid));
    }

    public Boolean getPermissionValue(UUID uuid, String permission) {
        if (uuid == null || permission == null) {
            return null;
        }

        EffectivePermissions effective = effectivePermissionsByUuid.get(uuid);
        if (effective == null) {
            return null;
        }

        return matchPermission(effective.getPermissionMap(), permission);
    }

    public EffectivePermissions getEffective(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return effectivePermissionsByUuid.get(uuid);
    }

    public Boolean getGroupPermissionValue(String groupName, String permission) {
        String key = normalize(groupName);
        if (key.isEmpty() || permission == null) {
            return null;
        }
        Map<String, Boolean> resolved = resolveGroupPermissions(key, new HashSet<>());
        return matchPermission(resolved, permission);
    }

    public boolean createGroup(String name, boolean isDefault) {
        try {
            repository.createGroup(name, isDefault);
            reloadGroups();
            return true;
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("[KaosCore] Erro ao criar grupo: " + e.getMessage());
            }
            return false;
        }
    }

    public boolean deleteGroup(String name) {
        boolean ok = repository.deleteGroup(name);
        reloadGroups();
        return ok;
    }

    public boolean setGroupPriority(String name, int priority) {
        boolean ok = repository.setGroupMeta(name, priority, null, null, null);
        reloadGroups();
        return ok;
    }

    public boolean setGroupPrefix(String name, String prefix) {
        boolean ok = repository.setGroupMeta(name, null, prefix == null ? "" : prefix, null, null);
        reloadGroups();
        return ok;
    }

    public boolean setGroupSuffix(String name, String suffix) {
        boolean ok = repository.setGroupMeta(name, null, null, suffix == null ? "" : suffix, null);
        reloadGroups();
        return ok;
    }

    public boolean setDefaultGroup(String name) {
        boolean ok = repository.setGroupMeta(name, null, null, null, true);
        reloadGroups();
        return ok;
    }

    public boolean addGroupPermission(String groupName, String permission, boolean value) {
        boolean ok = repository.setGroupPermission(groupName, permission, value);
        reloadGroups();
        return ok;
    }

    public boolean removeGroupPermission(String groupName, String permission) {
        boolean ok = repository.removeGroupPermission(groupName, permission);
        reloadGroups();
        return ok;
    }

    public boolean addGroupInherit(String groupName, String inheritGroup) {
        boolean ok = repository.addInherit(groupName, inheritGroup);
        reloadGroups();
        return ok;
    }

    public boolean removeGroupInherit(String groupName, String inheritGroup) {
        boolean ok = repository.removeInherit(groupName, inheritGroup);
        reloadGroups();
        return ok;
    }

    public void grant(UUID uuid, String playerName, String groupName, UUID giverUuid, String giverName, long durationMs, String reason) throws Exception {
        repository.grant(uuid, playerName, groupName, giverUuid, giverName, durationMs, reason);
        refreshUser(uuid, playerName);
    }

    public boolean ungrant(UUID uuid, String playerName, String groupName, UUID removerUuid, String removerName, String removedReason) {
        boolean ok = repository.ungrant(uuid, groupName, removerUuid, removerName, removedReason);
        refreshUser(uuid, playerName);
        return ok;
    }

    public boolean setUserPermission(UUID uuid, String playerName, String permission, boolean value, long durationMs) {
        boolean ok = repository.setUserPermission(uuid, permission, value, durationMs);
        refreshUser(uuid, playerName);
        return ok;
    }

    public boolean removeUserPermission(UUID uuid, String playerName, String permission) {
        boolean ok = repository.removeUserPermission(uuid, permission);
        refreshUser(uuid, playerName);
        return ok;
    }

    private void recalcIfOnline(UUID uuid) {
        if (uuid == null) {
            return;
        }

        Player online = Bukkit.getPlayer(uuid);
        if (online != null && online.isOnline()) {
            try {
                online.recalculatePermissions();
            } catch (Exception ignored) {
            }
        }
    }

    private EffectivePermissions computeEffective(PermissionUserData data) {
        if (data == null || data.getUuid() == null) {
            return new EffectivePermissions(null,
                    "",
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    "",
                    "",
                    0L);
        }

        long now = System.currentTimeMillis();
        Set<String> directGroups = new LinkedHashSet<>();
        Map<String, Long> directGroupExpiryAt = new LinkedHashMap<>();
        long nextExpiryAt = 0L;

        for (GroupGrant grant : data.getGrants()) {
            if (grant == null || grant.isExpired(now)) {
                continue;
            }
            String groupName = normalize(grant.getGroupName());
            if (!groupName.isEmpty()) {
                directGroups.add(groupName);
                long expiresAt = grant.getExpiresAt();
                if (expiresAt > 0L && expiresAt > now) {
                    if (nextExpiryAt <= 0L || expiresAt < nextExpiryAt) {
                        nextExpiryAt = expiresAt;
                    }
                }

                Long existing = directGroupExpiryAt.get(groupName);
                if (expiresAt <= 0L) {
                    directGroupExpiryAt.put(groupName, 0L);
                } else if (existing == null) {
                    directGroupExpiryAt.put(groupName, expiresAt);
                } else if (existing > 0L) {
                    directGroupExpiryAt.put(groupName, Math.max(existing, expiresAt));
                }
            }
        }

        if (directGroups.isEmpty()) {
            String def = normalize(defaultGroupName);
            if (!def.isEmpty()) {
                directGroups.add(def);
                directGroupExpiryAt.put(def, 0L);
            }
        }

        List<PermissionGroup> groups = new ArrayList<>();
        for (String groupName : directGroups) {
            PermissionGroup group = groupsByName.get(normalize(groupName));
            if (group != null) {
                groups.add(group);
            }
        }

        groups.sort(Comparator.comparingInt(PermissionGroup::getPriority));

        PermissionGroup primary = null;
        for (PermissionGroup group : groups) {
            if (group == null) {
                continue;
            }
            if (primary == null) {
                primary = group;
                continue;
            }
            if (group.getPriority() > primary.getPriority()) {
                primary = group;
            }
        }

        Map<String, Boolean> permissionMap = new LinkedHashMap<>();
        for (PermissionGroup group : groups) {
            if (group == null) {
                continue;
            }
            permissionMap.putAll(resolveGroupPermissions(group.getName(), new HashSet<>()));
        }

        Map<String, Long> userPermissionExpiryAt = new LinkedHashMap<>();
        for (UserPermissionNode node : data.getPermissions()) {
            if (node == null || node.isExpired(now)) {
                continue;
            }
            String perm = normalize(node.getPermission());
            if (!perm.isEmpty()) {
                permissionMap.put(perm, node.getValue());
                long expiresAt = node.getExpiresAt();
                userPermissionExpiryAt.put(perm, expiresAt <= 0L ? 0L : expiresAt);
                if (expiresAt > 0L && expiresAt > now) {
                    if (nextExpiryAt <= 0L || expiresAt < nextExpiryAt) {
                        nextExpiryAt = expiresAt;
                    }
                }
            }
        }

        String primaryName = primary == null ? "" : primary.getName();
        String prefix = primary == null ? "" : primary.getPrefix();
        String suffix = primary == null ? "" : primary.getSuffix();

        Set<String> inheritedGroups = new LinkedHashSet<>();
        for (String directGroup : directGroups) {
            collectInherits(directGroup, inheritedGroups, new HashSet<>());
        }

        return new EffectivePermissions(data.getUuid(),
                primaryName,
                directGroups,
                inheritedGroups,
                permissionMap,
                directGroupExpiryAt,
                userPermissionExpiryAt,
                prefix,
                suffix,
                nextExpiryAt);
    }

    private void collectInherits(String groupName, Set<String> out, Set<String> visiting) {
        String key = normalize(groupName);
        if (key.isEmpty()) {
            return;
        }
        if (!visiting.add(key)) {
            return;
        }
        out.add(key);
        PermissionGroup group = groupsByName.get(key);
        if (group != null) {
            for (String inherit : group.getInherits()) {
                collectInherits(inherit, out, visiting);
            }
        }
    }

    private Map<String, Boolean> resolveGroupPermissions(String groupName, Set<String> visiting) {
        String key = normalize(groupName);
        if (key.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Boolean> cached = resolvedGroupPermissions.get(key);
        if (cached != null) {
            return cached;
        }

        PermissionGroup group = groupsByName.get(key);
        if (group == null) {
            return Collections.emptyMap();
        }

        if (!visiting.add(key)) {
            return Collections.emptyMap();
        }

        Map<String, Boolean> resolved = new LinkedHashMap<>();
        for (String inheritName : group.getInherits()) {
            String inheritKey = normalize(inheritName);
            if (inheritKey.isEmpty() || visiting.contains(inheritKey)) {
                continue;
            }
            resolved.putAll(resolveGroupPermissions(inheritKey, visiting));
        }

        resolved.putAll(group.getPermissions());
        visiting.remove(key);

        resolvedGroupPermissions.put(key, resolved);
        return resolved;
    }

    private void inject(Player player) {
        if (player == null) {
            return;
        }

        Field field = permissibleField;
        if (field == null) {
            field = findPermissibleField(player.getClass());
            permissibleField = field;
        }

        if (field == null) {
            return;
        }

        try {
            Object current = field.get(player);
            if (current instanceof KaosPermissible) {
                return;
            }

            KaosPermissible permissible = new KaosPermissible(player, this, player.getUniqueId());
            field.set(player, permissible);
            try {
                permissible.recalculatePermissions();
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("[KaosCore] Falha ao injetar permissiveis: " + e.getMessage());
            }
        }
    }

    private Field findPermissibleField(Class<?> craftPlayerClass) {
        Class<?> current = craftPlayerClass;
        while (current != null && current != Object.class) {
            try {
                Field f = current.getDeclaredField("perm");
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Boolean matchPermission(Map<String, Boolean> map, String permission) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        String node = normalize(permission);
        if (node.isEmpty()) {
            return null;
        }

        if (map.containsKey(node)) {
            return map.get(node);
        }

        int last = node.length();
        while (true) {
            int dot = node.lastIndexOf('.', last - 1);
            if (dot <= 0) {
                break;
            }
            String wildcard = node.substring(0, dot) + ".*";
            if (map.containsKey(wildcard)) {
                return map.get(wildcard);
            }
            last = dot;
        }

        if (map.containsKey("*")) {
            return map.get("*");
        }

        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
