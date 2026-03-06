package com.knowplugins.ysubz.kaoscore;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.listeners.Listeners;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class KaosCore extends JavaPlugin {
   private static KaosCore instance;
   private Managers managers;

   public static KaosCore getInstance() {
      return instance;
   }

   @Override
   public void onEnable() {
      saveDefaultConfig();
      instance = this;

      this.getDataFolder().mkdirs();

      DatabaseManager db = new DatabaseManager(this);
      db.connect();

      // NPCInteractListener.register(this);

      // WorldProtectionListener.setPvp(false);
      Listeners.setupListeners(this);
      Commands.setupCommands(this);
      managers = new Managers(this);

      Managers.setDatabaseManager(db);

      managers.loadAll();

      if (Managers.getDisguiseManager() != null) {
         Managers.getDisguiseManager().loadNicksFile(this);
      }
   }

   @Override
   public void onDisable() {
      try {
         DatabaseManager db = Managers.getDatabaseManager();
         if (db != null) db.disconnect();
      } catch (Exception ignored) {
      }

      Bukkit.getConsoleSender().sendMessage("§c§lTCHAU :( -> §cO plugin foi desativado.");
   }
}