package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.utils.welcome.WelcomeMessage;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.player.VanishManager;

public class VanishCommand extends Commands {

   public VanishCommand() {
      super("vanish", "v");
   }

   private void toggleVanish(Player player) {
      if (VanishManager.isVanished(player)) {
         VanishManager.removeVanish(player);
         player.sendMessage("§a§lVANISH ➔ §aVanish mode disabled.");
         player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
         WelcomeMessage.sendWelcomeMessage(player);
      } else {
         VanishManager.addVanish(player);
         player.sendMessage("§a§lVANISH ➔ §aVanish mode enabled.");
         player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
      }
   }

   @Override
   public void perform(CommandSender sender, String label, String[] args) {


      if (!(sender instanceof Player)) {
         sender.sendMessage("§c§lVANISH ➔ §cOnly players can use this command.");
         Player senderPlayer = (Player) sender;
         senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
         return;
      }


      if (!sender.hasPermission("kaoscore.staff.vanish")) {
         sender.sendMessage("§c§lVANISH ➔ §cYou do not have permission to use this command.");
         Player senderPlayer = (Player) sender;
         senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
         return;
      }

      Player playerSender = (Player)sender;

      toggleVanish(playerSender);
   }

}
