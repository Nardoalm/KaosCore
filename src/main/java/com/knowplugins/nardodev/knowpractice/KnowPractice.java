package com.knowplugins.nardodev.knowpractice;

import com.knowplugins.nardodev.knowpractice.cmd.Commands;
import com.knowplugins.nardodev.knowpractice.listeners.Listeners;
import com.knowplugins.nardodev.knowpractice.listeners.entity.NPCInteractListener;
import com.knowplugins.nardodev.knowpractice.listeners.world.WorldProtectionListener;
import com.knowplugins.nardodev.knowpractice.managers.Managers;
import com.knowplugins.nardodev.knowpractice.managers.player.VanishManager;
import com.knowplugins.nardodev.knowpractice.managers.player.ScoreboardManager;
import com.knowplugins.nardodev.knowpractice.managers.world.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

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

      NPCInteractListener.register(this);

      WorldProtectionListener.setPvp(false);
      Listeners.setupListeners(this);
      Commands.setupCommands();
      managers = new Managers(this);
      managers.loadAll();
   }

   @Override
   public void onDisable() {
      Bukkit.getConsoleSender().sendMessage("§c§lTCHAU :( -> §cO plugin foi desativado.");
   }
}