package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.utils.message.sendRawJson;
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
         sender.sendMessage("§9§lDISCORD ➔ §ediscord.gg/kaosmc");
         return;
      }

      Player player = (Player) sender;
      String link = "https://discord.gg/kaosmc";

      sendRawJson.sendDiscordLink(player, link);

      player.playSound(player.getLocation(), Sound.ARROW_HIT, 1.0f, 1.0f);
   }
}