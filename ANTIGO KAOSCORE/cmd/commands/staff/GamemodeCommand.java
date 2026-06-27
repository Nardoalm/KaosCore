package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.staff;

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
         sender.sendMessage("§c§lGAMEMODE ➔ §cApenas jogadores podem utilizar este comando.");
      } else {
         Player player = (Player)sender;

         if (!player.hasPermission("kaoscore.staff.gamemode")) {
            player.sendMessage("§c§lGAMEMODE ➔ §cVocê não possui permissão para executar este comando.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);

         } else if (args.length == 0) {
            player.sendMessage("§c§lGAMEMODE ➔ §cUtilize /gamemode <modo> ou /gamemode <jogador> <modo>");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);

         } else {
            if (args.length == 1) {
               String gamemode = args[0];

               if (GameModeManager.isValidMode(gamemode)) {
                  GameModeManager.setGamemode(player, gamemode);
                  player.sendMessage("§a§lGAMEMODE ➔ §aSeu modo de jogo foi alterado para " + GameModeManager.getFrom(gamemode));
                  player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

               } else {
                  player.sendMessage("§c§lGAMEMODE ➔ §cUtilize /gamemode <modo>");
                  player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);

               }
            } else {
               Player target = Bukkit.getPlayer(args[0]);
               String gamemode = args[1];

               if (target == null || !target.isOnline()) {
                  player.sendMessage("§c§lGAMEMODE ➔ §cJogador não encontrado.");
                  player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                  return;
               }

               GameMode mode = getGameMode(args[0]);

               if (target.getGameMode() == mode) {
                  sender.sendMessage("§c§lGAMEMODE ➔ §cVocê ou o jogador já está neste modo de jogo!");
                  return;
               }

               if (GameModeManager.isValidMode(gamemode)) {
                  GameModeManager.setGamemode(target, gamemode);
                  target.sendMessage("§a§lGAMEMODE ➔ §aSeu modo de jogo foi alterado para " + GameModeManager.getFrom(gamemode));
                  target.playSound(target.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);

                  player.sendMessage("§a§lGAMEMODE ➔ §aO modo de jogo de " + target.getName() + " §afoi alterado para " + GameModeManager.getFrom(gamemode));
                  player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
               } else {
                  player.sendMessage("§c§lGAMEMODE ➔ §cUtilize /gamemode <jogador> <modo>");
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

