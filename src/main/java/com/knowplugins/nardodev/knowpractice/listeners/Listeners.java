package com.knowplugins.nardodev.knowpractice.listeners;

import com.knowplugins.nardodev.knowpractice.KnowPractice;
import com.knowplugins.nardodev.knowpractice.listeners.entity.EntityListener;
import com.knowplugins.nardodev.knowpractice.listeners.entity.MenuClickListener;
import com.knowplugins.nardodev.knowpractice.listeners.entity.NPCInteractListener;
import com.knowplugins.nardodev.knowpractice.listeners.player.*;
import com.knowplugins.nardodev.knowpractice.listeners.world.WeatherListener;
import com.knowplugins.nardodev.knowpractice.listeners.world.WorldProtectionListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

public class Listeners {
   public static void setupListeners(KnowPractice plugin) {
      try {
         PluginManager pm = Bukkit.getPluginManager();
         pm.registerEvents(new ProtectionListener(), plugin);
         pm.registerEvents(new PlayerJoinListener(), plugin);
         pm.registerEvents(new EntityListener(), plugin);
         pm.registerEvents(new ChatListener(), plugin);
         pm.registerEvents(new WeatherListener(), plugin);
         pm.registerEvents(new InteractListener(), plugin);
         pm.registerEvents(new MenuClickListener(), plugin);
         pm.registerEvents(new NPCInteractListener(), plugin);
         pm.registerEvents(new InventoryProtectListener(), plugin);
         pm.registerEvents(new PlayerQuitListener(), plugin);
         pm.registerEvents(new WorldProtectionListener(), plugin);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}