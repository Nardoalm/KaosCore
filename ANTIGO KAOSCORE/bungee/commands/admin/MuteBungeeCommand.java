package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class MuteBungeeCommand extends BaseBungeeCommand {

    MuteBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "mute", "mutar");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.mutar", "§c§lMUTE ➔ §cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length < 2) {
            reply(sender, "§c§lMUTE ➔ §cUso correto: /mute <jogador> <tempo|perm> [motivo]");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[0]);
        if (target.getName().isEmpty()) {
            reply(sender, "§c§lMUTE ➔ §cJogador inválido.");
            return;
        }

        long duration = PunishmentTime.parseDuration(args[1]);
        if (duration == Long.MIN_VALUE) {
            reply(sender, "§c§lMUTE ➔ §cTempo inválido. Ex: 30d, 12h, 30m ou perm.");
            return;
        }

        long expiresAt = duration == 0L ? 0L : System.currentTimeMillis() + duration;
        String reason = Texts.join(args, 2);
        if (reason.isEmpty()) {
            reason = "Sem motivo informado";
        }

        PunishmentEntry entry = plugin.getPunishments().punish(
                PunishmentType.MUTE,
                target.getUuid(),
                target.getName(),
                BungeeCommandUtil.senderUuid(sender),
                BungeeCommandUtil.senderName(sender),
                reason,
                expiresAt,
                "MUTE"
        );
        if (entry == null) {
            reply(sender, "§c§lMUTE ➔ §cNão foi possível aplicar o mute.");
            return;
        }

        reply(sender, "§a§lMUTE ➔ §a" + target.getName() + " foi mutado" + (entry.isPermanent() ? " permanentemente." : " por " + PunishmentTime.formatRemaining(entry.getExpiresAt())) + "§a.");
        if (target.getPlayer() != null) {
            Texts.send(target.getPlayer(), plugin.getPunishmentMessages().buildMuteNotice(entry));
        }
    }
}

