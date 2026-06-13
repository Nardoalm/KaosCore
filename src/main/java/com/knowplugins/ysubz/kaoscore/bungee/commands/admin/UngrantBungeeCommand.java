package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class UngrantBungeeCommand extends BaseBungeeCommand {

    UngrantBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "ungrant", "removergrant");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.permissions", "§c§lUNGRANT ➔ §cVocê não possui permissão para executar este comando.")) {
            return;
        }
        if (args.length < 2) {
            reply(sender, "§e§lUNGRANT ➔ §eUso: /ungrant <jogador> <grupo> [motivo]");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[0]);
        if (target.getName().isEmpty()) {
            reply(sender, "§c§lUNGRANT ➔ §cJogador inválido.");
            return;
        }

        UUID uuid = target.getUuid() == null ? Texts.offlineUuid(target.getName()) : target.getUuid();
        String reason = Texts.join(args, 2);
        if (reason.isEmpty()) {
            reason = "Grant removido";
        }

        boolean result = plugin.getPermissions().ungrant(uuid, target.getName(), args[1], BungeeCommandUtil.senderUuid(sender), BungeeCommandUtil.senderName(sender), reason);
        reply(sender, result
                ? "§a§lUNGRANT ➔ §aGrant removido de §f" + target.getName() + "§a."
                : "§c§lUNGRANT ➔ §cNenhum grant ativo encontrado para este grupo.");
    }
}
