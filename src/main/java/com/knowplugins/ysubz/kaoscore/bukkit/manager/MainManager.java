package com.knowplugins.ysubz.kaoscore.bukkit.manager;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.manager.AuthManager;
import com.knowplugins.ysubz.kaoscore.bukkit.clan.manager.ClanManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.friend.manager.FriendManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.*;
import com.knowplugins.ysubz.kaoscore.bukkit.permission.manager.PermissionManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.plugin.AutoBroadcastManager;
import com.knowplugins.ysubz.kaoscore.bukkit.database.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.repository.DisguiseRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.plugin.repository.PlayerAddressRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.plugin.repository.PunishmentRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.world.ClimateManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.world.NPCManager;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.world.SpawnLimitsManager;
import com.knowplugins.ysubz.kaoscore.bukkit.sit.SitManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.Logger;
import com.knowplugins.ysubz.kaoscore.bukkit.whitelist.WhitelistManager;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

public class MainManager {

    private final KaosCore plugin;
    @Setter
    @Getter
    private static DatabaseManager databaseManager;
    @Getter
    private static WhitelistManager whitelistManager;
    @Getter
    private static DisguiseManager disguiseManager;
    @Getter
    private static PunishmentManager punishmentManager;
    @Getter
    private static SitManager sitManager;
    private static JoinMessageManager joinMessageManager;
    @Getter
    private static PlayerAddressRepository playerAddressRepository;
    @Getter
    private static NPCManager npcManager;
    @Getter
    private static ClanManager clanManager;
    @Getter
    private static FriendManager friendManager;
    private static ClimateManager climateManager;
    @Getter
    private static AutoBroadcastManager autoBroadcastManager;
    @Getter
    private static SpawnLimitsManager spawnLimitsManager;
    @Getter
    private static PermissionManager permissionManager;
    @Getter
    private static AuthManager authManager;

    public MainManager(KaosCore plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        Logger.info("Inicializando sistemas e managers...");

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
        joinMessageManager = new JoinMessageManager(plugin);
        climateManager = new ClimateManager();
        if (plugin.getConfig().getBoolean("features.scoreboard.enabled", true)) {
            ScoreboardManager.setup();
        }
        VanishManager.setupManager();

        npcManager = new NPCManager(plugin, databaseManager);


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

        Logger.info("Systems loaded successfully.");
    }

    public void reloadAll() {
        Logger.info("Recarregando managers...");

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

        if (joinMessageManager == null) {
            joinMessageManager = new JoinMessageManager(plugin);
        } else {
            joinMessageManager.reload();
        }

        climateManager = new ClimateManager();

        ScoreboardManager.reload();
        VanishManager.setupManager();

        if (npcManager == null) {
            npcManager = new NPCManager(plugin, databaseManager);
        } else {
            npcManager.reload();
        }
        npcManager.refreshAllViewers();

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

        Logger.info("Managers reloaded successfully.");
    }

    public static ClimateManager getClimateManager() {
        return climateManager;
    }

    public static JoinMessageManager getJoinMessageManager() {
        return joinMessageManager;
    }

}
