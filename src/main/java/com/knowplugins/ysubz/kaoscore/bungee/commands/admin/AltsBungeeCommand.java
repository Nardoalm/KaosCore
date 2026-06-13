package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class AltsBungeeCommand extends BaseBungeeCommand {

    AltsBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "alts", "alt");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.alts", "§c§lALTS ➔ §cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length < 1) {
            reply(sender, "§c§lALTS ➔ §cUso correto: /alts <jogador>");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[0]);
        if (target.getName().isEmpty()) {
            reply(sender, "§c§lALTS ➔ §cJogador inválido.");
            return;
        }

        List<String> alts = plugin.getPlayerAddresses().findAlts(target.getName(), target.getUuid());
        if (alts.isEmpty()) {
            reply(sender, "§e§lALTS ➔ §eNenhuma conta alternativa encontrada para §f" + target.getName() + "§e.");
            return;
        }

        reply(sender, "§6§lALTS ➔ §ePossíveis contas de §f" + target.getName() + "§e: §f" + String.join("§7, §f", alts));
    }
}

