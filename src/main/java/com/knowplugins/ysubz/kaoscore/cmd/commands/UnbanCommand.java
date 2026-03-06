package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class UnbanCommand extends Commands {
    public UnbanCommand(){
        super("unban", "unb", "desbanir", "removerban");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if(args.length < 1){
            sender.sendMessage(ChatColor.RED + "§c§lERRO ➔ §cUso correto: /unban <jogador>");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (!sender.hasPermission("nardocore.staff.banir")){
            sender.sendMessage("§c§lERRO ➔ §cVocê não tem permissão para usar esse comando.");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        String targetName = args[0];
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        BanEntry ban = banList.getBanEntry(targetName);

        if (ban == null) {
            sender.sendMessage( "§c§lERRO ➔ §cEsse jogador não está banido.");
            Player senderPlayer = (Player) sender;
            senderPlayer.playSound(senderPlayer.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        banList.pardon(targetName);
        sender.sendMessage("§a§lSUCESSO ➔ §aO jogador " + targetName + " não está mais banido!");

        Player senderPlayer = (Player) sender;

        senderPlayer.playSound(senderPlayer.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
    }
}
