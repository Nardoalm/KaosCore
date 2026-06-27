package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.message.sendRawJson;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscordCommand extends Commands {

   public DiscordCommand() {
      super("discord", "dc");
   }

   @Override
   public void perform(CommandSender sender, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§9§lDISCORD ➔ §ediscord.gg/pvphard");
         return;
      }

      Player player = (Player) sender;
      String link = "https://discord.gg/pvphard";

      sendRawJson.sendDiscordLink(player, link);

      player.playSound(player.getLocation(), Sound.ARROW_HIT, 1.0f, 1.0f);
   }
}