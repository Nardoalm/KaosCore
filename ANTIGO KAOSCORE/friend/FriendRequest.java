package com.knowplugins.ysubz.kaoscore.bukkit.friend;

import java.util.UUID;

public class FriendRequest {

    public static final long EXPIRATION_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    private final UUID requesterId;
    private final String requesterName;
    private final UUID targetId;
    private final long createdAt;

    public FriendRequest(UUID requesterId, String requesterName, UUID targetId) {
        this(requesterId, requesterName, targetId, System.currentTimeMillis());
    }

    public FriendRequest(UUID requesterId, String requesterName, UUID targetId, long createdAt) {
        this.requesterId = requesterId;
        this.requesterName = requesterName == null ? "Desconhecido" : requesterName;
        this.targetId = targetId;
        this.createdAt = createdAt;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > EXPIRATION_MILLIS;
    }
}
