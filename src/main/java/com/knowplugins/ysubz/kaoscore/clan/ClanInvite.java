package com.knowplugins.ysubz.kaoscore.clan;

import java.util.UUID;

public class ClanInvite {

    private String clanName;
    private UUID inviter;
    private UUID target;
    private long timestamp;

    public ClanInvite(String clanName, UUID inviter, UUID target) {
        this.clanName = clanName;
        this.inviter = inviter;
        this.target = target;
        this.timestamp = System.currentTimeMillis();
    }

    public String getClanName() { return clanName; }
    public UUID getInviter() { return inviter; }
    public UUID getTarget() { return target; }
    public long getTimestamp() { return timestamp; }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > 60000; // 1 minute
    }
}
