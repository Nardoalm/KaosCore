package com.knowplugins.ysubz.kaoscore.bukkit.cmd.list.staff;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.world.BuildManager;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuildCommand extends Commands {

    public BuildCommand() {
        super("construtor", "build");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lERROR ➔ §cOnly players can use this command.");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("kaoscore.admin")) {
            player.sendMessage("§c§lBUILD ➔ §cYou do not have permission to use this command.");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            return;
        }

        if (BuildManager.isBuilder(player)) {
            BuildManager.removeBuilder(player);
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage("§a§lBUILD ➔ §aBuild mode disabled.");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
        } else {
            BuildManager.addBuilder(player);
            player.setGameMode(GameMode.CREATIVE);
            player.sendMessage("§a§lBUILD ➔ §aBuild mode enabled.");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.NOTE_PLING, 1.0f, 1.0f);
        }
    }
}
