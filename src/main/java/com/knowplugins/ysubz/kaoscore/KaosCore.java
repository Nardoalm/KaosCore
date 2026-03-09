package com.knowplugins.ysubz.kaoscore;

import com.knowplugins.ysubz.kaoscore.api.KaosCoreAPI;
import com.knowplugins.ysubz.kaoscore.api.KaosCoreAPIImpl;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.cmd.commands.TagCommand;
import com.knowplugins.ysubz.kaoscore.listeners.Listeners;
import com.knowplugins.ysubz.kaoscore.managers.ClanManager;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.MedalManager;
import com.knowplugins.ysubz.kaoscore.managers.TagManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.managers.player.SitManager;
import com.knowplugins.ysubz.kaoscore.managers.player.TagTeamManager;
import com.knowplugins.ysubz.kaoscore.utils.placeholder.KaosCoreExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class KaosCore extends JavaPlugin {

    private static KaosCore instance;
    private Managers managers;
    private TagManager tagManager;
    private MedalManager medalManager;
    private TagTeamManager tagTeamManager;
    private KaosCoreAPI api;

    public static KaosCore getInstance() {
        return instance;
    }

    public TagManager getTagManager() {
        return tagManager;
    }

    public MedalManager getMedalManager() {
        return medalManager;
    }

    public ClanManager getClanManager() {
        return Managers.getClanManager();
    }

    public TagTeamManager getTagTeamManager() {
        return tagTeamManager;
    }

    public KaosCoreAPI getAPI() {
        return api;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("tags.yml", false);
        saveResource("medals.yml", false);
        instance = this;

        getDataFolder().mkdirs();

        DatabaseManager db = new DatabaseManager(this);
        if (!db.connect() || !db.isConnected()) {
            getLogger().severe("Nao foi possivel conectar ao banco de dados. Desativando o plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Managers.setDatabaseManager(db);
        managers = new Managers(this);
        managers.loadAll();

        tagManager = new TagManager(this);
        medalManager = new MedalManager(this);
        tagTeamManager = new TagTeamManager(this, tagManager, medalManager);
        tagTeamManager.start();
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
    }

    @Override
    public void onDisable() {
        try {
            ClanManager clanManager = getClanManager();
            if (clanManager != null) {
                clanManager.saveClans();
            }
            if (tagTeamManager != null) {
                tagTeamManager.stop();
            }
            SitManager sitManager = Managers.getSitManager();
            if (sitManager != null) {
                sitManager.cleanupAll();
            }
            Bukkit.getServicesManager().unregisterAll(this);
            DatabaseManager db = Managers.getDatabaseManager();
            if (db != null) {
                db.disconnect();
            }
        } catch (Exception ignored) {
        }

        Bukkit.getConsoleSender().sendMessage("§c§lTCHAU :( -> §cO plugin foi desativado.");
    }
}
