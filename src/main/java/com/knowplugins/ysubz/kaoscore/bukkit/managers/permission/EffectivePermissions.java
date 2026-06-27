package com.knowplugins.ysubz.kaoscore.bukkit.managers.permission;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EffectivePermissions {

    private final UUID uuid;
    private final String primaryGroup;
    private final Set<String> directGroups;
    private final Set<String> inheritedGroups;
    private final Map<String, Boolean> permissionMap;
    private final Map<String, Long> directGroupExpiryAtByName;
    private final Map<String, Long> userPermissionExpiryAtByNode;
    private final String prefix;
    private final String suffix;
    private final long nextExpiryAt;

    public EffectivePermissions(UUID uuid,
                                String primaryGroup,
                                Set<String> directGroups,
                                Set<String> inheritedGroups,
                                Map<String, Boolean> permissionMap,
                                Map<String, Long> directGroupExpiryAtByName,
                                Map<String, Long> userPermissionExpiryAtByNode,
                                String prefix,
                                String suffix,
                                long nextExpiryAt) {
        this.uuid = uuid;
        this.primaryGroup = primaryGroup == null ? "" : primaryGroup;
        this.directGroups = directGroups == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(directGroups));
        this.inheritedGroups = inheritedGroups == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(inheritedGroups));
        this.permissionMap = permissionMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(permissionMap);
        this.directGroupExpiryAtByName = directGroupExpiryAtByName == null ? Collections.emptyMap() : Collections.unmodifiableMap(directGroupExpiryAtByName);
        this.userPermissionExpiryAtByNode = userPermissionExpiryAtByNode == null ? Collections.emptyMap() : Collections.unmodifiableMap(userPermissionExpiryAtByNode);
        this.prefix = prefix == null ? "" : prefix;
        this.suffix = suffix == null ? "" : suffix;
        this.nextExpiryAt = nextExpiryAt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPrimaryGroup() {
        return primaryGroup;
    }

    public Set<String> getDirectGroups() {
        return directGroups;
    }

    public Set<String> getInheritedGroups() {
        return inheritedGroups;
    }

    public Map<String, Boolean> getPermissionMap() {
        return permissionMap;
    }

    public Map<String, Long> getDirectGroupExpiryAtByName() {
        return directGroupExpiryAtByName;
    }

    public Map<String, Long> getUserPermissionExpiryAtByNode() {
        return userPermissionExpiryAtByNode;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public long getNextExpiryAt() {
        return nextExpiryAt;
    }
}
