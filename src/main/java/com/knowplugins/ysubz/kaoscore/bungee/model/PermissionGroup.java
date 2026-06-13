package com.knowplugins.ysubz.kaoscore.bungee;


import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
        this.name = Texts.normalize(name);
        this.permissions = new LinkedHashMap<>();
        this.inherits = new LinkedHashSet<>();
        this.prefix = "";
        this.suffix = "";
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
        String key = Texts.normalize(permission);
        if (!key.isEmpty()) {
            permissions.put(key, value);
        }
    }

    public Set<String> getInherits() {
        return Collections.unmodifiableSet(inherits);
    }

    public void addInherit(String groupName) {
        String key = Texts.normalize(groupName);
        if (!key.isEmpty() && !key.equals(name)) {
            inherits.add(key);
        }
    }
}


