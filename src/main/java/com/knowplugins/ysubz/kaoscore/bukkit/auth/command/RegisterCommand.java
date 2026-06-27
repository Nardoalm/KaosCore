package com.knowplugins.ysubz.kaoscore.bukkit.auth.command;

import com.knowplugins.ysubz.kaoscore.bukkit.KaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegisterCommand extends Commands {

    private final KaosCore plugin;

    public RegisterCommand(KaosCore plugin) {
        super("register", "reg");
        this.plugin = plugin;
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;

        if (!plugin.getAuthManager().isPending(player.getUniqueId())) {
            player.sendMessage("§cYou are already authenticated.");
            return;
        }

        if (args.length != 2) {
            player.sendMessage("§cUsage: /register <password> <password>");
            return;
        }

        if (!args[0].equals(args[1])) {
            player.sendMessage("§cPasswords do not match.");
            return;
        }

        if (!plugin.getAuthManager().register(player, args[0])) {
            player.sendMessage("§cYou are already registered. Use /login <password>");
        }
    }
}