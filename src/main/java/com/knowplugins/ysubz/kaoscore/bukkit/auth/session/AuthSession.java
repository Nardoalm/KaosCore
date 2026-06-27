package com.knowplugins.ysubz.kaoscore.bukkit.auth.session;

import com.knowplugins.ysubz.kaoscore.bukkit.auth.profile.AuthProfile;
import org.bukkit.Location;

public class AuthSession {
    private final AuthProfile profile;
    private final Location previousLocation;
    private int secondsPending;

    public AuthSession(AuthProfile profile, Location previousLocation) {
        this.profile = profile;
        this.previousLocation = previousLocation;
        this.secondsPending = 0;
    }

    public AuthProfile getProfile() {
        return profile;
    }

    public Location getPreviousLocation() {
        return previousLocation;
    }

    public int getSecondsPending() {
        return secondsPending;
    }

    public void incrementSeconds() {
        this.secondsPending++;
    }
}