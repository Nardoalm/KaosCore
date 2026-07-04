package com.knowplugins.ysubz.kaoscore.bukkit.listener;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.auth.listener.AuthListener;
import com.knowplugins.ysubz.kaoscore.bukkit.chat.ChatListener;
import com.knowplugins.ysubz.kaoscore.bukkit.friend.menu.FriendMenuListener;
import com.knowplugins.ysubz.kaoscore.bukkit.listener.player.*;
import com.knowplugins.ysubz.kaoscore.bukkit.listener.world.*;
import com.knowplugins.ysubz.kaoscore.bukkit.menu.listener.AnvilInputListener;
import com.knowplugins.ysubz.kaoscore.bukkit.permission.listener.PermissionListener;
import com.knowplugins.ysubz.kaoscore.bukkit.sit.listener.SitListener;
import com.knowplugins.ysubz.kaoscore.bukkit.util.join.listener.JoinMessageMenuListener;
import com.knowplugins.ysubz.kaoscore.bukkit.util.skin.menu.SkinMenuListener;
import com.knowplugins.ysubz.kaoscore.bukkit.whitelist.listener.WhitelistListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

public class Listeners {
   public static void setupListeners(KaosCore plugin) {
      try {
         PluginManager pm = Bukkit.getPluginManager();
         pm.registerEvents(new PermissionListener(), plugin);
         pm.registerEvents(new AuthListener(plugin), plugin);
         pm.registerEvents(new WhitelistListener(), plugin);
         pm.registerEvents(new PlayerJoinListener(), plugin);
         pm.registerEvents(new ChatListener(), plugin);
         pm.registerEvents(new JumpPadListener(plugin), plugin);
         pm.registerEvents(new JoinMessageMenuListener(), plugin);
         pm.registerEvents(new AnvilInputListener(), plugin);
         pm.registerEvents(new SkinMenuListener(), plugin);
         pm.registerEvents(new FriendMenuListener(), plugin);
         pm.registerEvents(new SitListener(), plugin);
         pm.registerEvents(new NPCWorldListener(), plugin);
         pm.registerEvents(new PlayerQuitListener(), plugin);
         pm.registerEvents(new StaffLogListener(plugin), plugin);
         pm.registerEvents(new PracticeCommandBlockListener(), plugin);
         pm.registerEvents(new DoubleJumpListener(), plugin);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}