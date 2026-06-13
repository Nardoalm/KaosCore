package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class BanBungeeCommand extends BaseBungeeCommand {

    BanBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "ban", "tempban", "banir");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.banir", "§c§lBAN ➔ §cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length < 2) {
            reply(sender, "§c§lBAN ➔ §cUso correto: /ban <jogador> <tempo|perm> [motivo]");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[0]);
        if (target.getName().isEmpty()) {
            reply(sender, "§c§lBAN ➔ §cJogador inválido.");
            return;
        }
        if (sender instanceof ProxiedPlayer && target.getUuid() != null && target.getUuid().equals(((ProxiedPlayer) sender).getUniqueId())) {
            reply(sender, "§c§lBAN ➔ §cVocê não pode se banir.");
            return;
        }

        long duration = PunishmentTime.parseDuration(args[1]);
        if (duration == Long.MIN_VALUE) {
            reply(sender, "§c§lBAN ➔ §cTempo inválido. Ex: 30d, 12h, 30m ou perm.");
            return;
        }

        long expiresAt = duration == 0L ? 0L : System.currentTimeMillis() + duration;
        String reason = Texts.join(args, 2);
        if (reason.isEmpty()) {
            reason = "Sem motivo informado";
        }

        PunishmentEntry entry = plugin.getPunishments().punish(
                PunishmentType.BAN,
                target.getUuid(),
                target.getName(),
                BungeeCommandUtil.senderUuid(sender),
                BungeeCommandUtil.senderName(sender),
                reason,
                expiresAt,
                "BAN"
        );
        if (entry == null) {
            reply(sender, "§c§lBAN ➔ §cNão foi possível aplicar o banimento.");
            return;
        }

        if (target.getPlayer() != null) {
            target.getPlayer().disconnect(plugin.getPunishmentMessages().buildBanScreen(entry));
        }

        reply(sender, "§a§lBAN ➔ §a" + target.getName() + " foi banido" + (entry.isPermanent() ? " permanentemente" : " por " + PunishmentTime.formatRemaining(entry.getExpiresAt())) + "§a.");
        Texts.broadcast("§c§lBANIMENTO ➔ §c" + target.getName() + " §cfoi banido. Motivo: §7" + reason);
    }
}
