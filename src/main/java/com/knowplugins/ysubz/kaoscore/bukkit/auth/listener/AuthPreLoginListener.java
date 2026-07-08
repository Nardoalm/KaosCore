package com.knowplugins.ysubz.kaoscore.bukkit.auth.listener;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.profile.AuthProfile;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.util.MojangAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class AuthPreLoginListener implements Listener {

    private final KaosCore plugin;

    public AuthPreLoginListener(KaosCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void processPreLogin(AsyncPlayerPreLoginEvent event) {
        String name = event.getName();

        UUID premiumUUID = null;
        Boolean cachedMojangStatus = null;

        if (plugin.getAuthManager() != null) {
            premiumUUID = plugin.getAuthManager().consumeVerifiedPremium(name);
            cachedMojangStatus = plugin.getAuthManager().consumeMojangStatus(name);
        }

        UUID finalUUID = event.getUniqueId();
        if (finalUUID == null) {
            if (premiumUUID != null) {
                finalUUID = premiumUUID;
            } else {
                finalUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            }
        }

        try {
            if (plugin.getAuthManager() == null || plugin.getAuthManager().getRepository() == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§cServer is still starting up...");
                return;
            }

            AuthProfile profile = plugin.getAuthManager().getRepository().loadProfile(finalUUID, name).join();

            if (profile != null) {
                if (premiumUUID != null) {
                    profile.setPremium(true);
                    plugin.getAuthManager().getRepository().saveProfile(profile);
                } else if (!profile.isRegistered()) {
                    if (cachedMojangStatus != null && cachedMojangStatus) {
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                                "§c§lPROTECTED NICKNAME\n\n§cThis username belongs to a Premium account.\n§cPlease choose another name or use a premium launcher.");
                        return;
                    }
                }
                plugin.getAuthManager().cacheProfile(profile);
            }

        } catch (Exception e) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§cDatabase communication failed. Please try again.");
            e.printStackTrace();
        }
    }
}