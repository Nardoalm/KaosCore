package com.knowplugins.ysubz.kaoscore.bukkit.listeners;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.listeners.entity.EntityListener;
import com.knowplugins.ysubz.kaoscore.bukkit.listeners.entity.MenuClickListener;
import com.knowplugins.ysubz.kaoscore.bukkit.listeners.entity.NPCInteractListener;
import com.knowplugins.ysubz.kaoscore.bukkit.listeners.player.*;
import com.knowplugins.ysubz.kaoscore.bukkit.listeners.world.SoundListener;
import com.knowplugins.ysubz.kaoscore.bukkit.listeners.world.TimeListener;
import com.knowplugins.ysubz.kaoscore.bukkit.listeners.world.WeatherListener;
import com.knowplugins.ysubz.kaoscore.bukkit.listeners.world.WorldProtectionListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

public class Listeners {
   public static void setupListeners(KaosCore plugin) {
      try {
         PluginManager pm = Bukkit.getPluginManager();
         /* try {
            WorldProtectionListener.setPvp(plugin.getConfig().getBoolean("features.protections.pvp.enabled", true));
         } catch (Exception ignored) {
         } */
         pm.registerEvents(new PermissionListener(), plugin);
         // pm.registerEvents(new ProtectionListener(), plugin);
         pm.registerEvents(new WhitelistListener(), plugin);
         pm.registerEvents(new PlayerJoinListener(), plugin);
         // pm.registerEvents(new EntityListener(), plugin);
         pm.registerEvents(new ChatListener(), plugin);
         pm.registerEvents(new WelcomeMessageMenuListener(), plugin);
         pm.registerEvents(new AnvilInputListener(), plugin);
         pm.registerEvents(new SkinMenuListener(), plugin);
         pm.registerEvents(new FriendMenuListener(), plugin);
         pm.registerEvents(new SitListener(), plugin);
         // pm.registerEvents(new WeatherListener(), plugin);
         // pm.registerEvents(new InteractListener(), plugin);
         // pm.registerEvents(new MenuClickListener(), plugin);
         // pm.registerEvents(new InventoryProtectListener(), plugin);
         pm.registerEvents(new PlayerQuitListener(), plugin);
         pm.registerEvents(new StaffLogListener(plugin), plugin);
         pm.registerEvents(new PracticeCommandBlockListener(), plugin);
         // pm.registerEvents(new WorldProtectionListener(), plugin);
         pm.registerEvents(new SoundListener(), plugin);
         // pm.registerEvents(new TimeListener(), plugin);
         pm.registerEvents(new DoubleJumpListener(), plugin);

         NPCInteractListener.register(plugin);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
