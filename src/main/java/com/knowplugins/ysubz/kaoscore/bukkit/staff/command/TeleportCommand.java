package com.knowplugins.ysubz.kaoscore.bukkit.staff.command;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.disguise.DisguiseManager;

public class TeleportCommand extends Commands {
   public TeleportCommand() {
      super("tp", "teleportar", "teleport");
   }

   public void perform(CommandSender sender, String label, String[] args) {

      if (!(sender instanceof Player)) {
         sender.sendMessage("§cOnly players can use this command.");
         return;
      }

      if (args.length > 1 && Objects.equals(args[0], args[1])) {
         sender.sendMessage("§cYou cannot teleport to yourself!");
         Player senderPlayer = (Player) sender;
         senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
         return;
      }

      if (!sender.hasPermission("kaoscore.staff")) {
         sender.sendMessage("§cYou do not have permission to use this command.");
         Player senderPlayer = (Player) sender;
         senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
         return;
      }
   }

   public boolean execute(CommandSender sender, String currentAlias, String[] args) {
      if (!sender.hasPermission("kaoscore.staff")) {
         sender.sendMessage("§cYou do not have permission to use this command.");
         Player senderPlayer = (Player) sender;
         senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
         return true;
      }
      if (!this.testPermission(sender)) {
         return true;
      } else if (args.length >= 1 && args.length <= 4) {
         Player player;
         if (args.length != 1 && args.length != 3) {
            player = Bukkit.getPlayerExact(args[0]);
            if (player == null) player = DisguiseManager.getOnlinePlayerByName(args[0]);
         } else {
            if (!(sender instanceof Player)) {
               sender.sendMessage("§cEnter a player name!");
               Player senderPlayer = (Player) sender;
               senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
               return true;
            }

            player = (Player)sender;
         }

         if (player == null) {
            sender.sendMessage("§cPlayer not found: " + args[0]);
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return true;
         } else {
            if (args.length < 3) {
               Player target = Bukkit.getPlayerExact(args[args.length - 1]);
               if (target == null) target = DisguiseManager.getOnlinePlayerByName(args[args.length - 1]);
               if (target == null) {
                  sender.sendMessage("§cCould not find player " + args[args.length - 1] + ". Teleport cancelled.");
                  Player senderPlayer = (Player) sender;
                  senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                  return true;
               }

               player.teleport(target, TeleportCause.COMMAND);
               Command.broadcastCommandMessage(sender, "§a§lTELEPORT ➔ §a " + player.getDisplayName() + " §awas teleported to: " + target.getDisplayName());
               Player senderPlayer = (Player) sender;
               senderPlayer.playSound(senderPlayer.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

            } else if (player.getWorld() != null) {
               Location playerLocation = player.getLocation();
               double x = this.getCoordinate(sender, playerLocation.getX(), args[args.length - 3]);
               double y = this.getCoordinate(sender, playerLocation.getY(), args[args.length - 2], 0, 0);
               double z = this.getCoordinate(sender, playerLocation.getZ(), args[args.length - 1]);
               if (x == -3.0000001E7D || y == -3.0000001E7D || z == -3.0000001E7D) {
                  sender.sendMessage("§cInvalid coordinates!");
                  Player senderPlayer = (Player) sender;
                  senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                  return true;
               }

               playerLocation.setX(x);
               playerLocation.setY(y);
               playerLocation.setZ(z);
               player.teleport(playerLocation, TeleportCause.COMMAND);
               Command.broadcastCommandMessage(sender, String.format("§a§lTELEPORT ➔ §aTeleported %s to %.2f, %.2f, %.2f", player.getDisplayName(), x, y, z));
               Player senderPlayer = (Player) sender;
               senderPlayer.playSound(senderPlayer.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
            }

            return true;
         }
      } else {
         sender.sendMessage("§cUsage: /tp <player>");
         Player senderPlayer = (Player) sender;
         senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
         return false;
      }
   }

   private double getCoordinate(CommandSender sender, double current, String input) {
      return this.getCoordinate(sender, current, input, -30000000, 30000000);
   }

   private double getCoordinate(CommandSender sender, double current, String input, int min, int max) {
      boolean relative = input.startsWith("~");
      double result = relative ? current : 0.0D;
      if (!relative || input.length() > 1) {
         boolean exact = input.contains(".");
         if (relative) {
            input = input.substring(1);
         }

         double testResult = this.getDouble(sender, input);
         if (testResult == -3.0000001E7D) {
            return -3.0000001E7D;
         }

         result += testResult;
      }

      if (min != 0 || max != 0) {
         if (result < (double)min) {
            result = -3.0000001E7D;
         }

         if (result > (double)max) {
            result = -3.0000001E7D;
         }
      }

      return result;
   }

   private double getDouble(CommandSender sender, String input) {
      try {
         return Double.parseDouble(input);
      } catch (NumberFormatException e) {
         return -3.0000001E7D;
      }
   }

   public List tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
      Validate.notNull(sender, "Sender cannot be null");
      Validate.notNull(args, "Arguments cannot be null");
      Validate.notNull(alias, "Alias cannot be null");
      return (List)(args.length != 1 && args.length != 2 ? ImmutableList.of() : super.tabComplete(sender, alias, args));
   }
}
