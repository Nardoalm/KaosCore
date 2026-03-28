package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class BungeePermissionService {

    private static final String DEFAULT_GROUP_NAME = "default";

    private final BungeeKaosCore plugin;
    private final BungeePermissionRepository repository;
    private final Map<String, PermissionGroup> groupsByName = new ConcurrentHashMap<>();
    private String defaultGroupName = DEFAULT_GROUP_NAME;

    BungeePermissionService(BungeeKaosCore plugin, BungeeDatabase database, Configuration permissionsConfig) {
        this.plugin = plugin;
        this.repository = new BungeePermissionRepository(plugin, database);
        syncFromPermissionsFile(permissionsConfig);
        reloadGroups();
    }

    void reload(Configuration permissionsConfig) {
        syncFromPermissionsFile(permissionsConfig);
        reloadGroups();
    }

    void reloadGroups() {
        groupsByName.clear();
        groupsByName.putAll(repository.loadGroups());
        ensureDefaultGroup();
    }

    PermissionGroup getGroup(String name) {
        return groupsByName.get(Texts.normalize(name));
    }

    Set<String> getGroupNames() {
        return new LinkedHashSet<>(groupsByName.keySet());
    }

    boolean createGroup(String name, boolean isDefault) {
        try {
            repository.createGroup(name, isDefault);
            reloadGroups();
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCoreBungee] Falha ao criar grupo: " + exception.getMessage());
            return false;
        }
    }

    boolean deleteGroup(String name) {
        boolean result = repository.deleteGroup(name);
        reloadGroups();
        return result;
    }

    boolean setDefaultGroup(String name) {
        boolean result = repository.setGroupMeta(name, null, null, null, true);
        reloadGroups();
        return result;
    }

    boolean setGroupPriority(String name, int priority) {
        boolean result = repository.setGroupMeta(name, priority, null, null, null);
        reloadGroups();
        return result;
    }

    boolean setGroupPrefix(String name, String prefix) {
        boolean result = repository.setGroupMeta(name, null, prefix, null, null);
        reloadGroups();
        return result;
    }

    boolean setGroupSuffix(String name, String suffix) {
        boolean result = repository.setGroupMeta(name, null, null, suffix, null);
        reloadGroups();
        return result;
    }

    boolean addGroupPermission(String groupName, String permission, boolean value) {
        boolean result = repository.setGroupPermission(groupName, permission, value);
        reloadGroups();
        return result;
    }

    boolean removeGroupPermission(String groupName, String permission) {
        boolean result = repository.removeGroupPermission(groupName, permission);
        reloadGroups();
        return result;
    }

    boolean addGroupInherit(String groupName, String inheritGroup) {
        boolean result = repository.addInherit(groupName, inheritGroup);
        reloadGroups();
        return result;
    }

    boolean removeGroupInherit(String groupName, String inheritGroup) {
        boolean result = repository.removeInherit(groupName, inheritGroup);
        reloadGroups();
        return result;
    }

    void grant(UUID uuid, String playerName, String groupName, UUID giverUuid, String giverName, long durationMs, String reason) throws Exception {
        repository.grant(uuid, playerName, groupName, giverUuid, giverName, durationMs, reason);
    }

    boolean ungrant(UUID uuid, String playerName, String groupName, UUID removerUuid, String removerName, String removedReason) {
        return repository.ungrant(uuid, groupName, removerUuid, removerName, removedReason);
    }

    EffectivePermissions getEffective(UUID uuid, String name) {
        return computeEffective(repository.loadUser(uuid, name));
    }

    Boolean getPermissionValue(UUID uuid, String name, String permission) {
        if (uuid == null || Texts.normalize(permission).isEmpty()) {
            return null;
        }
        return matchPermission(getEffective(uuid, name).getPermissionMap(), permission);
    }

    boolean hasPermission(CommandSender sender, String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            return true;
        }
        if (sender == null) {
            return false;
        }
        if (sender.hasPermission(permission)) {
            return true;
        }
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            Boolean value = getPermissionValue(player.getUniqueId(), player.getName(), permission);
            return value != null && value;
        }
        return false;
    }

    private void ensureDefaultGroup() {
        for (PermissionGroup group : groupsByName.values()) {
            if (group != null && group.isDefaultGroup()) {
                defaultGroupName = group.getName();
                return;
            }
        }
        defaultGroupName = groupsByName.containsKey("membro") ? "membro" : DEFAULT_GROUP_NAME;
    }

    private EffectivePermissions computeEffective(PermissionUserData data) {
        if (data == null || data.getUuid() == null) {
            return new EffectivePermissions(null, "", Collections.<String>emptySet(), Collections.<String>emptySet(), Collections.<String, Boolean>emptyMap(), "", "", 0L);
        }

        long now = System.currentTimeMillis();
        Set<String> directGroups = new LinkedHashSet<>();
        long nextExpiryAt = 0L;
        for (GroupGrant grant : data.getGrants()) {
            if (grant == null || grant.isExpired(now)) {
                continue;
            }
            String groupName = Texts.normalize(grant.getGroupName());
            if (groupName.isEmpty()) {
                continue;
            }
            directGroups.add(groupName);
            long expiresAt = grant.getExpiresAt();
            if (expiresAt > 0L && (nextExpiryAt <= 0L || expiresAt < nextExpiryAt)) {
                nextExpiryAt = expiresAt;
            }
        }

        if (directGroups.isEmpty() && !defaultGroupName.isEmpty()) {
            directGroups.add(defaultGroupName);
        }

        List<PermissionGroup> groups = new ArrayList<>();
        for (String groupName : directGroups) {
            PermissionGroup group = groupsByName.get(Texts.normalize(groupName));
            if (group != null) {
                groups.add(group);
            }
        }
        groups.sort(Comparator.comparingInt(PermissionGroup::getPriority));

        PermissionGroup primary = null;
        for (PermissionGroup group : groups) {
            if (primary == null || group.getPriority() > primary.getPriority()) {
                primary = group;
            }
        }

        Map<String, Boolean> permissionMap = new LinkedHashMap<>();
        Set<String> inherited = new LinkedHashSet<>();
        for (PermissionGroup group : groups) {
            permissionMap.putAll(resolveGroupPermissions(group.getName(), new HashSet<String>(), inherited));
        }
        for (UserPermissionNode node : data.getPermissions()) {
            if (node == null || node.isExpired(now)) {
                continue;
            }
            permissionMap.put(Texts.normalize(node.getPermission()), node.getValue());
            long expiresAt = node.getExpiresAt();
            if (expiresAt > 0L && (nextExpiryAt <= 0L || expiresAt < nextExpiryAt)) {
                nextExpiryAt = expiresAt;
            }
        }

        return new EffectivePermissions(data.getUuid(), primary == null ? "" : primary.getName(), directGroups, inherited, permissionMap, primary == null ? "" : primary.getPrefix(), primary == null ? "" : primary.getSuffix(), nextExpiryAt);
    }

    private Map<String, Boolean> resolveGroupPermissions(String groupName, Set<String> visiting, Set<String> inherited) {
        String key = Texts.normalize(groupName);
        if (key.isEmpty() || !visiting.add(key)) {
            return Collections.emptyMap();
        }
        inherited.add(key);
        PermissionGroup group = groupsByName.get(key);
        if (group == null) {
            visiting.remove(key);
            return Collections.emptyMap();
        }
        Map<String, Boolean> resolved = new LinkedHashMap<>();
        for (String inherit : group.getInherits()) {
            resolved.putAll(resolveGroupPermissions(inherit, visiting, inherited));
        }
        resolved.putAll(group.getPermissions());
        visiting.remove(key);
        return resolved;
    }

    private static Boolean matchPermission(Map<String, Boolean> map, String permission) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        String node = Texts.normalize(permission);
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
        return map.get("*");
    }

    private void syncFromPermissionsFile(Configuration config) {
        if (config == null || !config.getBoolean("sync.enabled", true)) {
            return;
        }
        Configuration groups = config.getSection("groups");
        if (groups == null) {
            return;
        }

        String defaultGroup = null;
        for (String key : groups.getKeys()) {
            Configuration section = groups.getSection(key);
            if (section != null && section.getBoolean("default", false)) {
                defaultGroup = key;
                break;
            }
        }

        if (defaultGroup != null) {
            repository.clearDefaultGroups();
        }

        for (String key : groups.getKeys()) {
            Configuration section = groups.getSection(key);
            if (section == null) {
                continue;
            }
            String name = key.trim();
            if (name.isEmpty()) {
                continue;
            }
            try {
                repository.createGroup(name, false);
            } catch (Exception ignored) {
            }

            repository.setGroupMeta(name, section.contains("priority") ? section.getInt("priority") : 0, section.getString("prefix", ""), section.getString("suffix", ""), null);

            Set<String> inherits = new LinkedHashSet<>();
            for (String parent : section.getStringList("inherits")) {
                String normalized = Texts.normalize(parent);
                if (!normalized.isEmpty()) {
                    inherits.add(normalized);
                }
            }
            repository.replaceGroupInherits(name, inherits);

            Map<String, Boolean> permissions = new LinkedHashMap<>();
            for (String raw : section.getStringList("permissions")) {
                String value = Texts.safe(raw).trim();
                if (value.isEmpty()) {
                    continue;
                }
                boolean allow = true;
                if (value.startsWith("-")) {
                    allow = false;
                    value = value.substring(1).trim();
                }
                String normalized = Texts.normalize(value);
                if (!normalized.isEmpty()) {
                    permissions.put(normalized, allow);
                }
            }
            repository.replaceGroupPermissions(name, permissions);
        }

        if (defaultGroup != null) {
            repository.setGroupMeta(defaultGroup, null, null, null, true);
        }
    }
}


