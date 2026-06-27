package com.knowplugins.ysubz.kaoscore.bukkit.cmd.commands.staff;

import com.knowplugins.ysubz.kaoscore.bukkit.cmd.Commands;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.Managers;
import com.knowplugins.ysubz.kaoscore.bukkit.managers.world.WhitelistManager;
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
        if (!sender.hasPermission("kaoscore.staff.whitelist")) {
            sender.sendMessage("§c§lWHITELIST ➔ §cVocê não tem permissão para usar esse comando.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        WhitelistManager whitelist = Managers.getWhitelistManager();
        if (whitelist == null) {
            sender.sendMessage("§c§lWHITELIST ➔ §cSistema indisponível.");
            ModerationCommandUtil.play(sender, Sound.CLICK);
            return;
        }

        if (args.length == 0) {
            sender.sendMessage("§e§lWHITELIST ➔ §eUse: /whitelist on|off|add <nick>|remove <nick>|reload");
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "on":
            case "enable":
                whitelist.setEnabled(true);
                sender.sendMessage("§a§lWHITELIST ➔ §aWhitelist ativada.");
                return;
            case "off":
            case "disable":
                whitelist.setEnabled(false);
                sender.sendMessage("§c§lWHITELIST ➔ §cWhitelist desativada.");
                return;
            case "reload":
                whitelist.load();
                sender.sendMessage("§a§lWHITELIST ➔ §aConfig recarregada.");
                return;
            case "add":
                if (args.length < 2) {
                    sender.sendMessage("§c§lWHITELIST ➔ §cUso: /whitelist add <nick>");
                    return;
                }
                Player online = Bukkit.getPlayerExact(args[1]);
                boolean added = online != null ? whitelist.add(online) : whitelist.addName(args[1]);
                sender.sendMessage(added
                        ? "§a§lWHITELIST ➔ §aAdicionado: §f" + args[1]
                        : "§e§lWHITELIST ➔ §eJá estava na whitelist: §f" + args[1]);
                return;
            case "remove":
            case "del":
                if (args.length < 2) {
                    sender.sendMessage("§c§lWHITELIST ➔ §cUso: /whitelist remove <nick>");
                    return;
                }
                boolean removed = whitelist.removeName(args[1]);
                sender.sendMessage(removed
                        ? "§a§lWHITELIST ➔ §aRemovido: §f" + args[1]
                        : "§e§lWHITELIST ➔ §eNao estava na whitelist: §f" + args[1]);
                return;
            default:
                sender.sendMessage("§e§lWHITELIST ➔ §eUse: /whitelist on|off|add <nick>|remove <nick>|reload");
        }
    }
}


