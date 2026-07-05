package com.knowplugins.ysubz.kaoscore.bukkit.moderation.admin.command;

import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpCommand extends Commands {
    public OpCommand() {
        super("op");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("kaoscore.op")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /op <player>");
            return;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);

        if (player == null || !player.hasPlayedBefore()) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }

        if (player.isOp()) {
            sender.sendMessage("§cThis player is already an operator.");
            return;
        }

        player.setOp(true);
        sender.sendMessage("§aAdministrative permissions have been enabled.");

        if (sender instanceof Player && player.isOnline()) {
            Player targetPlayer = player.getPlayer();
            ((Player) sender).playSound(targetPlayer.getLocation(), Sound.NOTE_PLING, 1f, 1f);
        }
    }
}