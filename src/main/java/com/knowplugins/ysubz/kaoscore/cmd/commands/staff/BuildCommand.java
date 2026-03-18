package com.knowplugins.ysubz.kaoscore.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.world.BuildManager;
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
            sender.sendMessage("§c§lERRO -> §cApenas jogadores podem usar este comando.");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("kaoscore.admin")) {
            player.sendMessage( "§c§lBUILD ➔ §cVocê não tem permissão para usar esse comando.");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (BuildManager.isBuilder(player)) {
            BuildManager.removeBuilder(player);
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage("§a§lBUILD ➔ §aModo construtor desativado.");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
        } else {
            BuildManager.addBuilder(player);
            player.setGameMode(GameMode.CREATIVE);
            player.sendMessage("§a§lBUILD ➔ §aModo construtor ativado.");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
        }
    }
}
