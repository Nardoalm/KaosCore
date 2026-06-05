package com.knowplugins.ysubz.kaoscore.bungee.model;

import java.util.UUID;

public final class ClanInvite {

    public static final long EXPIRATION_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    private final String clanName;
    private final UUID inviter;
    private final UUID target;
    private final long timestamp;

    public ClanInvite(String clanName, UUID inviter, UUID target) {
        this(clanName, inviter, target, System.currentTimeMillis());
    }

    public ClanInvite(String clanName, UUID inviter, UUID target, long timestamp) {
        this.clanName = clanName;
        this.inviter = inviter;
        this.target = target;
        this.timestamp = timestamp;
    }

    public String getClanName() {
        return clanName;
    }

    public UUID getInviter() {
        return inviter;
    }

    public UUID getTarget() {
        return target;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > EXPIRATION_MILLIS;
    }
}
