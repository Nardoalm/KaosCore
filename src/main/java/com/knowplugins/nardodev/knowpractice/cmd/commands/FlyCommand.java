package com.knowplugins.nardodev.knowpractice.cmd.commands;

import com.knowplugins.nardodev.knowpractice.cmd.Commands;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand extends Commands {

    public FlyCommand() {
        super("fly", "voar", "f");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        Player target;

        if (args.length > 0) {
            if (!sender.hasPermission("nardocore.staff.fly")) {
                sender.sendMessage("§c§lERRO ➔ §cVocê não tem permissão para dar fly em outros jogadores.");
                Player senderPlayer = (Player) sender;
                senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§c§lERRO ➔ §cJogador não encontrado.");
                Player senderPlayer = (Player) sender;
                senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage( "§c§lERRO -> §cApenas jogadores podem usar este comando.");
                return;
            }
            target = (Player) sender;

            if (!target.hasPermission("nardocore.member.fly")) {
                sender.sendMessage(ChatColor.RED + "§c§lERRO ➔ §cVocê não possui permissão para executar este comando.");
                Player senderPlayer = (Player) sender;
                senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }


        boolean newStatus = !target.getAllowFlight();

        target.setAllowFlight(newStatus);

        if (!newStatus) {
            target.setFlying(false);
        }

        String status = newStatus ? ChatColor.GREEN + "ativado" : ChatColor.GREEN + "desativado";

        target.sendMessage ("§a§lSUCESSO ➔ §aFly está " + status + ".");

        if (sender != target) {
            sender.sendMessage(ChatColor.GREEN + "§a§lSUCESSO ➔ §aFly está " + target.getName() + " " + status + ".");
        }
    }
}