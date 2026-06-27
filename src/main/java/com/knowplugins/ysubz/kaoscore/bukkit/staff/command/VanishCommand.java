package com.knowplugins.ysubz.kaoscore.bukkit.staff.command;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.VanishManager;

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

}
