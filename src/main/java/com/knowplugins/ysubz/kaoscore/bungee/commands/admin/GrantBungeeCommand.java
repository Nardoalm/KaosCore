package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class GrantBungeeCommand extends BaseBungeeCommand {

    GrantBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "grant", "atribuir", "conceder");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.permissions", "§c§lGRANT ➔ §cVocê não possui permissão para executar este comando.")) {
            return;
        }
        if (args.length < 2) {
            reply(sender, "§e§lGRANT ➔ §eUso: /grant <jogador> <grupo> [duração] [motivo]");
            return;
        }

        String groupName = args[1];
        if (plugin.getPermissions().getGroup(groupName) == null) {
            reply(sender, "§c§lGRANT ➔ §cGrupo não encontrado: §f" + groupName);
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[0]);
        if (target.getName().isEmpty()) {
            reply(sender, "§c§lGRANT ➔ §cJogador inválido.");
            return;
        }

        long durationMs = 0L;
        int reasonIndex = 2;
        if (args.length >= 3) {
            long parsed = PunishmentTime.parseDuration(args[2]);
            if (parsed != Long.MIN_VALUE) {
                durationMs = parsed;
                reasonIndex = 3;
            }
        }

        String reason = Texts.join(args, reasonIndex);
        if (reason.isEmpty()) {
            reason = "Sem motivo informado";
        }

        UUID uuid = target.getUuid() == null ? Texts.offlineUuid(target.getName()) : target.getUuid();
        try {
            plugin.getPermissions().grant(uuid, target.getName(), groupName, BungeeCommandUtil.senderUuid(sender), BungeeCommandUtil.senderName(sender), durationMs, reason);
            reply(sender, "§a§lGRANT ➔ §aGrant aplicado em §f" + target.getName() + " §a(" + groupName + ").");
        } catch (Exception exception) {
            reply(sender, "§c§lGRANT ➔ §cErro ao aplicar grant.");
            plugin.getLogger().warning("[KaosCore] Falha ao aplicar grant: " + exception.getMessage());
        }
    }
}

