package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class WarnBungeeCommand extends BaseBungeeCommand {

    WarnBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "warn", "avisar");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.warn", "§c§lWARN ➔ §cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length < 2) {
            reply(sender, "§c§lWARN ➔ §cUso correto: /warn <jogador> <motivo>");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[0]);
        if (target.getName().isEmpty()) {
            reply(sender, "§c§lWARN ➔ §cJogador inválido.");
            return;
        }

        String reason = Texts.join(args, 1);
        PunishmentEntry entry = plugin.getPunishments().warn(
                target.getUuid(),
                target.getName(),
                BungeeCommandUtil.senderUuid(sender),
                BungeeCommandUtil.senderName(sender),
                reason,
                "WARN"
        );
        if (entry == null) {
            reply(sender, "§c§lWARN ➔ §cNão foi possível aplicar o aviso.");
            return;
        }

        reply(sender, "§a§lWARN ➔ §a" + target.getName() + " foi advertido.");
        if (target.getPlayer() != null) {
            Texts.send(target.getPlayer(), "§e§lAVISO ➔ §eVocê recebeu uma advertência. Motivo: §f" + reason);
        }
    }
}
