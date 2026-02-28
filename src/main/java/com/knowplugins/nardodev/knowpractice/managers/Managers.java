package com.knowplugins.nardodev.knowpractice.managers;

import com.knowplugins.nardodev.knowpractice.KnowPractice;
import com.knowplugins.nardodev.knowpractice.managers.player.DisguiseManager;
import com.knowplugins.nardodev.knowpractice.managers.player.ScoreboardManager;
import com.knowplugins.nardodev.knowpractice.managers.player.VanishManager;
import com.knowplugins.nardodev.knowpractice.managers.plugin.DatabaseManager;
import com.knowplugins.nardodev.knowpractice.managers.world.NPCManager;

public class Managers {

    private final KnowPractice plugin;
    private static DatabaseManager databaseManager;
    private static DisguiseManager disguiseManager;
    private static NPCManager npcManager;

    public Managers(KnowPractice plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.getLogger().info("Inicializando sistemas e managers...");

        disguiseManager = new DisguiseManager();
        disguiseManager.loadNicksFile(plugin);

        databaseManager = new DatabaseManager(plugin);

        npcManager = new NPCManager(plugin);
        npcManager.loadSkinCache();

        VanishManager.setupManager();
        ScoreboardManager.setup();

        plugin.getLogger().info("Sistemas carregados com sucesso!");
    }

    public DisguiseManager getDisguiseManager() {
        return this.disguiseManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public static NPCManager getNpcManager() {
         return npcManager;
    }
}