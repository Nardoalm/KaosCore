package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.vip;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.integration.practice.PracticeModeBlocker;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand extends Commands {

    public FlyCommand() {
        super("fly");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        Player target;

        if (args.length > 0) {
            if (!sender.hasPermission("kaoscore.staff.fly")) {
                sender.sendMessage("§cYou do not have permission to toggle flight for other players.");
                if (sender instanceof Player) {
                    Player senderPlayer = (Player) sender;
                    senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                }
                return;
            }

            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                if (sender instanceof Player) {
                    Player senderPlayer = (Player) sender;
                    senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                }
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can use this command.");
                return;
            }
            target = (Player) sender;

            if (!target.hasPermission("kaoscore.member.fly")) {
                sender.sendMessage("§cYou do not have permission to execute this command.");
                target.playSound(target.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                return;
            }
        }

        boolean newStatus = !target.getAllowFlight();
        if (PracticeModeBlocker.checkActiveMatch(target, true)) {
            if (sender != target) {
                sender.sendMessage("§cThis player is currently in a Practice match.");
            }
            return;
        }

        target.setAllowFlight(newStatus);

        if (!newStatus) {
            target.setFlying(false);
        }

        String status = newStatus ? "enabled" : "disabled";

        target.sendMessage("§aFlight has been " + status + ".");

        if (sender != target) {
            sender.sendMessage("§aFlight has been " + status + " §afor " + target.getName() + ".");
        }
    }
}