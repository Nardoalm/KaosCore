package com.knowplugins.nardodev.knowpractice;

import com.knowplugins.nardodev.knowpractice.cmd.Commands;
import com.knowplugins.nardodev.knowpractice.listeners.Listeners;
import com.knowplugins.nardodev.knowpractice.listeners.entity.NPCInteractListener;
import com.knowplugins.nardodev.knowpractice.listeners.world.WorldProtectionListener;
import com.knowplugins.nardodev.knowpractice.managers.Managers;
import com.knowplugins.nardodev.knowpractice.managers.plugin.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class KnowPractice extends JavaPlugin {
   private static KnowPractice instance;
   private Managers managers;

   public static KnowPractice getInstance() {
      return instance;
   }

   @Override
   public void onEnable() {
      saveDefaultConfig();
      instance = this;

      this.getDataFolder().mkdirs();

      DatabaseManager db = new DatabaseManager(this);
      db.connect();

      NPCInteractListener.register(this);

      WorldProtectionListener.setPvp(false);
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