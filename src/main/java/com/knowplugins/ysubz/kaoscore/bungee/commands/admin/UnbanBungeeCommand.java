package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class UnbanBungeeCommand extends BaseBungeeCommand {

    UnbanBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "unban", "desbanir");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.banir", "§c§lUNBAN ➔ §cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length < 1) {
            reply(sender, "§c§lUNBAN ➔ §cUso correto: /unban <jogador> [motivo]");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[0]);
        if (target.getName().isEmpty()) {
            reply(sender, "§c§lUNBAN ➔ §cJogador inválido.");
            return;
        }

        String reason = Texts.join(args, 1);
        if (reason.isEmpty()) {
            reason = "Punicao removida";
        }

        boolean result = plugin.getPunishments().clearActive(
                PunishmentType.BAN,
                target.getUuid(),
                target.getName(),
                BungeeCommandUtil.senderUuid(sender),
                BungeeCommandUtil.senderName(sender),
                reason
        );
        reply(sender, result
                ? "§a§lUNBAN ➔ §a" + target.getName() + " foi desbanido."
                : "§c§lUNBAN ➔ §cNenhum ban ativo encontrado para este jogador.");
    }
}

