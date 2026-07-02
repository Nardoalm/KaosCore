package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import com.knowplugins.ysubz.kaoscore.bungee.service.BungeeWhitelistService;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;

final class WhitelistBungeeCommand extends BaseBungeeCommand {

    WhitelistBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "whitelist");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.whitelist", "§c§lWHITELIST ➔ §cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        BungeeWhitelistService whitelist = plugin.getWhitelist();
        String sub = Texts.normalize(args[0]);
        if ("on".equals(sub) || "enable".equals(sub)) {
            whitelist.setEnabled(true);
            reply(sender, "§a§lWHITELIST ➔ §aWhitelist ativada.");
            return;
        }
        if ("off".equals(sub) || "disable".equals(sub)) {
            whitelist.setEnabled(false);
            reply(sender, "§a§lWHITELIST ➔ §aWhitelist desativada.");
            return;
        }
        if ("status".equals(sub)) {
            reply(sender, "§e§lWHITELIST ➔ §eStatus: " + (whitelist.isEnabled() ? "§aativada" : "§cdesativada"));
            return;
        }
        if ("add".equals(sub)) {
            if (args.length < 2) {
                reply(sender, "§c§lWHITELIST ➔ §cUso: /whitelist add <jogador>");
                return;
            }
            ProxiedPlayer online = ProxyServer.getInstance().getPlayer(args[1]);
            boolean result = online != null ? whitelist.addPlayer(online) : whitelist.addName(args[1]);
            reply(sender, result ? "§a§lWHITELIST ➔ §aJogador adicionado." : "§c§lWHITELIST ➔ §cNada foi alterado.");
            return;
        }
        if ("remove".equals(sub) || "del".equals(sub)) {
            if (args.length < 2) {
                reply(sender, "§c§lWHITELIST ➔ §cUso: /whitelist remove <jogador>");
                return;
            }
            reply(sender, whitelist.removeName(args[1]) ? "§a§lWHITELIST ➔ §aJogador removido." : "§c§lWHITELIST ➔ §cJogador não estava na whitelist.");
            return;
        }
        if ("list".equals(sub)) {
            List<String> players = new ArrayList<>(whitelist.getAllowedNames());
            reply(sender, "§e§lWHITELIST ➔ §eAtivada: " + (whitelist.isEnabled() ? "§asim" : "§cnão"));
            reply(sender, "§e§lWHITELIST ➔ §eJogadores liberados: §f" + (players.isEmpty() ? "(nenhum)" : String.join("§7, §f", players)));
            return;
        }

        sendHelp(sender);
    }

    private void sendHelp(CommandSender sender) {
        reply(sender, "§e§lWHITELIST ➔ §eComandos:");
        reply(sender, "§e• §e/whitelist on");
        reply(sender, "§e• §e/whitelist off");
        reply(sender, "§e• §e/whitelist status");
        reply(sender, "§e• §e/whitelist add <jogador>");
        reply(sender, "§e• §e/whitelist remove <jogador>");
        reply(sender, "§e• §e/whitelist list");
    }
}
