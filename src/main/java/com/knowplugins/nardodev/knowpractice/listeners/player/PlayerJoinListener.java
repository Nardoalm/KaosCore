package com.knowplugins.nardodev.knowpractice.listeners.player;

import com.knowplugins.nardodev.knowpractice.KnowPractice;
import com.knowplugins.nardodev.knowpractice.managers.Managers;
import com.knowplugins.nardodev.knowpractice.managers.player.VanishManager;
import com.knowplugins.nardodev.knowpractice.managers.player.TabListManager;
import com.knowplugins.nardodev.knowpractice.managers.world.NPCManager;
import com.knowplugins.nardodev.knowpractice.utils.title.TitleUtils;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;
import com.knowplugins.nardodev.knowpractice.utils.welcomeMessages.WelcomeMessage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PlayerJoinListener implements Listener {

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      KnowPractice plugin = JavaPlugin.getPlugin(KnowPractice.class);

      player.setGameMode(GameMode.SURVIVAL);
      player.setLevel(2026);
      event.setJoinMessage(null);

      String header = "\n §a§lFUSION \n";
      String footer = "\n §eWebsite: §bfusionmc.com.br \n §eDiscord: §bdc.fusionmc.com.br \n";
      TabListManager.setTabList(player, header, footer);

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
      TitleUtils.sendTitle(player, "§a§lFUSION", "§eBem-vindo!", 20, 10, 20);

      NPCManager npcManager = Managers.getNpcManager();
      Location npcLocation = npcManager.getNPCLocation();

      if (npcLocation != null) {
         npcManager.spawnNPC(player, npcLocation);
      }

      player.getInventory().clear();

      ItemStack itemMenu = new ItemStack(Material.COMPASS);
      ItemMeta metaMenu = itemMenu.getItemMeta();
      metaMenu.setDisplayName("§aMenu de Opções §7(Clique Direito)");
      itemMenu.setItemMeta(metaMenu);

      player.getInventory().setItem(0, itemMenu);

      if (player.hasPermission("nardocore.staff.vanish")) {
         VanishManager.addVanish(player);
      } else {
         WelcomeMessage.sendWelcomeMessage(player);
      }
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
                         "  &eAdquira seu unban em: &bwww.fusionmc.com.br\n" +
                         "  &cBanido injustamente? Contate-nos via\n" +
                         "  &9&ndiscord.gg/fusionmc";

         e.disallow(
                 AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                 ChatColor.translateAlternateColorCodes('&', banMessage)
         );
      }


   }
}
