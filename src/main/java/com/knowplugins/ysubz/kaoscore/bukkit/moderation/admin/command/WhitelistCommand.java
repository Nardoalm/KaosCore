package com.knowplugins.ysubz.kaoscore.bukkit.moderation.admin.command;

import com.knowplugins.ysubz.kaoscore.bukkit.command.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.moderation.ModerationCommandUtil;
import com.knowplugins.ysubz.kaoscore.bukkit.manager.MainManager;
import com.knowplugins.ysubz.kaoscore.bukkit.whitelist.WhitelistManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WhitelistCommand extends Commands {

    public WhitelistCommand() {
        super("whitelist", "wl");
    }

    @Override
    public void perform(CommandSender sender, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        WhitelistManager whitelist = MainManager.getWhitelistManager();
        if (whitelist == null) {
            sender.sendMessage("§cSystem unavailable.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /whitelist on|off|add <name>|remove <name>|reload");
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "on":
            case "enable":
                whitelist.setEnabled(true);
                sender.sendMessage("§aWhitelist enabled.");
                return;
            case "off":
            case "disable":
                whitelist.setEnabled(false);
                sender.sendMessage("§cWhitelist disabled.");
                return;
            case "reload":
                whitelist.load();
                sender.sendMessage("§aConfiguration reloaded.");
                return;
            case "list":
                sender.sendMessage("§aPlayers whitelisted:");
                for (String name : whitelist.getAllowedNames()){
                    sender.sendMessage(" - " + name);
                }
                return;
            case "add":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /whitelist add <name>");
                    return;
                }
                Player online = Bukkit.getPlayerExact(args[1]);
                boolean added = online != null ? whitelist.add(online) : whitelist.addName(args[1]);
                sender.sendMessage(added
                        ? "§aAdded: §f" + args[1]
                        : "§eAlready whitelisted: §f" + args[1]);
                return;
            case "remove":
            case "del":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /whitelist remove <name>");
                    return;
                }
                boolean removed = whitelist.removeName(args[1]);
                sender.sendMessage(removed
                        ? "§aRemoved: §f" + args[1]
                        : "§eNot in whitelist: §f" + args[1]);
                return;
            default:
                sender.sendMessage("§eUsage: /whitelist on|off|add <name>|remove <name>|reload|list");
        }
    }
}

