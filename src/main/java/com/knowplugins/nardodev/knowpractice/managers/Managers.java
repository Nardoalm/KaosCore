package com.knowplugins.nardodev.knowpractice.managers;

import com.knowplugins.nardodev.knowpractice.KnowPractice;
import com.knowplugins.nardodev.knowpractice.managers.player.DisguiseManager;
import com.knowplugins.nardodev.knowpractice.managers.player.ScoreboardManager;
import com.knowplugins.nardodev.knowpractice.managers.player.VanishManager;
import com.knowplugins.nardodev.knowpractice.managers.plugin.DatabaseManager;
import com.knowplugins.nardodev.knowpractice.managers.plugin.DisguiseRepository;
import com.knowplugins.nardodev.knowpractice.managers.world.NPCManager;

import java.util.Map;
import java.util.UUID;

public class Managers {

    private final KnowPractice plugin;
    private static DatabaseManager databaseManager;
    private static DisguiseManager disguiseManager;
    private static NPCManager npcManager;
    private static ClanManager clanManager;

    public Managers(KnowPractice plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.getLogger().info("Inicializando sistemas e managers...");

        disguiseManager = new DisguiseManager();
        disguiseManager.loadNicksFile(plugin);

        npcManager = new NPCManager(plugin);
        npcManager.loadSkinCache();

        clanManager = new ClanManager(plugin);

        VanishManager.setupManager();
        ScoreboardManager.setup();

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

        plugin.getLogger().info("Sistemas carregados com sucesso!");
    }

    public static DisguiseManager getDisguiseManager() {
        return disguiseManager;
    }

    public static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public static NPCManager getNpcManager() {
         return npcManager;
    }

    public static ClanManager getClanManager() { return clanManager; }

    public static void setDatabaseManager(DatabaseManager db) {
        databaseManager = db;
    }
}