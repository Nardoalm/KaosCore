package com.knowplugins.ysubz.kaoscore;

import com.knowplugins.ysubz.kaoscore.api.KaosCoreAPI;
import com.knowplugins.ysubz.kaoscore.api.KaosCoreAPIImpl;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.cmd.commands.TagCommand;
import com.knowplugins.ysubz.kaoscore.listeners.Listeners;
import com.knowplugins.ysubz.kaoscore.managers.player.ClanManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DiscordWebhookManager;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.MedalManager;
import com.knowplugins.ysubz.kaoscore.managers.player.TagManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.managers.player.SitManager;
import com.knowplugins.ysubz.kaoscore.managers.player.TagTeamManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.LicenseManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.PunishmentMessageManager;
import com.knowplugins.ysubz.kaoscore.utils.placeholder.KaosCoreExpansion;
import com.knowplugins.ysubz.kaoscore.utils.skin.SkinPoolManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class KaosCore extends JavaPlugin {

    private static KaosCore instance;
    private Managers managers;
    private TagManager tagManager;
    private MedalManager medalManager;
    private TagTeamManager tagTeamManager;
    private DiscordWebhookManager webhookManager;
    private SkinPoolManager skinPoolManager;
    private PunishmentMessageManager punishmentMessageManager;
    private KaosCoreAPI api;

    public static KaosCore getInstance() {
        return instance;
    }

    public TagManager getTagManager() { return tagManager; }
    public MedalManager getMedalManager() { return medalManager; }
    public ClanManager getClanManager() { return Managers.getClanManager(); }
    public TagTeamManager getTagTeamManager() { return tagTeamManager; }
    public DiscordWebhookManager getWebhookManager() { return webhookManager; }
    public SkinPoolManager getSkinPoolManager() { return skinPoolManager; }
    public PunishmentMessageManager getPunishmentMessageManager() { return punishmentMessageManager; }
    public KaosCoreAPI getAPI() { return api; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        String key = getConfig().getString("license");

        Bukkit.getConsoleSender().sendMessage("§e[KaosCore] Verificando licença...");

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            LicenseManager license = new LicenseManager(key);

            if (license.isValid()) {
                Bukkit.getScheduler().runTask(this, () -> {
                    Bukkit.getConsoleSender().sendMessage("§a[KaosCore] Licença validada com sucesso!");
                    carregarSistemas();
                });
            } else {
                Bukkit.getScheduler().runTask(this, () -> {
                    getLogger().severe("§c---------------------------------------");
                    getLogger().severe("§c     LICENCA INVALIDA OU EXPIRADA       ");
                    getLogger().severe("§c   Adquira com o ysubz para usar este plugin!   ");
                    getLogger().severe("§c---------------------------------------");
                    saveDefaultConfig();
                    Bukkit.getPluginManager().disablePlugin(this);
                });
            }
        });
    }

    private void carregarSistemas() {
        saveResource("tags.yml", false);
        saveResource("medals.yml", false);
        saveResource("webhooks.yml", false);
        saveResource("skins.yml", false);
        saveResource("whitelist.yml", false);
        saveResource("welcomeMessages.yml", false);
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

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new KaosCoreExpansion(this).register();
        }

        if (getCommand("tag") != null) {
            getCommand("tag").setExecutor(new TagCommand(this));
        }

        if (Managers.getDisguiseManager() != null) {
            Managers.getDisguiseManager().loadNicksFile(this);
        }

        getLogger().info("Todos os sistemas foram carregados!");
    }

    @Override
    public void onDisable() {
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

            DatabaseManager db = Managers.getDatabaseManager();
            if (db != null) db.disconnect();
        } catch (Exception ignored) {}

        if (Managers.getSitManager() != null) {
            Managers.getSitManager().cleanupAll();
        }

        Bukkit.getConsoleSender().sendMessage("§c[KaosCore] §c§lTCHAU :( -> §cO plugin foi desativado.");
    }
}
