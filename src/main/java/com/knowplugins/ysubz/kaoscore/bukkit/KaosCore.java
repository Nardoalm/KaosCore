package com.knowplugins.ysubz.kaoscore.bukkit;

import com.knowplugins.ysubz.kaoscore.bukkit.api.KaosCoreAPI;
import com.knowplugins.ysubz.kaoscore.bukkit.api.KaosCoreAPIImpl;
import com.knowplugins.ysubz.kaoscore.bukkit.account.AccountController;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.manager.AuthManager;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.listener.AuthPreLoginListener;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.listener.AuthPacketListener;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.list.vip.SkinCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.listeners.Listeners;
import com.knowplugins.ysubz.kaoscore.bukkit.listeners.proxy.ProxySyncListener;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.ClanManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.DiscordWebhookManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.MedalManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.TagManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.SitManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.TagTeamManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.PunishmentMessageManager;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.placeholder.KaosCoreExpansion;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.skin.SkinPoolManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class KaosCore extends JavaPlugin {

    private static KaosCore instance;
    private AccountController accountController;
    private Managers managers;
    private TagManager tagManager;
    private MedalManager medalManager;
    private TagTeamManager tagTeamManager;
    private AuthManager authManager;
    private AuthPacketListener authPacketListener;
    private AuthPreLoginListener authPreLoginListener;
    private DiscordWebhookManager webhookManager;
    private SkinPoolManager skinPoolManager;
    private PunishmentMessageManager punishmentMessageManager;
    private KaosCoreAPI api;
    private ProxySyncListener proxySyncListener;

    public static KaosCore getInstance() {
        return instance;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }
    public AuthPacketListener getAuthPacketListener() {
        return authPacketListener;
    }
    public AuthPreLoginListener getAuthPreLoginListener() {
        return authPreLoginListener;
    }
    public TagManager getTagManager() { return tagManager; }
    public MedalManager getMedalManager() { return medalManager; }
    public ClanManager getClanManager() { return Managers.getClanManager(); }
    public TagTeamManager getTagTeamManager() { return tagTeamManager; }
    public AccountController getAccountController() { return accountController; }
    public DiscordWebhookManager getWebhookManager() { return webhookManager; }
    public SkinPoolManager getSkinPoolManager() { return skinPoolManager; }
    public PunishmentMessageManager getPunishmentMessageManager() { return punishmentMessageManager; }
    public KaosCoreAPI getAPI() { return api; }

    @Override
    public void onEnable() {
        instance = this;
        accountController = new AccountController(this);
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadSystem();
    }

    private void loadSystem() {
        saveResource("tags.yml", false);
        saveResource("medals.yml", false);
        saveResource("webhooks.yml", false);
        saveResource("skins.yml", false);
        saveResource("whitelist.yml", false);
        saveResource("welcome.yml", false);
        saveResource("punish.yml", false);
        saveResource("permissions.yml", false);
        saveResource("npcs.yml", false);
        getDataFolder().mkdirs();

        webhookManager = new DiscordWebhookManager(this);
        punishmentMessageManager = new PunishmentMessageManager(this);

        DatabaseManager db = new DatabaseManager(this);
        if (!db.connect() || !db.isConnected()) {
            getLogger().severe("§c[KaosCore] Nao foi possivel conectar ao banco de dados. Desativando o plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Managers.setDatabaseManager(db);

        this.authManager = new AuthManager(this);
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            this.authPacketListener = new AuthPacketListener(this);
        }
        this.authPreLoginListener = new AuthPreLoginListener(this);

        skinPoolManager = new SkinPoolManager(this, db);
        skinPoolManager.preloadPoolAsync();
        managers = new Managers(this);
        managers.loadAll();

        tagManager = new TagManager(this);
        medalManager = new MedalManager(this);
        boolean nametagEnabled = getConfig().getBoolean("features.nametag.enabled", true);
        if (nametagEnabled) {
            tagTeamManager = new TagTeamManager(this, tagManager, medalManager);
            tagTeamManager.start();
        } else {
            tagTeamManager = null;
        }

        api = new KaosCoreAPIImpl(this);
        Bukkit.getServicesManager().register(KaosCoreAPI.class, api, this, ServicePriority.Normal);

        Listeners.setupListeners(this);
        Commands.setupCommands(this);
        proxySyncListener = new ProxySyncListener(this);
        proxySyncListener.register();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new KaosCoreExpansion(this).register();
        }

        if (Managers.getDisguiseManager() != null) {
            Managers.getDisguiseManager().loadNicksFile(this);
        }

        getLogger().info("All systems have been loaded.");
    }

    public synchronized void reloadSystems() {
        reloadConfig();

        saveResource("tags.yml", false);
        saveResource("medals.yml", false);
        saveResource("webhooks.yml", false);
        saveResource("skins.yml", false);
        saveResource("whitelist.yml", false);
        saveResource("welcome.yml", false);
        saveResource("punish.yml", false);
        saveResource("permissions.yml", false);
        saveResource("npcs.yml", false);

        if (webhookManager == null) {
            webhookManager = new DiscordWebhookManager(this);
        } else {
            webhookManager.reload();
        }

        if (punishmentMessageManager == null) {
            punishmentMessageManager = new PunishmentMessageManager(this);
        } else {
            punishmentMessageManager.reload();
        }

        if (managers == null) {
            managers = new Managers(this);
            managers.loadAll();
        } else {
            managers.reloadAll();
        }

        if (tagManager == null) {
            tagManager = new TagManager(this);
        } else {
            tagManager.loadTags();
        }

        if (medalManager == null) {
            medalManager = new MedalManager(this);
        } else {
            medalManager.loadMedals();
        }

        if (tagTeamManager != null) {
            tagTeamManager.stop();
            tagTeamManager = null;
        }

        boolean nametagEnabled = getConfig().getBoolean("features.nametag.enabled", true);
        if (nametagEnabled) {
            tagTeamManager = new TagTeamManager(this, tagManager, medalManager);
            tagTeamManager.start();
            tagManager.refreshAll();
        }

        if (Managers.getDisguiseManager() != null) {
            Managers.getDisguiseManager().loadNicksFile(this);
        }

        getLogger().info("Configuration reloaded successfully.");
    }

    @Override
    public void onDisable() {
        try {
            DisguiseManager disguiseManager = Managers.getDisguiseManager();
            if (disguiseManager != null) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (DisguiseManager.isDisguised(online)) {
                        disguiseManager.removeDisguise(online);
                        SkinCommand.resetSkin(online);
                    }
                }
            }
        } catch (Exception ignored) {}

        try {
            if (getClanManager() != null) getClanManager().saveClans();
            if (tagTeamManager != null) tagTeamManager.stop();

            try {
                if (Managers.getAutoBroadcastManager() != null) {
                    Managers.getAutoBroadcastManager().stop();
                }
                if (Managers.getSpawnLimitsManager() != null) {
                    Managers.getSpawnLimitsManager().stop();
                }
            } catch (Exception ignored) {
            }

            SitManager sitManager = Managers.getSitManager();
            if (sitManager != null) sitManager.cleanupAll();

            Bukkit.getServicesManager().unregisterAll(this);
            if (proxySyncListener != null) {
                proxySyncListener.unregister();
            }

            DatabaseManager db = Managers.getDatabaseManager();
            if (db != null) db.disconnect();
        } catch (Exception ignored) {}

        if (accountController != null) {
            accountController.clear();
        }

        if (Managers.getSitManager() != null) {
            Managers.getSitManager().cleanupAll();
        }

        Bukkit.getConsoleSender().sendMessage("§c[KaosCore] §cThe plugin has been disabled.");
    }
}