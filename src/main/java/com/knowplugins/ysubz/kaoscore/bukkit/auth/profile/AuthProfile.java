package com.knowplugins.ysubz.kaoscore.bukkit.auth.profile;

import java.util.UUID;

public class AuthProfile {
    private final UUID uuid;
    private final String name;
    private String passwordHash;
    private boolean premium;

    public AuthProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public boolean isRegistered() {
        return passwordHash != null && !passwordHash.isEmpty();
    }
}