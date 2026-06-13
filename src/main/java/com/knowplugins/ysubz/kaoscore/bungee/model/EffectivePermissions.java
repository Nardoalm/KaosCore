package com.knowplugins.ysubz.kaoscore.bungee;

import java.util.Collections;
import java.util.LinkedHashMap;
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
    private final String prefix;
    private final String suffix;
    private final long nextExpiryAt;

    public EffectivePermissions(UUID uuid, String primaryGroup, Set<String> directGroups, Set<String> inheritedGroups, Map<String, Boolean> permissionMap, String prefix, String suffix, long nextExpiryAt) {
        this.uuid = uuid;
        this.primaryGroup = primaryGroup == null ? "" : primaryGroup;
        this.directGroups = directGroups == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(directGroups));
        this.inheritedGroups = inheritedGroups == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(inheritedGroups));
        this.permissionMap = permissionMap == null ? Collections.<String, Boolean>emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(permissionMap));
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

    public Map<String, Boolean> getPermissionMap() {
        return permissionMap;
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


