package com.knowplugins.ysubz.kaoscore.bukkit.managers.player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.module.staffmod.StaffModModule;
import com.lunarclient.apollo.module.staffmod.StaffMod;
import com.lunarclient.apollo.recipients.Recipients;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

import static com.knowplugins.ysubz.kaoscore.bukkit.utils.message.ActionBarUtil.sendActionBar;

public class VanishManager {

   private static final Set<UUID> USING_VANISH = new HashSet<>();
   private static String version = "";

   public static boolean isVanished(Player player) {
      if (player == null) return false;
      return USING_VANISH.contains(player.getUniqueId());
   }

   public static void addVanish(Player player) {
      if (player == null) return;
      USING_VANISH.add(player.getUniqueId());
      updateVanishState(player);
      toggleStaffModules(player, true);
   }

   public static void removeVanish(Player player) {
      if (player == null) return;
      USING_VANISH.remove(player.getUniqueId());
      toggleStaffModules(player, false);
      for (Player online : Bukkit.getOnlinePlayers()) {
         online.showPlayer(player);
      }
   }

   public static void setupManager() {
      try {
         version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
      } catch (Exception e) {
         version = "v1_8_R3";
      }

      new BukkitRunnable() {
         @Override
         public void run() {
            for (UUID uuid : new HashSet<>(USING_VANISH)) {
               Player player = Bukkit.getPlayer(uuid);
               if (player == null) continue;

               sendActionBar(player, "§dYou are in vanish.");

               updateVanishState(player);
            }
         }
      }.runTaskTimer(JavaPlugin.getPlugin(KaosCore.class), 0L, 20L);
   }

   private static void updateVanishState(Player player) {
      for (Player online : Bukkit.getOnlinePlayers()) {
         if (!online.hasPermission("kaoscore.staff.vanish")) {
            online.hidePlayer(player);
         }
      }
   }

   private static void toggleStaffModules(Player player, boolean active) {
      StaffModModule staffModModule = Apollo.getModuleManager().getModule(StaffModModule.class);
      if (staffModModule != null) {
         com.lunarclient.apollo.player.ApolloPlayer apolloPlayer = Apollo.getPlayerManager()
                 .getPlayer(player.getUniqueId())
                 .orElse(null);

         if (apolloPlayer != null) {
            Iterable<com.lunarclient.apollo.player.ApolloPlayer> recipientsIterable = Collections.singletonList(apolloPlayer);
            if (active) {
               staffModModule.enableStaffMods(Recipients.of(recipientsIterable), Collections.singletonList(StaffMod.XRAY));
            } else {
               staffModModule.disableStaffMods(Recipients.of(recipientsIterable), Collections.singletonList(StaffMod.XRAY));
            }
         }
      }

      byte[] cbPacket = buildCBStaffModPacket("XRAY", active);
      if (cbPacket != null) {
         player.sendPluginMessage(JavaPlugin.getPlugin(KaosCore.class), "CB|Binary", cbPacket);
      }
   }

   private static byte[] buildCBStaffModPacket(String modName, boolean state) {
      try {
         ByteBuf buffer = Unpooled.buffer();

         int packetId = 5;
         while ((packetId & 0xFFFFFF80) != 0x0) {
            buffer.writeByte((packetId & 0x7F) | 0x80);
            packetId >>>= 7;
         }
         buffer.writeByte(packetId);

         byte[] stringBytes = modName.getBytes(java.nio.charset.StandardCharsets.UTF_8);
         int stringLen = stringBytes.length;
         while ((stringLen & 0xFFFFFF80) != 0x0) {
            buffer.writeByte((stringLen & 0x7F) | 0x80);
            stringLen >>>= 7;
         }
         buffer.writeByte(stringLen);
         buffer.writeBytes(stringBytes);

         buffer.writeBoolean(state);

         byte[] packetData = new byte[buffer.readableBytes()];
         buffer.readBytes(packetData);
         return packetData;
      } catch (Exception e) {
         return null;
      }
   }
}