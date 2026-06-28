package com.knowplugins.ysubz.kaoscore.bukkit.listeners.player;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.account.AccountController;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.VanishManager;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.TabListManager;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.repository.PlayerAddressRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.SkinData;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.repository.SkinRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.repository.SkinCacheRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.list.vip.SkinCommand;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.world.NPCManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.bukkit.util.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.bukkit.util.title.TitleAPI;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.repository.VanishRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.repository.DisguiseRepository;
import com.knowplugins.ysubz.kaoscore.bukkit.util.skin.OriginalSkinManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.skin.SkinApplier;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.plugin.PunishmentMessageManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import com.knowplugins.ysubz.kaoscore.bukkit.util.welcome.WelcomeMessage;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PlayerJoinListener implements Listener {

   private static final ConcurrentMap<UUID, CachedDisguise> pendingDisguises = new ConcurrentHashMap<>();

   private static class CachedDisguise {
      String nick;
      SkinData skin;
      CachedDisguise(String nick, SkinData skin) {
         this.nick = nick;
         this.skin = skin;
      }
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onPlayerLogin(PlayerLoginEvent event) {
      Player player = event.getPlayer();
      CachedDisguise cache = pendingDisguises.get(player.getUniqueId());

      if (cache != null) {
         try {
            DisguiseManager disguiseManager = Managers.getDisguiseManager();
            if (disguiseManager != null) {
               disguiseManager.setOriginalName(player, player.getName());
            }

            GameProfile profile = ((CraftPlayer) player).getProfile();
            OriginalSkinManager.cache(player, profile);
            changeName(profile, cache.nick);

            if (cache.skin != null && cache.skin.value != null) {
               profile.getProperties().removeAll("textures");
               profile.getProperties().put("textures", new Property("textures", cache.skin.value, cache.skin.signature));
            }

            if (disguiseManager != null) {
               disguiseManager.addInDisguise(player, cache.nick);
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      KaosCore plugin = KaosCore.getInstance();
      if (plugin == null) return;

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
         if (player.isOnline() && Managers.getPermissionManager() != null) {
            try {
               Managers.getPermissionManager().handleLogin(player);
            } catch (Throwable t) {
               plugin.getLogger().warning("Could not inject permissions for " + player.getName());
            }
         }
      }, 20L);

      FileConfiguration config = plugin.getConfig();
      AccountController accounts = plugin.getAccountController();

      if (accounts != null) {
         String ip = null;
         if (player.getAddress() != null && player.getAddress().getAddress() != null) {
            ip = player.getAddress().getAddress().getHostAddress();
         }
         accounts.trackLogin(uuid, player.getName(), ip);
      }

      event.setJoinMessage(null);

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
         if (player.isOnline()) {
            plugin.getAuthManager().handleJoin(player);
         }
      }, 2L);

      if (config.getBoolean("features.tablist.enabled", true)) {
         String header = String.join("\n", config.getStringList("features.tablist.header"));
         String footer = String.join("\n", config.getStringList("features.tablist.footer"));
         TabListManager.setTabList(player, header, footer);
      }

      if (!config.getBoolean("features.scoreboard.enabled", true)) {
         try {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
         } catch (Exception ignored) {
         }
      }

      if (plugin.getTagManager() != null) {
         plugin.getTagManager().getTag(player);
      }

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
         if (player.isOnline() && plugin.getTagManager() != null) {
            plugin.getTagManager().refreshViewer(player);
            plugin.getTagManager().refreshTag(player);
         }
      }, 1L);

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
         if (player.isOnline() && Managers.getClanManager() != null) {
            Managers.getClanManager().notifyPendingInvites(player);
         }
      }, 20L);

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
         if (player.isOnline() && Managers.getFriendManager() != null) {
            Managers.getFriendManager().notifyPendingRequests(player);
         }
      }, 24L);

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
         if (player.isOnline() && Managers.getFriendManager() != null) {
            Managers.getFriendManager().notifyFriendJoin(player);
         }
      }, 30L);

      player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

      if (config.getBoolean("features.join-title.enabled", true)) {
         String title = ChatColor.translateAlternateColorCodes('&', config.getString("features.join-title.title", "&b&lFREEP"));
         String subtitle = ChatColor.translateAlternateColorCodes('&', config.getString("features.join-title.subtitle", "&eBem-vindo!"));
         int fadeIn = Math.max(0, config.getInt("features.join-title.fade-in", 20));
         int stay = Math.max(0, config.getInt("features.join-title.stay", 10));
         int fadeOut = Math.max(0, config.getInt("features.join-title.fade-out", 20));
         TitleAPI.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
      }

      NPCManager npcManager = Managers.getNpcManager();
      if (npcManager != null) {
         Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && Managers.getNpcManager() != null) {
               Managers.getNpcManager().spawnAll(player);
            }
         }, 20L);
      }

      CachedDisguise cache = pendingDisguises.remove(uuid);

      if (cache != null) {
         player.setDisplayName(cache.nick);

         Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
               new SkinApplier(player).applySkinForOthers();
               KaosCore core = KaosCore.getInstance();
               if (core != null && core.getTagManager() != null) {
                  core.getTagManager().refreshTag(player);
               }
               if (Managers.getDisguiseManager() != null) {
                  Managers.getDisguiseManager().updateTabName(player);
               }
            }
         }, 2L);

         if (isPendingSkinLookup(cache.skin)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> SkinCommand.setSkinForOthers(player, cache.nick), 10L);
         }

         WelcomeMessage.sendWelcomeMessage(player);

      } else {
         if (Managers.getDatabaseManager() != null) {
            VanishRepository vanishRepo = new VanishRepository(plugin, Managers.getDatabaseManager());
            SkinRepository skinRepo = new SkinRepository(plugin, Managers.getDatabaseManager());

            vanishRepo.load(uuid).thenAccept(isVanished -> {
               if (isVanished != null && isVanished) {
                  Bukkit.getScheduler().runTask(plugin, () -> {
                     if (player.isOnline() && player.hasPermission("kaoscore.staff.vanish")) {
                        VanishManager.addVanish(player);
                     }
                  });
               }
            });

            skinRepo.load(uuid).thenAccept(skinData -> {
               if (!player.isOnline()) return;

               if (hasStoredTextures(skinData)) {
                  Bukkit.getScheduler().runTask(plugin, () -> applyStoredSkin(player, skinData));
                  return;
               }

               if (isPendingSkinLookup(skinData)) {
                  Bukkit.getScheduler().runTask(plugin, () -> SkinCommand.setSkin(player, skinData.name));
                  return;
               }

               scheduleJoinSkinRefresh(plugin, player);
            });
         } else {
            scheduleJoinSkinRefresh(plugin, player);
         }
         WelcomeMessage.sendWelcomeMessage(player);
      }
   }

   @EventHandler
   public void onAdvancement(PlayerAchievementAwardedEvent event) {
      event.setCancelled(true);
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onPreLogin(AsyncPlayerPreLoginEvent e) {
      PlayerAddressRepository addressRepository = Managers.getPlayerAddressRepository();
      if (addressRepository != null && e.getUniqueId() != null && e.getAddress() != null) {
         addressRepository.track(e.getUniqueId(), e.getName(), e.getAddress().getHostAddress());
      }

      KaosCore plugin = KaosCore.getInstance();

      if (plugin != null && plugin.getAuthPreLoginListener() != null) {
         plugin.getAuthPreLoginListener().processPreLogin(e);
         if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
         }
      }

      PunishmentManager punishmentManager = Managers.getPunishmentManager();
      if (punishmentManager != null) {
         PunishmentEntry ban = punishmentManager.getActiveBan(e.getUniqueId(), e.getName());
         if (ban != null) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, buildPunishmentMessage(ban));
            return;
         }
      }

      if (Managers.getDatabaseManager() != null) {
         DisguiseRepository disguiseRepo = new DisguiseRepository(KaosCore.getInstance(), Managers.getDatabaseManager());
         disguiseRepo.load(e.getUniqueId()).thenAccept(res -> {
            if (res != null && !res.trim().isEmpty()) {
               SkinCacheRepository skinCacheRepo = new SkinCacheRepository(KaosCore.getInstance(), Managers.getDatabaseManager());
               SkinData skin = skinCacheRepo.loadSync(res);
               pendingDisguises.put(e.getUniqueId(), new CachedDisguise(res, skin));
            }
         });
      }

      if (Bukkit.getBanList(BanList.Type.NAME).isBanned(e.getName())) {
         BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(e.getName());
         if (ban != null) {
            String reason = ban.getReason() == null ? "No reason provided" : ban.getReason();
            Date expires = ban.getExpiration();
            String duration = expires == null ? "Permanent" : PunishmentTime.formatDate(expires.getTime());

            PunishmentMessageManager messages = plugin == null ? null : plugin.getPunishmentMessageManager();
            String banMessage = messages != null
                    ? messages.buildBanScreen(expires == null, reason, duration)
                    : ("§cYou have been banned.\n\n" +
                    "  §cReason: §7" + reason + "\n" +
                    "  §cExpires in: §7" + duration + "\n" +
                    "  §eBuy your unban at: §bstore.alleypvp.us\n" +
                    "  §cBanned unfairly? Contact us via\n" +
                    "  §9§ndiscord.gg/freep");

            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, banMessage);
         }
      }
   }

   private void applyStoredSkin(Player player, SkinData skinData) {
      if (player == null || !player.isOnline() || skinData == null || skinData.value == null || skinData.value.trim().isEmpty()) {
         return;
      }

      try {
         GameProfile profile = ((CraftPlayer) player).getProfile();
         OriginalSkinManager.cache(player, profile);

         profile.getProperties().removeAll("textures");
         profile.getProperties().put("textures", new Property("textures", skinData.value, skinData.signature));

         new SkinApplier(player).applySkinForSelf();

         Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(KaosCore.class), () -> {
            if (player.isOnline()) {
               KaosCore core = KaosCore.getInstance();
               if (core != null && core.getTagManager() != null) {
                  core.getTagManager().refreshTag(player);
               }
            }
         }, 5L);

      } catch (Exception e) {
         e.printStackTrace();
         Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(KaosCore.class), () -> applyJoinSkin(player), 2L);
      }
   }

   private void changeName(GameProfile profile, String name) throws Exception {
      Field nameField = profile.getClass().getDeclaredField("name");
      nameField.setAccessible(true);
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);
      nameField.set(profile, name);
   }

   private String buildPunishmentMessage(PunishmentEntry entry) {
      KaosCore plugin = KaosCore.getInstance();
      PunishmentMessageManager messages = plugin == null ? null : plugin.getPunishmentMessageManager();
      if (messages != null) {
         return messages.buildBanScreen(entry);
      }

      String expires = entry == null || entry.isPermanent() ? "Permanent" : PunishmentTime.formatDate(entry.getExpiresAt());
      String reason = entry == null || entry.getReason() == null ? "No reason provided" : entry.getReason();
      String title = entry != null && entry.isPermanent() ? "You have been permanently banned." : "You have been temporarily banned.";

      return "§c" + title + "\n\n" +
              "  §cReason: §7" + reason + "\n" +
              "  §cExpires in: §7" + expires + "\n" +
              "  §eBuy your unban at: §bstore.pvphard.live\n" +
              "  §cBanned unfairly? Contact us via\n" +
              "  §9§ndc.pvphard.live";
   }

   private void applyJoinSkin(Player player) {
      if (player == null || !player.isOnline()) {
         return;
      }

      if (DisguiseManager.isDisguised(player)) {
         return;
      }

      KaosCore plugin = KaosCore.getInstance();
      if (plugin == null || plugin.getSkinPoolManager() == null) {
         return;
      }

      plugin.getSkinPoolManager().applyJoinSkin(player);
   }

   private void scheduleJoinSkinRefresh(KaosCore plugin, Player player) {
      if (plugin == null || player == null) {
         return;
      }

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
         if (player.isOnline() && !DisguiseManager.isDisguised(player)) {
            applyJoinSkin(player);
         }
      }, 2L);
   }

   private boolean hasStoredTextures(SkinData skinData) {
      return skinData != null && skinData.value != null && !skinData.value.trim().isEmpty();
   }

   private boolean isPendingSkinLookup(SkinData skinData) {
      return skinData != null
              && skinData.name != null
              && !skinData.name.trim().isEmpty()
              && (skinData.value == null || skinData.value.trim().isEmpty());
   }
}