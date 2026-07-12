package com.knowplugins.ysubz.kaoscore.bukkit.util.sit.command;

import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.util.sit.SitManager;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SitCommand extends Commands {

    public SitCommand() {
        super("sit");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        Player player = (Player) sender;
        SitManager sitManager = MainManager.getSitManager();

        if (sitManager == null) {
            player.sendMessage("§cThe seating system is temporarily unavailable.");
            return;
        }

        if (!player.hasPermission("kaoscore.sit")) {
            player.sendMessage("§cYou do not have permission to execute this command.");
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        if (!sitManager.sit(player)) {
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
        } else {
            player.sendMessage("§aYou are now sitting. Press §6SHIFT §ato stand up.");
        }
    }
}