package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class HistoryBungeeCommand extends BaseBungeeCommand {

    HistoryBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "history", "historico");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.history", "§c§lHISTORY ➔ §cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length < 1) {
            reply(sender, "§c§lHISTORY ➔ §cUso correto: /history <jogador> [limite]");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[0]);
        if (target.getName().isEmpty()) {
            reply(sender, "§c§lHISTORY ➔ §cJogador inválido.");
            return;
        }

        int limit = 10;
        if (args.length >= 2) {
            try {
                limit = Math.max(1, Math.min(30, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
            }
        }

        List<PunishmentEntry> history = plugin.getPunishments().getHistory(target.getUuid(), target.getName(), limit);
        if (history.isEmpty()) {
            reply(sender, "§e§lHISTORY ➔ §eNenhum registro encontrado para §f" + target.getName() + "§e.");
            return;
        }

        reply(sender, "§6§lHISTORY ➔ §eÚltimos registros de §f" + target.getName() + "§e:");
        for (PunishmentEntry entry : history) {
            reply(sender, " §8- §f" + BungeeCommandUtil.punishmentSummary(entry));
        }
    }
}
