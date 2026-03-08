package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.managers.player.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GoCommand extends Commands {

    public GoCommand() {
        super("go", "goto");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c§lERRO ➔ §cApenas jogadores podem usar este comando.");
            return;
        }

        if (!sender.hasPermission("kaoscore.staff.go")){
            sender.sendMessage("§c§lERRO ➔ §cVocê não tem permissão para usar este comando.");
            return;
        }

        if(args.length != 1) {
            sender.sendMessage("§cUso correto: /go <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage("§c§lERRO ➔ §cJogador não encontrado.");
            return;
        }

        Player staff = (Player) sender;
        VanishManager.addVanish(staff);

        staff.teleport(target.getLocation());
    }
}