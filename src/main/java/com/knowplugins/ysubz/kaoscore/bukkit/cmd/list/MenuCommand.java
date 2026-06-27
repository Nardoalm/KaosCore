package com.knowplugins.ysubz.kaoscore.bukkit.cmd.list;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.utils.menu.MenuUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MenuCommand extends Commands {
    public MenuCommand() {
        super("menu");
    }

    public void perform(CommandSender sender, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            MenuUtils.openMenu(player);
        } else {
            sender.sendMessage("§c§lERROR ➔ §cOnly players can use this command.");
        }
    }
}
