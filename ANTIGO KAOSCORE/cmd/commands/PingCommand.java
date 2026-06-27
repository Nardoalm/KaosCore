package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;

public class PingCommand extends Commands {
   public PingCommand() {
      super("ping", "p");
   }

   public void perform(CommandSender sender, String label, String[] args) {
      if(args.length < 1){
         sender.sendMessage("§a§lPING ➔ §aSeu ping é de " + (sender instanceof Player ? ((CraftPlayer) sender).getHandle().ping : 0) + " §ams!");
         Player senderPlayer = (Player) sender;
         senderPlayer.playSound(senderPlayer.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
      } else {
         Player target = (args.length > 0) ? Bukkit.getPlayer(args[0]) : (sender instanceof Player ? (Player) sender : null);

         if (target == null) {
            sender.sendMessage("§a§lERRO ➔ §cJogador não encontrado.");
            if (sender instanceof Player) {
               Player p = (Player) sender;
               p.playSound(p.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
            return;
         }

         int ping = ((CraftPlayer) target).getHandle().ping;
         sender.sendMessage(String.format("§a§lPING ➔ §aO ping de %s é %d ms!", target.getName(), ping));

         if (sender instanceof Player) {
            Player p = (Player) sender;
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
         }
      }
   }
}
