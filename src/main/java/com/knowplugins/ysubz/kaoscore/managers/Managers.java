package com.knowplugins.ysubz.kaoscore.managers;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.player.*;
import com.knowplugins.ysubz.kaoscore.managers.permission.PermissionManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.AutoBroadcastManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DisguiseRepository;
import com.knowplugins.ysubz.kaoscore.managers.plugin.PlayerAddressRepository;
import com.knowplugins.ysubz.kaoscore.managers.plugin.PunishmentRepository;
import com.knowplugins.ysubz.kaoscore.managers.world.ClimateManager;
import com.knowplugins.ysubz.kaoscore.managers.world.NPCManager;
import com.knowplugins.ysubz.kaoscore.managers.world.SpawnLimitsManager;
import com.knowplugins.ysubz.kaoscore.managers.world.WhitelistManager;

import java.util.Map;
import java.util.UUID;

public class Managers {

    private final KaosCore plugin;
    private static DatabaseManager databaseManager;
    private static WhitelistManager whitelistManager;
    private static DisguiseManager disguiseManager;
    private static PunishmentManager punishmentManager;
    private static SitManager sitManager;
    private static WelcomeMessageManager welcomeMessageManager;
    private static PlayerAddressRepository playerAddressRepository;
    private static NPCManager npcManager;
    private static ClanManager clanManager;
    private static FriendManager friendManager;
    private static ClimateManager climateManager;
    private static AutoBroadcastManager autoBroadcastManager;
    private static SpawnLimitsManager spawnLimitsManager;
    private static PermissionManager permissionManager;

    public Managers(KaosCore plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.getLogger().info("Inicializando sistemas e managers...");

        whitelistManager = new WhitelistManager(plugin);

        disguiseManager = new DisguiseManager();
        disguiseManager.loadNicksFile(plugin);

        if (databaseManager != null && databaseManager.isConnected()) {
            permissionManager = new PermissionManager(plugin, databaseManager);
            PunishmentRepository punishmentRepository = new PunishmentRepository(plugin, databaseManager);
            punishmentManager = new PunishmentManager(punishmentRepository, plugin == null ? null : plugin.getWebhookManager());
            punishmentManager.loadActive();
            playerAddressRepository = new PlayerAddressRepository(plugin, databaseManager);
        }

        clanManager = new ClanManager(plugin);
        clanManager.loadClans();
        friendManager = new FriendManager(plugin);
        friendManager.loadFriends();
        sitManager = new SitManager(plugin);
        welcomeMessageManager = new WelcomeMessageManager(plugin);
        climateManager = new ClimateManager();
        if (plugin.getConfig().getBoolean("features.scoreboard.enabled", true)) {
            ScoreboardManager.setup();
        }
        VanishManager.setupManager();

        if (plugin.getConfig().getBoolean("npc-spawn.enabled", false)) {
            npcManager = new NPCManager(plugin, databaseManager);
        } else {
            npcManager = null;
        }


        if (databaseManager != null && databaseManager.isConnected()) {
            DisguiseRepository repo = new DisguiseRepository(plugin, databaseManager);
            repo.loadAllActive().thenAccept(map -> {
                if (map != null && !map.isEmpty()) {
                    for (Map.Entry<UUID, String> e : map.entrySet()) {
                        disguiseManager.addInDisguise(e.getKey(), e.getValue());
                    }
                }
            });
        }

        autoBroadcastManager = new AutoBroadcastManager(plugin);
        // spawnLimitsManager = new SpawnLimitsManager(plugin);

        plugin.getLogger().info("Sistemas carregados com sucesso!");
    }

    public void reloadAll() {
        plugin.getLogger().info("Recarregando managers...");

        if (whitelistManager == null) {
            whitelistManager = new WhitelistManager(plugin);
        } else {
            whitelistManager.load();
        }

        if (disguiseManager == null) {
            disguiseManager = new DisguiseManager();
        }
        disguiseManager.loadNicksFile(plugin);

        if (databaseManager != null && databaseManager.isConnected()) {
            if (permissionManager == null) {
                permissionManager = new PermissionManager(plugin, databaseManager);
            } else {
                permissionManager.reload();
            }
        }

        if (clanManager == null) {
            clanManager = new ClanManager(plugin);
        }
        clanManager.reload();

        if (friendManager == null) {
            friendManager = new FriendManager(plugin);
        }
        friendManager.loadFriends();

        if (welcomeMessageManager == null) {
            welcomeMessageManager = new WelcomeMessageManager(plugin);
        } else {
            welcomeMessageManager.reload();
        }

        climateManager = new ClimateManager();

        ScoreboardManager.reload();
        VanishManager.setupManager();

        if (npcManager != null) {
            npcManager.shutdown();
            npcManager = null;
        }
        if (plugin.getConfig().getBoolean("npc-spawn.enabled", false)) {
            npcManager = new NPCManager(plugin, databaseManager);
            npcManager.refreshAllViewers();
        }

        if (autoBroadcastManager == null) {
            autoBroadcastManager = new AutoBroadcastManager(plugin);
        } else {
            autoBroadcastManager.reload();
        }

        if (spawnLimitsManager == null) {
            spawnLimitsManager = new SpawnLimitsManager(plugin);
        } else {
            spawnLimitsManager.reload();
        }

        plugin.getLogger().info("Managers recarregados com sucesso!");
    }

    public static DisguiseManager getDisguiseManager() {
        return disguiseManager;
    }

    public static WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    public static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public static PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public static SitManager getSitManager() {
        return sitManager;
    }

    public static WelcomeMessageManager getWelcomeMessageManager() {
        return welcomeMessageManager;
    }

    public static PlayerAddressRepository getPlayerAddressRepository() {
        return playerAddressRepository;
    }

    public static NPCManager getNpcManager() {
        return npcManager;
    }

    public static ClanManager getClanManager() {
        return clanManager;
    }

    public static FriendManager getFriendManager() {
        return friendManager;
    }

    public static void setDatabaseManager(DatabaseManager db) {
        databaseManager = db;
    }

    public static AutoBroadcastManager getAutoBroadcastManager() {
        return autoBroadcastManager;
    }

    public static SpawnLimitsManager getSpawnLimitsManager() {
        return spawnLimitsManager;
    }

    public static PermissionManager getPermissionManager() {
        return permissionManager;
    }
}
