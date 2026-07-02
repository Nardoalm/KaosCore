package com.knowplugins.ysubz.kaoscore.bukkit.permission;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PermissionGroup {

    private final String name;
    private int priority;
    private String prefix;
    private String suffix;
    private boolean defaultGroup;
    private final Map<String, Boolean> permissions;
    private final Set<String> inherits;

    public PermissionGroup(String name) {
        this.name = normalize(name);
        this.permissions = new LinkedHashMap<>();
        this.inherits = new LinkedHashSet<>();
        this.prefix = "";
        this.suffix = "";
        this.priority = 0;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getPrefix() {
        return prefix == null ? "" : prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix == null ? "" : prefix;
    }

    public String getSuffix() {
        return suffix == null ? "" : suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix == null ? "" : suffix;
    }

    public boolean isDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(boolean defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public Map<String, Boolean> getPermissions() {
        return Collections.unmodifiableMap(permissions);
    }

    public void setPermission(String permission, boolean value) {
        String key = normalize(permission);
        if (key.isEmpty()) {
            return;
        }
        permissions.put(key, value);
    }

    public void removePermission(String permission) {
        String key = normalize(permission);
        if (key.isEmpty()) {
            return;
        }
        permissions.remove(key);
    }

    public Set<String> getInherits() {
        return Collections.unmodifiableSet(inherits);
    }

    public void addInherit(String groupName) {
        String key = normalize(groupName);
        if (key.isEmpty() || key.equals(this.name)) {
            return;
        }
        inherits.add(key);
    }

    public void removeInherit(String groupName) {
        String key = normalize(groupName);
        if (key.isEmpty()) {
            return;
        }
        inherits.remove(key);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

