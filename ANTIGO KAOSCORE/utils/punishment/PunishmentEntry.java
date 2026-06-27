package com.knowplugins.ysubz.kaoscore.bukkit.utils.punishment;

import java.util.UUID;

public class PunishmentEntry {

    private final long id;
    private final UUID targetUuid;
    private final String targetName;
    private final PunishmentType type;
    private final String reason;
    private final long createdAt;
    private final long expiresAt;
    private final UUID punisherUuid;
    private final String punisherName;
    private final String source;
    private boolean active;
    private long removedAt;
    private UUID removedByUuid;
    private String removedByName;
    private String removedReason;

    public PunishmentEntry(long id, UUID targetUuid, String targetName, PunishmentType type, String reason, long createdAt, long expiresAt, UUID punisherUuid, String punisherName, String source, boolean active, long removedAt, UUID removedByUuid, String removedByName, String removedReason) {
        this.id = id;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.type = type;
        this.reason = reason;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.punisherUuid = punisherUuid;
        this.punisherName = punisherName;
        this.source = source;
        this.active = active;
        this.removedAt = removedAt;
        this.removedByUuid = removedByUuid;
        this.removedByName = removedByName;
        this.removedReason = removedReason;
    }

    public long getId() {
        return id;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public PunishmentType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public UUID getPunisherUuid() {
        return punisherUuid;
    }

    public String getPunisherName() {
        return punisherName;
    }

    public String getSource() {
        return source;
    }

    public boolean isActive() {
        return active;
    }

    public long getRemovedAt() {
        return removedAt;
    }

    public UUID getRemovedByUuid() {
        return removedByUuid;
    }

    public String getRemovedByName() {
        return removedByName;
    }

    public String getRemovedReason() {
        return removedReason;
    }

    public boolean isPermanent() {
        return expiresAt <= 0L;
    }

    public boolean isExpired() {
        return !isPermanent() && expiresAt <= System.currentTimeMillis();
    }

    public void deactivate(UUID staffUuid, String staffName, String reason) {
        this.active = false;
        this.removedAt = System.currentTimeMillis();
        this.removedByUuid = staffUuid;
        this.removedByName = staffName;
        this.removedReason = reason;
    }
}
