package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CbanBungeeCommand extends BaseBungeeCommand {

    CbanBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "cban", "cb", "cheatban");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.banir", "§c§lCHEATBAN ➔ §cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length < 1) {
            reply(sender, "§c§lCHEATBAN ➔ §cUso correto: /cban <jogador>");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[0]);
        if (target.getPlayer() == null) {
            reply(sender, "§c§lCHEATBAN ➔ §cJogador não existe ou não está online.");
            return;
        }
        if (sender instanceof ProxiedPlayer && target.getUuid() != null && target.getUuid().equals(((ProxiedPlayer) sender).getUniqueId())) {
            reply(sender, "§c§lCHEATBAN ➔ §cVocê não pode se banir.");
            return;
        }

        long expiresAt = System.currentTimeMillis() + (30L * 86_400_000L);
        PunishmentEntry entry = plugin.getPunishments().punish(
                PunishmentType.BAN,
                target.getUuid(),
                target.getName(),
                BungeeCommandUtil.senderUuid(sender),
                BungeeCommandUtil.senderName(sender),
                "Uso de trapaças",
                expiresAt,
                "CHEATBAN"
        );
        if (entry == null) {
            reply(sender, "§c§lCHEATBAN ➔ §cNão foi possível aplicar o cheatban.");
            return;
        }

        target.getPlayer().disconnect(plugin.getPunishmentMessages().buildBanScreen(entry));
        reply(sender, "§a§lCHEATBAN ➔ §a" + target.getName() + " foi banido por 30 dias com sucesso.");
        Texts.broadcast("§c§lBANIMENTO ➔ §c" + target.getName() + " §cfoi banido por uso de trapaças.");
    }
}

