package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class GrantBungeeCommand extends BaseBungeeCommand {

    GrantBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "grant", "atribuir", "setar", "conceder");
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

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[0]);
        if (target.getName().isEmpty()) {
            reply(sender, "§c§lGRANT ➔ §cJogador inválido.");
            return;
        }

        String groupName = args[1];
        if (plugin.getPermissions().getGroup(groupName) == null) {
            reply(sender, "§c§lGRANT ➔ §cGrupo não encontrado: §f" + groupName);
            return;
        }

        UUID uuid = target.getUuid();
        if (uuid == null) {
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + target.getName()).getBytes(StandardCharsets.UTF_8));
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

        try {
            plugin.getPermissions().grant(uuid, target.getName(), groupName, BungeeCommandUtil.senderUuid(sender), BungeeCommandUtil.senderName(sender), durationMs, reason);
            reply(sender, "§a§lGRANT ➔ §aGrant aplicado em §f" + target.getName() + " §a(" + groupName + ").");
            if (target.getPlayer() != null) {
                Texts.send(target.getPlayer(), "§aVocê recebeu o grupo §f" + groupName + "§a.");
            }
        } catch (Exception exception) {
            reply(sender, "§c§lGRANT ➔ §cErro ao aplicar grant.");
            plugin.getLogger().severe("Falha ao aplicar grant: " + exception.getMessage());
        }
    }
}
