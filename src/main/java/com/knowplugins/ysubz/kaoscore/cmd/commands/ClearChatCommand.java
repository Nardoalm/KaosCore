package com.knowplugins.ysubz.kaoscore.cmd.commands;

import java.util.Arrays;
import java.util.stream.IntStream;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;

public class ClearChatCommand extends Commands {
   public ClearChatCommand() {
      super("clearchat");
   }

   @Override
   public void perform(CommandSender sender, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§c§lCHAT ➔ §cApenas jogadores podem executar este comando.");
      } else {
         Player player = (Player)sender;

         if (!player.hasPermission("nardocore.cmd.clearchat")) {
            player.sendMessage("§c§lCHAT ➔ §cVocê não tem permissão para executar este comando.");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);

         } else {
            IntStream.range(0, 200).forEach((i) -> {
               Bukkit.getOnlinePlayers().forEach((consumer) -> {
                  consumer.sendMessage("");
               });
            });
            TextComponent component = new TextComponent("");
            Arrays.stream(TextComponent.fromLegacyText("§a§lCHAT ➔ §aO chat foi limpo.")).forEach(component::addExtra);
            Bukkit.getOnlinePlayers().forEach((consumer) -> {
               consumer.spigot().sendMessage(component);
            });

            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
         }
      }
   }
}
