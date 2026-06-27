package com.knowplugins.ysubz.kaoscore.bukkit.auth.commands;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetAuthLocationCommand extends Commands {

    private final KaosCore plugin;

    public SetAuthLocationCommand(KaosCore plugin) {
        super("setauthlocation", "setauthloc");
        this.plugin = plugin;
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        if (!player.hasPermission("kaoscore.admin")) {
            player.sendMessage("§cNo permission.");
            return;
        }

        plugin.getAuthManager().setAuthLocation(player.getLocation());
        player.sendMessage("§aAuthentication location set to your current position.");
    }
}