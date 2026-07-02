package com.knowplugins.ysubz.kaoscore.bukkit.permission;

public final class UserPermissionNode {

    private final String permission;
    private final boolean value;
    private final long expiresAt;

    public UserPermissionNode(String permission, boolean value, long expiresAt) {
        this.permission = permission == null ? "" : permission;
        this.value = value;
        this.expiresAt = expiresAt;
    }

    public String getPermission() {
        return permission;
    }

    public boolean getValue() {
        return value;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(long now) {
        return expiresAt > 0L && expiresAt <= now;
    }
}

