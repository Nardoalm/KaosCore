package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.staff;

import java.util.Arrays;
import java.util.stream.IntStream;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;

public class ClearChatCommand extends Commands {
   public ClearChatCommand() {
      super("clearchat");
   }

   @Override
   public void perform(CommandSender sender, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§cOnly players can use this command.");
      } else {
         Player player = (Player) sender;

         if (!player.hasPermission("kaoscore.cmd.clearchat")) {
            player.sendMessage("§cYou do not have permission to execute this command.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);

         } else {
            IntStream.range(0, 200).forEach((i) -> {
               Bukkit.getOnlinePlayers().forEach((consumer) -> {
                  consumer.sendMessage("");
               });
            });
            TextComponent component = new TextComponent("");
            Arrays.stream(TextComponent.fromLegacyText("§aThe chat has been cleared.")).forEach(component::addExtra);
            Bukkit.getOnlinePlayers().forEach((consumer) -> {
               consumer.spigot().sendMessage(component);
            });

            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
         }
      }
   }
}