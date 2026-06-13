package com.knowplugins.ysubz.kaoscore.bungee;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class PermissionUserData {

    private final UUID uuid;
    private final String lastKnownName;
    private final List<GroupGrant> grants;
    private final List<UserPermissionNode> permissions;
    private final long loadedAt;

    public PermissionUserData(UUID uuid, String lastKnownName, List<GroupGrant> grants, List<UserPermissionNode> permissions, long loadedAt) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName == null ? "" : lastKnownName;
        this.grants = grants == null ? Collections.<GroupGrant>emptyList() : Collections.unmodifiableList(grants);
        this.permissions = permissions == null ? Collections.<UserPermissionNode>emptyList() : Collections.unmodifiableList(permissions);
        this.loadedAt = loadedAt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public List<GroupGrant> getGrants() {
        return grants;
    }

    public List<UserPermissionNode> getPermissions() {
        return permissions;
    }

    public long getLoadedAt() {
        return loadedAt;
    }
}


