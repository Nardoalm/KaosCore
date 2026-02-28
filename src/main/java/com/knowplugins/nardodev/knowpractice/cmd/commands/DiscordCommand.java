package com.knowplugins.nardodev.knowpractice.cmd.commands;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import com.knowplugins.nardodev.knowpractice.cmd.Commands;
import org.bukkit.entity.Player;

public class DiscordCommand extends Commands {
   public DiscordCommand() {
      super("discord", "dc");
   }

   public void perform(CommandSender sender, String label, String[] args) {
      sender.sendMessage("§9§lDISCORD ➔ §eNosso discord: §bdiscord.gg/fusionmc.");
      Player senderPlayer = (Player) sender;
      senderPlayer.playSound(senderPlayer.getLocation(), Sound.ARROW_HIT, 1.0f, 1.0f);
   }
}
