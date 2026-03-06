package com.knowplugins.ysubz.kaoscore;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.cmd.commands.TagCommand;
import com.knowplugins.ysubz.kaoscore.listeners.Listeners;
import com.knowplugins.ysubz.kaoscore.managers.ClanManager;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.TagManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DatabaseManager;
import com.knowplugins.ysubz.kaoscore.utils.placeholder.KaosCoreExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class KaosCore extends JavaPlugin {
   private static KaosCore instance;
   private Managers managers;
   private TagManager tagManager;

   public static KaosCore getInstance() {
      return instance;
   }

   public TagManager getTagManager() {
      return tagManager;
   }

   public ClanManager getClanManager() {
      return Managers.getClanManager();
   }

   @Override
   public void onEnable() {
      saveDefaultConfig();
      saveResource("tags.yml", false);
      instance = this;

      this.getDataFolder().mkdirs();

      DatabaseManager db = new DatabaseManager(this);
      if (!db.connect() || !db.isConnected()) {
         getLogger().severe("Nao foi possivel conectar ao banco de dados. Desativando o plugin.");
         Bukkit.getPluginManager().disablePlugin(this);
         return;
      }

      Listeners.setupListeners(this);
      Commands.setupCommands(this);
      managers = new Managers(this);

      Managers.setDatabaseManager(db);

      managers.loadAll();
      tagManager = new TagManager(this);

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
         DatabaseManager db = Managers.getDatabaseManager();
         if (db != null) db.disconnect();
      } catch (Exception ignored) {
      }

      Bukkit.getConsoleSender().sendMessage("§c§lTCHAU :( -> §cO plugin foi desativado.");
   }
}
