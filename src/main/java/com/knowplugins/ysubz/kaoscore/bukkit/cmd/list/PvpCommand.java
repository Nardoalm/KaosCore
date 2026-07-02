package com.knowplugins.ysubz.kaoscore.bukkit.cmd.list;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.listener.world.WorldProtectionListener;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvpCommand extends Commands {

    public PvpCommand() {
        super("pvp", "combate");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("kaoscore.admin.pvp")) {
            sender.sendMessage("§c§lPVP ➔ §cYou do not have permission to use this command.");
            if (sender instanceof Player) {
                Player p = (Player) sender;
                p.playSound(p.getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
            return;
        }

        if(!WorldProtectionListener.getPvp()){
            WorldProtectionListener.setPvp(true);
            sender.sendMessage("§a§lPVP ➔ §aCombat mode has been ENABLED.");
            if (sender instanceof Player) {
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
        } else {
            WorldProtectionListener.setPvp(false);
            sender.sendMessage("§a§lPVP ➔ §aCombat mode has been DISABLED.");
            if (sender instanceof Player) {
                ((Player) sender).playSound(((Player) sender).getLocation(), Sound.CLICK, 1.0f, 1.0f);
            }
        }
    }
}
