package com.knowplugins.ysubz.kaoscore.listeners.player;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.managers.Managers;
import com.knowplugins.ysubz.kaoscore.managers.player.VanishManager;
import com.knowplugins.ysubz.kaoscore.managers.player.DisguiseManager;
import com.knowplugins.ysubz.kaoscore.managers.player.TabListManager;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinData;
import com.knowplugins.ysubz.kaoscore.managers.plugin.SkinRepository;
import com.knowplugins.ysubz.kaoscore.managers.world.NPCManager;
import com.knowplugins.ysubz.kaoscore.utils.title.TitleUtils;
import com.knowplugins.ysubz.kaoscore.managers.plugin.VanishRepository;
import com.knowplugins.ysubz.kaoscore.managers.plugin.DisguiseRepository;
import com.knowplugins.ysubz.kaoscore.utils.skin.OriginalSkinManager;
import com.knowplugins.ysubz.kaoscore.utils.skin.SkinApplier;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import com.knowplugins.ysubz.kaoscore.utils.welcomeMessages.WelcomeMessage;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      KaosCore plugin = JavaPlugin.getPlugin(KaosCore.class);

      // player.setGameMode(GameMode.SURVIVAL);
      // player.setLevel(2026);
      event.setJoinMessage(null);

      String header = "\n §6§lKAOS \n";
      String footer = "\n §eWebsite: §bkaosmc.com \n §eDiscord: §bdc.kaosmc.com \n";
      TabListManager.setTabList(player, header, footer);

      if (plugin.getTagManager() != null) {
         plugin.getTagManager().getTag(player);
      }

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
         if (player.isOnline() && plugin.getTagManager() != null) {
            plugin.getTagManager().refreshViewer(player);
            plugin.getTagManager().refreshTag(player);
         }
      }, 1L);

      player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);


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
                  if (player.hasPermission("nardocore.member.fly")) {
                     Location newLocation = player.getLocation().clone().add(0, 4, 0);
                     player.teleport(newLocation);
                     player.setAllowFlight(true);
                     player.setFlying(true);
                  }
               } else {
                  Bukkit.getConsoleSender().sendMessage("§c§lERRO ➔ §cMundo do spawn não encontrado!");
               }
            }
         } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§c§lERRO ➔ §cErro ao teleportar jogador para o spawn: Configuração inválida.");
         }
      }
      // TitleUtils.sendTitle(player, "§a§lFUSION", "§eBem-vindo!", 20, 10, 20);

      /* NPCManager npcManager = Managers.getNpcManager();
      Location npcLocation = npcManager.getNPCLocation();

      if (npcLocation != null) {
         npcManager.spawnNPC(player, npcLocation);
      }

      player.getInventory().clear();

      ItemStack itemMenu = new ItemStack(Material.COMPASS);
      ItemMeta metaMenu = itemMenu.getItemMeta();
      metaMenu.setDisplayName("§aMenu de Opções §7(Clique Direito)");
      itemMenu.setItemMeta(metaMenu);

      player.getInventory().setItem(0, itemMenu); */

      if (Managers.getDatabaseManager() != null) {
         VanishRepository vanishRepo = new VanishRepository(plugin, Managers.getDatabaseManager());
         DisguiseRepository disguiseRepo = new DisguiseRepository(plugin, Managers.getDatabaseManager());
         SkinRepository skinRepo = new SkinRepository(plugin, Managers.getDatabaseManager());

         UUID uuid = player.getUniqueId();

         vanishRepo.load(uuid).thenAccept(isVanished -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
               if (player.isOnline() && isVanished && player.hasPermission("nardocore.staff.vanish")) {
                  VanishManager.addVanish(player);
               }
            });
         });

         disguiseRepo.load(uuid).thenAccept(isDisguised -> {
            if (isDisguised != null) {
               String nick = isDisguised;
               skinRepo.load(uuid).thenAccept(skinData ->
                  Bukkit.getScheduler().runTask(plugin, () -> applyStoredDisguise(player, nick, skinData))
               );
            }
         });
      }

      WelcomeMessage.sendWelcomeMessage(player);
   }

   @EventHandler
   public void onAdvancement(PlayerAchievementAwardedEvent event) {
      event.setCancelled(true);
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      event.setQuitMessage(null);
   }

   @EventHandler
   public void onPreLogin(AsyncPlayerPreLoginEvent e) {
      if (Bukkit.getBanList(BanList.Type.NAME).isBanned(e.getName())) {

         BanEntry ban = Bukkit.getBanList(BanList.Type.NAME).getBanEntry(e.getName());
         String reason = ban.getReason();
         Date expires = ban.getExpiration();

         SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

         String banMessage =
                 "&cVocê foi banido temporariamente.\n\n" +
                         "  &cMotivo: &7" + reason + "\n" +
                         "  &cExpira em: &7" + sdf.format(expires) + "\n" +
                         "  &eAdquira seu unban em: &bwww.kaosmc.com\n" +
                         "  &cBanido injustamente? Contate-nos via\n" +
                         "  &9&ndiscord.gg/kaosmc";

         e.disallow(
                 AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                 ChatColor.translateAlternateColorCodes('&', banMessage)
         );
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

   private void changeName(GameProfile profile, String name) throws Exception {
      Field nameField = profile.getClass().getDeclaredField("name");
      nameField.setAccessible(true);
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(nameField, nameField.getModifiers() & ~Modifier.FINAL);
      nameField.set(profile, name);
   }
}
