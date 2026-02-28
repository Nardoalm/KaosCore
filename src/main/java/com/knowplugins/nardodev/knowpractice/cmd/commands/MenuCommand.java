package com.knowplugins.nardodev.knowpractice.cmd.commands;

import com.knowplugins.nardodev.knowpractice.cmd.Commands;
import com.knowplugins.nardodev.knowpractice.utils.menu.MenuUtils;
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
            sender.sendMessage("§c§lERRO -> §cApenas jogadores podem usar este comando.");
        }
    }
}
