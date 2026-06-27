package com.knowplugins.ysubz.kaoscore.bukkit.staff.command;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.player.GameModeManager;

public class GamemodeCommand extends Commands {
   public GamemodeCommand() {
      super("gamemode", "gm");
   }

   public void perform(CommandSender sender, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§cOnly players can use this command.");
      } else {
         Player player = (Player)sender;

         if (!player.hasPermission("kaoscore.gamemode")) {
            player.sendMessage("§cYou do not have permission to execute this command.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);

         } else if (args.length == 0) {
            player.sendMessage("§cUse /gamemode <mode> or /gamemode <player> <mode>");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);

         } else {
            if (args.length == 1) {
               String gamemode = args[0];

               if (GameModeManager.isValidMode(gamemode)) {
                  GameModeManager.setGamemode(player, gamemode);
                  player.sendMessage("§aYour game mode has been changed to " + GameModeManager.getFrom(gamemode) + "§a.");
                  player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

               } else {
                  player.sendMessage("§cUse /gamemode <mode>");
                  player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);

               }
            } else {
               Player target = Bukkit.getPlayer(args[0]);
               String gamemode = args[1];

               if (target == null || !target.isOnline()) {
                  player.sendMessage("§cPlayer not found.");
                  player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                  return;
               }

               GameMode mode = getGameMode(args[0]);

               if (target.getGameMode() == mode) {
                  sender.sendMessage("§cYou or the player is already in this game mode!");
                  return;
               }

               if (GameModeManager.isValidMode(gamemode)) {
                  GameModeManager.setGamemode(target, gamemode);
                  target.sendMessage("§aYour game mode has been changed to " + GameModeManager.getFrom(gamemode) + "§a.");
                  target.playSound(target.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

                  player.sendMessage("§aThe game mode for " + target.getName() + " §ahas been changed to " + GameModeManager.getFrom(gamemode) + "§a.");
                  player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
               } else {
                  player.sendMessage("§cUse /gamemode <player> <mode>");
               }
            }

         }
      }
   }

   private GameMode getGameMode(String arg) {
      if (arg.equalsIgnoreCase("0") || arg.equalsIgnoreCase("s") || arg.equalsIgnoreCase("survival")) return GameMode.SURVIVAL;
      if (arg.equalsIgnoreCase("1") || arg.equalsIgnoreCase("c") || arg.equalsIgnoreCase("creative")) return GameMode.CREATIVE;
      if (arg.equalsIgnoreCase("2") || arg.equalsIgnoreCase("a") || arg.equalsIgnoreCase("adventure")) return GameMode.ADVENTURE;
      if (arg.equalsIgnoreCase("3") || arg.equalsIgnoreCase("sp") || arg.equalsIgnoreCase("spectator")) return GameMode.SPECTATOR;
      return null;
   }
}