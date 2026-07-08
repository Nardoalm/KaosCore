package com.knowplugins.ysubz.kaoscore.bukkit.auth.manager;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.profile.AuthProfile;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.repository.AuthRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.security.BCrypt;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.session.AuthSession;
import com.knowplugins.ysubz.kaoscore.bukkit.util.join.JoinMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {

    private final KaosCore plugin;
    private final AuthRepository repository;
    private final Map<UUID, AuthSession> pendingPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, AuthProfile> cachedProfiles = new ConcurrentHashMap<>();
    private final Set<UUID> welcomedPlayers = ConcurrentHashMap.newKeySet();

    // Sincronia perfeita entre ProtocolLib e Bukkit Event
    private final Map<String, UUID> verifiedPremiumPlayers = new ConcurrentHashMap<>();
    private final Map<String, Boolean> mojangPremiumCache = new ConcurrentHashMap<>();

    private Location authLocation;
    private static final int TIMEOUT_SECONDS = 60;

    public AuthManager(KaosCore plugin) {
        this.plugin = plugin;
        this.repository = new AuthRepository(plugin);
        this.repository.createTables();
        this.loadAuthLocation();
        this.startTimeoutTask();
    }

    public AuthRepository getRepository() { return repository; }

    public void addVerifiedPremium(String name, UUID uuid) {
        verifiedPremiumPlayers.put(name.toLowerCase(), uuid);
    }

    public UUID consumeVerifiedPremium(String name) {
        return verifiedPremiumPlayers.remove(name.toLowerCase());
    }

    public void cacheMojangStatus(String name, boolean isPremium) {
        mojangPremiumCache.put(name.toLowerCase(), isPremium);
    }

    public Boolean consumeMojangStatus(String name) {
        return mojangPremiumCache.remove(name.toLowerCase());
    }

    public void cacheProfile(AuthProfile profile) {
        cachedProfiles.put(profile.getUuid(), profile);
    }

    public AuthProfile getCachedProfile(UUID uuid) {
        return uuid == null ? null : cachedProfiles.get(uuid);
    }

    private void loadAuthLocation() {
        if (plugin.getConfig().contains("auth.location.world")) {
            this.authLocation = new Location(
                    Bukkit.getWorld(plugin.getConfig().getString("auth.location.world")),
                    plugin.getConfig().getDouble("auth.location.x"),
                    plugin.getConfig().getDouble("auth.location.y"),
                    plugin.getConfig().getDouble("auth.location.z"),
                    (float) plugin.getConfig().getDouble("auth.location.yaw"),
                    (float) plugin.getConfig().getDouble("auth.location.pitch")
            );
        }
    }

    public void setAuthLocation(Location location) {
        this.authLocation = location;
        plugin.getConfig().set("auth.location.world", location.getWorld().getName());
        plugin.getConfig().set("auth.location.x", location.getX());
        plugin.getConfig().set("auth.location.y", location.getY());
        plugin.getConfig().set("auth.location.z", location.getZ());
        plugin.getConfig().set("auth.location.yaw", location.getYaw());
        plugin.getConfig().set("auth.location.pitch", location.getPitch());
        plugin.saveConfig();
    }

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();
        if (welcomedPlayers.contains(uuid)) {
            return;
        }

        AuthProfile profile = cachedProfiles.get(uuid);

        if (profile == null) {
            // Try lookup by name in cache first (in case of UUID mismatch)
            for (AuthProfile p : cachedProfiles.values()) {
                if (player.getName().equalsIgnoreCase(p.getName())) {
                    profile = p;
                    cachedProfiles.remove(p.getUuid());
                    cachedProfiles.put(uuid, p);
                    break;
                }
            }
        }

        if (profile == null) {
            // Try loading from database synchronously (blocking) as a fallback
            try {
                profile = repository.loadProfile(uuid, player.getName()).join();
                if (profile != null) {
                    cacheProfile(profile);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load auth profile fallback for " + player.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("[AuthDebug] handleJoin for player=" + player.getName() + ", uuid=" + uuid + ", profileLoaded=" + (profile != null) + ", isPremium=" + (profile != null && profile.isPremium()));

        if (profile == null) {
            player.kickPlayer("§cFailed to load authentication profile.");
            return;
        }

        welcomedPlayers.add(uuid);

        if (profile.isPremium()) {
            player.sendMessage("§aPremium Authentication verified. Welcome!");
            JoinMessage.sendWelcomeMessage(player);
            if (Bukkit.getServer().getPluginManager().getPlugin("HardPractice") != null) {
                try {
                    live.pvphard.practice.HardPractice.getInstance().getService(live.pvphard.practice.feature.spawn.SpawnService.class).teleportToSpawn(player);
                    live.pvphard.practice.HardPractice.getInstance().getService(live.pvphard.practice.feature.hotbar.HotbarService.class).applyHotbarItems(player);
                } catch (Exception e) {
                    player.teleport(player.getWorld().getSpawnLocation());
                }
            } else {
                player.teleport(player.getWorld().getSpawnLocation());
            }
            return;
        }

        Location previous = player.getLocation();
        pendingPlayers.put(uuid, new AuthSession(profile, previous));

        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));

        // Clear inventory so slots are empty during authentication
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.updateInventory();

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.hidePlayer(player);
            player.hidePlayer(online);
        }

        Location teleportLoc = (authLocation != null) ? authLocation : player.getWorld().getSpawnLocation();
        if (teleportLoc != null) {
            player.teleport(teleportLoc);
        }

        if (profile.isRegistered()) {
            player.sendMessage("§ePlease authenticate using: §b/login <password>");
        } else {
            player.sendMessage("§eSecure your account using: §b/register <password> <password>");
        }
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        pendingPlayers.remove(uuid);
        cachedProfiles.remove(uuid);
        welcomedPlayers.remove(uuid);
    }

    public boolean isPending(UUID uuid) {
        return pendingPlayers.containsKey(uuid);
    }

    public boolean register(Player player, String password) {
        AuthSession session = pendingPlayers.get(player.getUniqueId());
        if (session == null || session.getProfile().isRegistered()) return false;

        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        session.getProfile().setPasswordHash(hash);

        repository.saveProfile(session.getProfile());
        completeAuth(player, session);
        return true;
    }

    public boolean login(Player player, String password) {
        AuthSession session = pendingPlayers.get(player.getUniqueId());
        if (session == null || !session.getProfile().isRegistered()) return false;

        if (BCrypt.checkpw(password, session.getProfile().getPasswordHash())) {
            completeAuth(player, session);
            return true;
        }
        return false;
    }

    private void completeAuth(Player player, AuthSession session) {
        pendingPlayers.remove(player.getUniqueId());

        player.setWalkSpeed(0.2F);
        player.setFlySpeed(0.1F);
        player.removePotionEffect(PotionEffectType.BLINDNESS);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(player);
            player.showPlayer(online);
        }

        if (Bukkit.getServer().getPluginManager().getPlugin("HardPractice") != null) {
            try {
                live.pvphard.practice.HardPractice.getInstance().getService(live.pvphard.practice.feature.spawn.SpawnService.class).teleportToSpawn(player);
                live.pvphard.practice.HardPractice.getInstance().getService(live.pvphard.practice.feature.hotbar.HotbarService.class).applyHotbarItems(player);
            } catch (Exception e) {
                if (session.getPreviousLocation() != null && authLocation != null) {
                    player.teleport(session.getPreviousLocation());
                }
            }
        } else {
            if (session.getPreviousLocation() != null && authLocation != null) {
                player.teleport(session.getPreviousLocation());
            }
        }
        player.sendMessage("§aAuthentication successful.");
        JoinMessage.sendWelcomeMessage(player);
    }

    private void startTimeoutTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Map.Entry<UUID, AuthSession> entry : pendingPlayers.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null) continue;

                AuthSession session = entry.getValue();
                session.incrementSeconds();

                if (session.getSecondsPending() >= TIMEOUT_SECONDS) {
                    Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer("§cAuthentication timeout."));
                }
            }
        }, 20L, 20L);
    }
    public Location getAuthLocation() {
        return authLocation;
    }
}