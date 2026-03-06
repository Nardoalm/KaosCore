package com.knowplugins.ysubz.kaoscore.cmd.commands;

import com.knowplugins.ysubz.kaoscore.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.listeners.world.WorldProtectionListener;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvpCommand extends Commands {

    public PvpCommand() {
        super("pvp", "combate");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("nardocore.admin.pvp")) {
            sender.sendMessage("§c§lERRO ➔ §cVocê não tem permissao para usar este comando.");
            if (sender instanceof Player) {
                Player p = (Player) sender;
                p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f);
            }
            return;
        }

        if(!WorldProtectionListener.getPvp()){
            WorldProtectionListener.setPvp(true);
            sender.sendMessage("§a§lSUCESSO ➔ §aModo de combate foi ATIVADO.");
            if (sender instanceof Player) {
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
        } else {
            WorldProtectionListener.setPvp(false);
            sender.sendMessage("§a§lSUCESSO ➔ §aModo de combate foi DESATIVADO.");
            if (sender instanceof Player) {
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
        }
    }
}