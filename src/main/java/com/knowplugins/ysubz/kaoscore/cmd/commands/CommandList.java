package com.knowplugins.ysubz.kaoscore.cmd.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;

public class CommandList extends Commands {
   public CommandList() {
      super("listadecomandos", "commandlist", "cl");
   }

   public void perform(CommandSender sender, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§c§lCOMMANDS ➔ §cOnly players can use this command.");
      } else {
         Player player = (Player)sender;
         if (!player.hasPermission("kaoscore.staff.commandlist")) {
            player.sendMessage("§e§lDEV ➔ §eysubz.");
         } else {
            sender.sendMessage("");
            sender.sendMessage("§7Utility command list:");
            sender.sendMessage("");
            sender.sendMessage("§e/aviso");
            sender.sendMessage("§e/divulgar");
            sender.sendMessage("§e/chatvip");
            sender.sendMessage("§e/tell");
            sender.sendMessage("§e/r");
            sender.sendMessage("§e/whitelist");
            sender.sendMessage("§e/online");
            sender.sendMessage("§e/staffchat");
            sender.sendMessage("§e/gamemode");
            sender.sendMessage("§e/lobby");
            sender.sendMessage("§e/ping");
            sender.sendMessage("§e/vanish");
            sender.sendMessage("§e/discord");
            sender.sendMessage("§e/clearchat");
            sender.sendMessage("§e/teleportar");
         }
      }

   }
}
