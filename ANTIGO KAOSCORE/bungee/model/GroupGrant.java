package com.knowplugins.ysubz.kaoscore.bungee;

public final class GroupGrant {

    private final long id;
    private final String groupName;
    private final long createdAt;
    private final long expiresAt;

    public GroupGrant(long id, String groupName, long createdAt, long expiresAt) {
        this.id = id;
        this.groupName = groupName == null ? "" : groupName;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public long getId() {
        return id;
    }

    public String getGroupName() {
        return groupName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(long now) {
        return expiresAt > 0L && expiresAt <= now;
    }
}


