package com.knowplugins.ysubz.kaoscore.bukkit.moderation.staff.command;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.player.VanishManager;
import live.pvphard.practice.HardPractice;
import live.pvphard.practice.core.profile.Profile;
import live.pvphard.practice.core.profile.ProfileService;
import live.pvphard.practice.core.profile.enums.ProfileState;
import live.pvphard.practice.feature.match.Match;

public class VanishCommand extends Commands {

   public VanishCommand() {
      super("vanish", "v");
   }

   private void toggleVanish(Player player) {
      if (VanishManager.isVanished(player)) {
         VanishManager.removeVanish(player);
         player.sendMessage("§aVanish mode disabled.");
         player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
      } else {
         endMatch(player);
         VanishManager.addVanish(player);
         player.sendMessage("§aVanish mode enabled.");
         player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
      }
   }

   @Override
   public void perform(CommandSender sender, String label, String[] args) {


      if (!(sender instanceof Player)) {
         sender.sendMessage("§cOnly players can use this command.");
         Player senderPlayer = (Player) sender;
         senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
         return;
      }


      if (!sender.hasPermission("kaoscore.vanish") || !sender.hasPermission("kaoscore.staff")) {
         sender.sendMessage("§cYou do not have permission to use this command.");
         Player senderPlayer = (Player) sender;
         senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
         return;
      }

      Player playerSender = (Player)sender;

      toggleVanish(playerSender);
   }


   public void endMatch(Player player) {
      if(Bukkit.getServer().getPluginManager().getPlugin("HardPractice") != null) {
         ProfileService profileService = HardPractice.getInstance().getService(ProfileService.class);
         Profile profile = profileService.getProfile(player.getUniqueId());

         if(profile.getState() == ProfileState.PLAYING || profile.getState() == ProfileState.PLAYING_TOURNAMENT || profile.getState() == ProfileState.PLAYING_EVENT) {
            Match match = profile.getMatch();

            if(match == null) {
               return;
            }

            if(match.canEndMatch()){
               match.handleDisconnect(player);
            }
         }
      }
   }
}
