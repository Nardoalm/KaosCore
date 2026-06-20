package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class UnmuteBungeeCommand extends BaseBungeeCommand {

    UnmuteBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "unmute", "desmutar");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.mutar", "§c§lUNMUTE ➔ §cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length < 1) {
            reply(sender, "§c§lUNMUTE ➔ §cUso correto: /unmute <jogador> [motivo]");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[0]);
        if (target.getName().isEmpty()) {
            reply(sender, "§c§lUNMUTE ➔ §cJogador inválido.");
            return;
        }

        String reason = Texts.join(args, 1);
        if (reason.isEmpty()) {
            reason = "Punicao removida";
        }

        boolean result = plugin.getPunishments().clearActive(
                PunishmentType.MUTE,
                target.getUuid(),
                target.getName(),
                BungeeCommandUtil.senderUuid(sender),
                BungeeCommandUtil.senderName(sender),
                reason
        );
        reply(sender, result
                ? "§a§lUNMUTE ➔ §a" + target.getName() + " foi desmutado."
                : "§c§lUNMUTE ➔ §cNenhum mute ativo encontrado para este jogador.");
    }
}

