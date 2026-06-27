package com.knowplugins.ysubz.kaoscore.bukkit.auth.manager;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.profile.AuthProfile;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.repository.AuthRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.security.BCrypt;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.session.AuthSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {

    private final KaosCore plugin;
    private final AuthRepository repository;
    private final Map<UUID, AuthSession> pendingPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, AuthProfile> cachedProfiles = new ConcurrentHashMap<>();
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

    public void cacheProfile(AuthProfile profile) {
        cachedProfiles.put(profile.getUuid(), profile);
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
        AuthProfile profile = cachedProfiles.get(player.getUniqueId());

        if (profile == null) {
            player.kickPlayer("§cProfile failed to load.");
            return;
        }

        // Auto-Login inquebrável: Só executa se o ProtocolLib comprovou a assinatura RSA da Mojang.
        if (profile.isPremium() && player.getUniqueId().version() == 4) {
            player.sendMessage("§a§lKaosCore §8» §aPremium Authentication verified. Welcome!");
            return;
        }

        Location previous = player.getLocation();
        pendingPlayers.put(player.getUniqueId(), new AuthSession(profile, previous));

        player.setWalkSpeed(0F);
        player.setFlySpeed(0F);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.hidePlayer(player);
            player.hidePlayer(online);
        }

        if (authLocation != null) player.teleport(authLocation);

        if (profile.isRegistered()) {
            player.sendMessage("§eAuthenticate using: §b/login <password>");
        } else {
            player.sendMessage("§eRegister your account using: §b/register <password> <password>");
        }
    }

    public void handleQuit(Player player) {
        pendingPlayers.remove(player.getUniqueId());
        cachedProfiles.remove(player.getUniqueId());
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

        if (session.getPreviousLocation() != null && authLocation != null) {
            player.teleport(session.getPreviousLocation());
        }
        player.sendMessage("§a§lKaosCore §8» §aAuthentication successful.");
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
}