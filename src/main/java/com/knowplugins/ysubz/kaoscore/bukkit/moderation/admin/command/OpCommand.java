package com.knowplugins.ysubz.kaoscore.bukkit.moderation.admin.command;

import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import org.bukkit.Bukkit;
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
            sender.sendMessage("§c§lOP ➔ §cYou don't have permission to use this command.");
            return;
        }

        if (args.length == 0) {
            sender.sendMessage("§c§lOP ➔ §cUsage: /op <player>");
            return;
        }

        Player player = Bukkit.getPlayer(args[0]);

        if(player.isOp()) {
            sender.sendMessage("§c§lOP ➔ §cThis player is already an operator.");
            return;
        }

        if (player == null) {
            sender.sendMessage("§c§lOP ➔ §cPlayer not found.");
            return;
        }

        player.setOp(true);
        sender.sendMessage("§a§lOP ➔ §aAdministrative permissions have been enabled.");

        if (sender instanceof Player) {
            ((Player) sender).playSound(player.getLocation(), Sound.NOTE_PLING, 1f, 1f);
        }
    }
}
