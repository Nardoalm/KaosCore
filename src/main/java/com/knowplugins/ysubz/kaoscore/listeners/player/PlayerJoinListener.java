package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.VanishManager;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.managers.player.PunishmentManager;
import com.knowplugins.ysubz.kaoscore.managers.player.TabListManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.PlayerAddressRepository;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinData;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinRepository;
import com.knowplugins.ysubz.kaoscore.cmd.commands.vip.SkinCommand;
import com.knowplugins.ysubz.kaoscore.managers.world.NPCManager;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentEntry;
import com.knowplugins.ysubz.kaoscore.utils.punishment.PunishmentTime;
import com.knowplugins.ysubz.kaoscore.utils.title.TitleAPI;
import com.knowplugins.ysubz.kaoscore.managers.plugin.VanishRepository;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DisguiseRepository;
import com.knowplugins.ysubz.kaoscore.utils.skin.OriginalSkinManager;
import com.knowplugins.ysubz.kaoscore.utils.skin.SkinApplier;
import com.knowplugins.ysubz.kaoscore.managers.plugin.PunishmentMessageManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import com.knowplugins.ysubz.kaoscore.utils.welcome.WelcomeMessage;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      KaosCore plugin = KaosCore.getInstance();

      // player.setGameMode(GameMode.SURVIVAL);
      // player.setLevel(2026);
      event.setJoinMessage(null);

      if (plugin.getConfig().getBoolean("features.tablist.enabled", true)) {
         String header = String.join("\n", plugin.getConfig().getStringList("features.tablist.header"));
         String footer = String.join("\n", plugin.getConfig().getStringList("features.tablist.footer"));
         TabListManager.setTabList(player, header, footer);
      }

      if (!plugin.getConfig().getBoolean("features.scoreboard.enabled", true)) {
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
      }, 40L);

      player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

      if (plugin.getConfig().getBoolean("features.join-title.enabled", true)) {
         String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("features.join-title.title", "&b&lFREEP"));
         String subtitle = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("features.join-title.subtitle", "&eBem-vindo!"));
         int fadeIn = Math.max(0, plugin.getConfig().getInt("features.join-title.fade-in", 20));
         int stay = Math.max(0, plugin.getConfig().getInt("features.join-title.stay", 10));
         int fadeOut = Math.max(0, plugin.getConfig().getInt("features.join-title.fade-out", 20));
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

      /* player.getInventory().clear();

      ItemStack itemMenu = new ItemStack(Material.COMPASS);
      ItemMeta metaMenu = itemMenu.getItemMeta();
      metaMenu.setDisplayName("§aMenu de Opções §7(Clique Direito)");
      itemMenu.setItemMeta(metaMenu);
      player.getInventory().setItem(4, itemMenu); */

      if (Managers.getDatabaseManager() != null) {
         VanishRepository vanishRepo = new VanishRepository(plugin, Managers.getDatabaseManager());
         DisguiseRepository disguiseRepo = new DisguiseRepository(plugin, Managers.getDatabaseManager());
         SkinRepository skinRepo = new SkinRepository(plugin, Managers.getDatabaseManager());

         UUID uuid = player.getUniqueId();

         vanishRepo.load(uuid).thenAccept(isVanished -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
               if (player.isOnline() && isVanished && player.hasPermission("kaoscore.staff.vanish")) {
                  VanishManager.addVanish(player);
               }
            });
         });

         disguiseRepo.load(uuid).thenAccept(isDisguised -> {
            if (isDisguised != null) {
               String nick = isDisguised;
               skinRepo.load(uuid).thenAccept(skinData ->
                  Bukkit.getScheduler().runTask(plugin, () -> {
                     applyStoredDisguise(player, nick, hasStoredTextures(skinData) ? skinData : null);
                     if (isPendingSkinLookup(skinData)) {
                        SkinCommand.setSkinForOthers(player, skinData.name);
                     }
                  })
               );
               return;
            }

            skinRepo.load(uuid).thenAccept(skinData -> {
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
         });
      } else {
         scheduleJoinSkinRefresh(plugin, player);
      }

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
         if (plugin.getConfig().contains("spawn.location")) {
            try {
               String raw = plugin.getConfig().getString("spawn.location");
               String[] parts = raw.split(";");

               if (parts.length >= 6) {
                  World world = Bukkit.getWorld(parts[0]);

                  if (world != null) {
                     double x = Double.parseDouble(parts[1]);
                     double y = Double.parseDouble(parts[2]);
                     double z = Double.parseDouble(parts[3]);
                     float yaw = Float.parseFloat(parts[4]);
                     float pitch = Float.parseFloat(parts[5]);

                     Location loc = new Location(world, x, y, z, yaw, pitch);
                     player.teleport(loc);
                     if (player.hasPermission("kaoscore.vip.fly")) {
                        Location newLocation = player.getLocation().clone().add(0, 4, 0);
                        player.teleport(newLocation);
                        player.setAllowFlight(true);
                        player.setFlying(true);
                     }
                  } else {
                     Bukkit.getConsoleSender().sendMessage("§c§lSPAWN ➔ §cMundo do spawn não encontrado!");
                  }
               }
            } catch (Exception e) {
               Bukkit.getConsoleSender().sendMessage("§c§lSPAWN ➔ §cErro ao teleportar jogador para o spawn: Configuração inválida.");
            }
         }
      }, 4L);

      WelcomeMessage.sendWelcomeMessage(player);
   }

   @EventHandler
   public void onPlayerJoinTime(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      player.setPlayerTime(6000L, false);
   }

   @EventHandler
   public void onAdvancement(PlayerAchievementAwardedEvent event) {
      event.setCancelled(true);
   }

   @EventHandler
   public void onPreLogin(AsyncPlayerPreLoginEvent e) {
      PlayerAddressRepository addressRepository = Managers.getPlayerAddressRepository();
      if (addressRepository != null && e.getUniqueId() != null && e.getAddress() != null) {
         addressRepository.track(e.getUniqueId(), e.getName(), e.getAddress().getHostAddress());
      }

      PunishmentManager punishmentManager = Managers.getPunishmentManager();
      if (punishmentManager != null) {
         PunishmentEntry ban = punishmentManager.getActiveBan(e.getUniqueId(), e.getName());
         if (ban != null) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, buildPunishmentMessage(ban));
            return;
         }
      }

      if (Bukkit.getBanList(BanList.Type.NAME).isBanned(e.getName())) {
         BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(e.getName());
         if (ban != null) {
            String reason = ban.getReason() == null ? "Sem motivo informado" : ban.getReason();
            Date expires = ban.getExpiration();
            String duration = expires == null ? "Permanente" : PunishmentTime.formatDate(expires.getTime());

            KaosCore plugin = KaosCore.getInstance();
            PunishmentMessageManager messages = plugin == null ? null : plugin.getPunishmentMessageManager();
            String banMessage = messages != null
                    ? messages.buildBanScreen(expires == null, reason, duration)
                    : ("§cVocê foi banido.\n\n" +
                    "  §cMotivo: §7" + reason + "\n" +
                    "  §cExpira em: §7" + duration + "\n" +
                    "  §eAdquira seu unban em: §bwww.kaosmc.com\n" +
                    "  §cBanido injustamente? Contate-nos via\n" +
                    "  §9§ndiscord.gg/freep");

            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, banMessage);
         }
      }
   }

   private void applyStoredDisguise(Player player, String nick, SkinData skinData) {
      if (player == null || !player.isOnline() || nick == null || nick.trim().isEmpty()) {
         return;
      }

      try {
         DisguiseManager disguiseManager = Managers.getDisguiseManager();
         if (disguiseManager != null) {
            disguiseManager.setOriginalName(player, player.getName());
         }

         GameProfile profile = ((CraftPlayer) player).getProfile();
         OriginalSkinManager.cache(player, profile);
         changeName(profile, nick);

         if (skinData != null && skinData.value != null) {
            profile.getProperties().removeAll("textures");
            profile.getProperties().put("textures", new Property("textures", skinData.value, skinData.signature));
         }

         player.setDisplayName(nick);
         player.setPlayerListName(nick);

         if (disguiseManager != null) {
            disguiseManager.addInDisguise(player, nick);
         }

         KaosCore plugin = KaosCore.getInstance();
         if (plugin != null && plugin.getTagManager() != null) {
            plugin.getTagManager().refreshTag(player);
         }

         Bukkit.getScheduler().runTaskLater(JavaPlugin.getPlugin(KaosCore.class), () -> {
            if (player.isOnline()) {
               new SkinApplier(player).applySkinForOthers();
            }
         }, 2L);
      } catch (Exception e) {
         e.printStackTrace();
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

      String expires = entry == null || entry.isPermanent() ? "Permanente" : PunishmentTime.formatDate(entry.getExpiresAt());
      String reason = entry == null || entry.getReason() == null ? "Sem motivo informado" : entry.getReason();
      String title = entry != null && entry.isPermanent() ? "Você foi banido permanentemente." : "Você foi banido temporariamente.";

      String message =
              "§c" + title + "\n\n" +
                      "  §cMotivo: §7" + reason + "\n" +
                      "  §cExpira em: §7" + expires + "\n" +
                      "  §eAdquira seu unban em: §bwww.freepmc.com.br\n" +
                      "  §cBanido injustamente? Contate-nos via\n" +
                      "  §9§ndiscord.gg/freepmc";

      return message;
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

      Bukkit.getScheduler().runTaskLater(plugin, () -> applyJoinSkin(player), 2L);
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
         if (player.isOnline() && !DisguiseManager.isDisguised(player)) {
            applyJoinSkin(player);
         }
      }, 40L);
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

