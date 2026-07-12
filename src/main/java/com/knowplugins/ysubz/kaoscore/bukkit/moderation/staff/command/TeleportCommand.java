package com.knowplugins.ysubz.kaoscore.bukkit.moderation.staff.command;

import com.google.common.collect.ImmutableList;
import java.util.List;
import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.Tag;
import com.knowplugins.ysubz.kaoscore.bukkit.util.tag.manager.TagManager;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.util.disguise.DisguiseManager;

public class TeleportCommand extends Commands {

   private static final double INVALID_COORD = -3.0000001E7D;

   public TeleportCommand() {
      super("tp", "teleportar", "teleport");
   }

   public void perform(CommandSender sender, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§cOnly players can use this command.");
         return;
      }

      Player p = (Player) sender;
      if (!p.hasPermission("kaoscore.staff")) {
         sendError(p, "§cYou do not have permission to use this command.");
         return;
      }

      if (args.length > 1 && args[0].equalsIgnoreCase(args[1])) {
         sendError(p, "§cYou cannot teleport to yourself!");
      }
   }

   public boolean execute(CommandSender sender, String currentAlias, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§cOnly players can use this command.");
         return true;
      }

      Player p = (Player) sender;

      if (!p.hasPermission("kaoscore.staff") || !this.testPermission(p)) {
         sendError(p, "§cYou do not have permission to use this command.");
         return true;
      }

      if (args.length < 1 || args.length > 4) {
         sendError(p, "§cUsage: /tp <player> [target] or /tp [player] <x> <y> <z>");
         return false;
      }

      Player playerToTeleport;
      if (args.length == 2 || args.length == 4) {
         playerToTeleport = getTargetPlayer(args[0]);
      } else {
         playerToTeleport = p;
      }

      if (playerToTeleport == null) {
         sendError(p, "§cPlayer not found §e" + args[0]);
         return true;
      }

      if (args.length < 3) {
         String targetName = args[args.length - 1];
         Player target = getTargetPlayer(targetName);

         if (target == null) {
            sendError(p, "§cCould not find player " + targetName + ". Teleport cancelled.");
            return true;
         }

         if (playerToTeleport.equals(target)) {
            sendError(p, "§cYou cannot teleport a player to themselves!");
            return true;
         }

         String playerColor = getTagColor(playerToTeleport);
         String targetColor = getTagColor(target);

         playerToTeleport.teleport(target, TeleportCause.COMMAND);
         Command.broadcastCommandMessage(p, "§a" + playerColor + playerToTeleport.getName() + " §awas teleported to " + targetColor + target.getName() + "§a.");
         p.playSound(p.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);

      } else if (playerToTeleport.getWorld() != null) {
         Location loc = playerToTeleport.getLocation();
         double x = this.getCoordinate(p, loc.getX(), args[args.length - 3]);
         double y = this.getCoordinate(p, loc.getY(), args[args.length - 2], 0, 0);
         double z = this.getCoordinate(p, loc.getZ(), args[args.length - 1]);

         if (x == INVALID_COORD || y == INVALID_COORD || z == INVALID_COORD) {
            sendError(p, "§cInvalid coordinates!");
            return true;
         }

         loc.setX(x);
         loc.setY(y);
         loc.setZ(z);
         playerToTeleport.teleport(loc, TeleportCause.COMMAND);

         String playerColor = getTagColor(playerToTeleport);
         Command.broadcastCommandMessage(p, String.format("§aTeleported %s%s §ato %.2f, %.2f, %.2f", playerColor, playerToTeleport.getName(), x, y, z));
         p.playSound(p.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
      }

      return true;
   }

   private Player getTargetPlayer(String name) {
      Player target = Bukkit.getPlayerExact(name);
      if (target == null) {
         target = DisguiseManager.getOnlinePlayerByName(name);
      }
      return target;
   }

   private String getTagColor(Player player) {
      if (KaosCore.getInstance() != null) {
         TagManager tagManager = KaosCore.getInstance().getTagManager();
         if (tagManager != null) {
            Tag tag = tagManager.getTag(player);
            if (tag != null && tag.getColor() != null) {
               return tag.getColor();
            }
         }
      }
      return "";
   }

   private void sendError(Player player, String message) {
      player.sendMessage(message);
      player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
   }

   private double getCoordinate(CommandSender sender, double current, String input) {
      return this.getCoordinate(sender, current, input, -30000000, 30000000);
   }

   private double getCoordinate(CommandSender sender, double current, String input, int min, int max) {
      boolean relative = input.startsWith("~");
      double result = relative ? current : 0.0D;
      if (!relative || input.length() > 1) {
         if (relative) {
            input = input.substring(1);
         }

         double testResult = this.getDouble(input);
         if (testResult == INVALID_COORD) {
            return INVALID_COORD;
         }

         result += testResult;
      }

      if (min != 0 || max != 0) {
         if (result < (double) min || result > (double) max) {
            return INVALID_COORD;
         }
      }

      return result;
   }

   private double getDouble(String input) {
      try {
         return Double.parseDouble(input);
      } catch (NumberFormatException e) {
         return INVALID_COORD;
      }
   }

   public List tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
      Validate.notNull(sender, "Sender cannot be null");
      Validate.notNull(args, "Arguments cannot be null");
      Validate.notNull(alias, "Alias cannot be null");
      return (List) (args.length != 1 && args.length != 2 ? ImmutableList.of() : super.tabComplete(sender, alias, args));
   }
}