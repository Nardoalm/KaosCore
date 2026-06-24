package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.KaosCore;
import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminChatCommand extends Commands {

    public AdminChatCommand() {
        super("adminchat", "ac");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can run this command!");
            return;
        }

        if (!sender.hasPermission("kaoscore.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /adminchat <message>");
            return;
        }

        StringBuilder message = new StringBuilder();
        for (String arg : args) {
            message.append(arg).append(" ");
        }


        String formattedMessage = ChatColor.translateAlternateColorCodes('&', "&c[AdminChat] " + KaosCore.getInstance().getTagManager().getTag(((Player) sender).getPlayer()).getColor() + sender.getName() + ": &f" + message.toString().trim());

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }

            if (player.hasPermission("kaoscore.admin")) {
                player.sendMessage(formattedMessage);
            }
        }
    }
}