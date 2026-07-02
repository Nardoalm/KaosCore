package com.knowplugins.ysubz.kaoscore.bungee.service;

import com.knowplugins.ysubz.kaoscore.bungee.BungeeDatabase;
import com.knowplugins.ysubz.kaoscore.bungee.EffectivePermissions;
import com.knowplugins.ysubz.kaoscore.bungee.PermissionGroup;
import com.knowplugins.ysubz.kaoscore.bungee.PermissionUserData;
import com.knowplugins.ysubz.kaoscore.bungee.GroupGrant;
import com.knowplugins.ysubz.kaoscore.bungee.UserPermissionNode;
import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import com.knowplugins.ysubz.kaoscore.bungee.BungeePermissionRepository;
import com.knowplugins.ysubz.kaoscore.bungee.Texts;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BungeePermissionService {

    private final BungeeKaosCore plugin;
    private final BungeeDatabase database;
    private final BungeePermissionRepository repository;
    private final Map<String, PermissionGroup> groups = new ConcurrentHashMap<>();

    public BungeePermissionService(BungeeKaosCore plugin, BungeeDatabase database, Configuration permissionsConfig) {
        this.plugin = plugin;
        this.database = database;
        this.repository = new BungeePermissionRepository(plugin, database);
        reload(permissionsConfig);
    }

    public synchronized void reload(Configuration permissionsConfig) {
        groups.clear();
        groups.putAll(repository.loadGroups());
    }

    public Set<String> getGroupNames() {
        return new LinkedHashSet<>(groups.keySet());
    }

    public PermissionGroup getGroup(String name) {
        return groups.get(Texts.normalize(name));
    }

    public boolean createGroup(String name, boolean isDefault) {
        try {
            repository.createGroup(name, isDefault);
            reload(null);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("[KaosCore] Falha ao criar grupo: " + exception.getMessage());
            return false;
        }
    }

    public boolean deleteGroup(String name) {
        boolean result = repository.deleteGroup(name);
        if (result) {
            reload(null);
        }
        return result;
    }

    public boolean setDefaultGroup(String name) {
        PermissionGroup group = getGroup(name);
        if (group == null) {
            return false;
        }
        if (!repository.clearDefaultGroups()) {
            return false;
        }
        boolean result = repository.setGroupMeta(group.getName(), null, null, null, true);
        if (result) {
            reload(null);
        }
        return result;
    }

    public boolean setGroupPriority(String name, int priority) {
        boolean result = repository.setGroupMeta(name, priority, null, null, null);
        if (result) {
            reload(null);
        }
        return result;
    }

    public boolean setGroupPrefix(String name, String prefix) {
        boolean result = repository.setGroupMeta(name, null, prefix, null, null);
        if (result) {
            reload(null);
        }
        return result;
    }

    public boolean setGroupSuffix(String name, String suffix) {
        boolean result = repository.setGroupMeta(name, null, null, suffix, null);
        if (result) {
            reload(null);
        }
        return result;
    }

    public boolean addGroupPermission(String group, String permission, boolean value) {
        boolean result = repository.setGroupPermission(group, permission, value);
        if (result) {
            reload(null);
        }
        return result;
    }

    public boolean removeGroupPermission(String group, String permission) {
        boolean result = repository.removeGroupPermission(group, permission);
        if (result) {
            reload(null);
        }
        return result;
    }

    public boolean addGroupInherit(String group, String inherit) {
        boolean result = repository.addInherit(group, inherit);
        if (result) {
            reload(null);
        }
        return result;
    }

    public boolean removeGroupInherit(String group, String inherit) {
        boolean result = repository.removeInherit(group, inherit);
        if (result) {
            reload(null);
        }
        return result;
    }

    public boolean hasPermission(CommandSender sender, String permission) {
        if (sender == null) {
            return false;
        }
        if (!(sender instanceof ProxiedPlayer)) {
            return true;
        }
        return hasPermission(((ProxiedPlayer) sender).getUniqueId(), sender.getName(), permission);
    }

    public boolean hasPermission(UUID uuid, String name, String permission) {
        String target = Texts.normalize(permission);
        if (target.isEmpty()) {
            return true;
        }

        if (uuid == null) {
            return false;
        }

        PermissionUserData data = repository.loadUser(uuid, name);
        for (UserPermissionNode node : data.getPermissions()) {
            if (permissionMatches(node.getPermission(), target)) {
                return node.getValue();
            }
        }

        for (String groupName : resolveGroups(data)) {
            PermissionGroup group = groups.get(Texts.normalize(groupName));
            if (group == null) {
                continue;
            }
            if (groupHasPermission(group, target, new LinkedHashSet<String>())) {
                return true;
            }
        }
        return false;
    }

    public boolean ungrant(UUID uuid, String playerName, String groupName, UUID removerUuid, String removerName, String reason) {
        return repository.ungrant(uuid, groupName, removerUuid, removerName, reason);
    }

    public void grant(UUID uuid, String playerName, String groupName, UUID giverUuid, String giverName, long durationMs, String reason) throws Exception {
        repository.grant(uuid, playerName, groupName, giverUuid, giverName, durationMs, reason);
    }

    public PermissionUserData loadUser(UUID uuid, String name) {
        return repository.loadUser(uuid, name);
    }

    public EffectivePermissions resolveEffective(UUID uuid, String name) {
        PermissionUserData data = repository.loadUser(uuid, name);
        Map<String, Boolean> permissions = new LinkedHashMap<>();
        Set<String> directGroups = new LinkedHashSet<>();
        Set<String> inheritedGroups = new LinkedHashSet<>();
        long nextExpiry = 0L;

        for (GroupGrant grant : data.getGrants()) {
            directGroups.add(Texts.normalize(grant.getGroupName()));
            if (grant.getExpiresAt() > 0L && (nextExpiry == 0L || grant.getExpiresAt() < nextExpiry)) {
                nextExpiry = grant.getExpiresAt();
            }
        }
        for (UserPermissionNode node : data.getPermissions()) {
            permissions.put(Texts.normalize(node.getPermission()), node.getValue());
            if (node.getExpiresAt() > 0L && (nextExpiry == 0L || node.getExpiresAt() < nextExpiry)) {
                nextExpiry = node.getExpiresAt();
            }
        }

        for (String groupName : resolveGroups(data)) {
            collectGroupPermissions(groups.get(Texts.normalize(groupName)), permissions, inheritedGroups, new LinkedHashSet<String>());
        }

        return new EffectivePermissions(uuid, findPrimaryGroup(data), directGroups, inheritedGroups, permissions, "", "", nextExpiry);
    }

    private String findPrimaryGroup(PermissionUserData data) {
        for (GroupGrant grant : data.getGrants()) {
            String groupName = Texts.normalize(grant.getGroupName());
            PermissionGroup group = groups.get(groupName);
            if (group != null && group.isDefaultGroup()) {
                return group.getName();
            }
        }
        for (GroupGrant grant : data.getGrants()) {
            return Texts.normalize(grant.getGroupName());
        }
        return "";
    }

    private Collection<String> resolveGroups(PermissionUserData data) {
        if (data == null) {
            return Collections.emptyList();
        }
        ArrayList<String> result = new ArrayList<>();
        for (GroupGrant grant : data.getGrants()) {
            result.add(grant.getGroupName());
        }
        return result;
    }

    private boolean groupHasPermission(PermissionGroup group, String permission, Set<String> visited) {
        if (group == null) {
            return false;
        }
        String groupName = Texts.normalize(group.getName());
        if (!visited.add(groupName)) {
            return false;
        }
        for (Map.Entry<String, Boolean> entry : group.getPermissions().entrySet()) {
            if (permissionMatches(entry.getKey(), permission)) {
                return entry.getValue() == null || entry.getValue();
            }
        }
        for (String inherit : group.getInherits()) {
            PermissionGroup inherited = groups.get(Texts.normalize(inherit));
            if (groupHasPermission(inherited, permission, visited)) {
                return true;
            }
        }
        return false;
    }

    private void collectGroupPermissions(PermissionGroup group, Map<String, Boolean> permissions, Set<String> inheritedGroups, Set<String> visited) {
        if (group == null) {
            return;
        }
        String groupName = Texts.normalize(group.getName());
        if (!visited.add(groupName)) {
            return;
        }
        inheritedGroups.add(groupName);
        for (Map.Entry<String, Boolean> entry : group.getPermissions().entrySet()) {
            permissions.put(entry.getKey(), entry.getValue());
        }
        for (String inherit : group.getInherits()) {
            collectGroupPermissions(groups.get(Texts.normalize(inherit)), permissions, inheritedGroups, visited);
        }
    }

    private boolean permissionMatches(String stored, String requested) {
        String normalizedStored = Texts.normalize(stored);
        String normalizedRequested = Texts.normalize(requested);
        if (normalizedStored.equals(normalizedRequested)) {
            return true;
        }
        if (normalizedStored.endsWith(".*")) {
            String prefix = normalizedStored.substring(0, normalizedStored.length() - 2);
            return normalizedRequested.startsWith(prefix + ".");
        }
        return false;
    }
}
