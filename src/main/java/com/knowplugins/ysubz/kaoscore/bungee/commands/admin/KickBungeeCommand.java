package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class KickBungeeCommand extends BaseBungeeCommand {

    KickBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "kick", "expulsar");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!require(sender, "kaoscore.staff.kick", "§c§lKICK ➔ §cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length < 2) {
            reply(sender, "§c§lKICK ➔ §cUso correto: /kick <jogador> <motivo>");
            return;
        }

        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);
        if (target == null) {
            reply(sender, "§c§lKICK ➔ §cJogador não está online.");
            return;
        }
        if (sender instanceof ProxiedPlayer && target.getUniqueId().equals(((ProxiedPlayer) sender).getUniqueId())) {
            reply(sender, "§c§lKICK ➔ §cVocê não pode se expulsar.");
            return;
        }

        String reason = Texts.join(args, 1);
        target.disconnect(plugin.getPunishmentMessages().buildKickScreen(reason, sender.getName()));
        plugin.getWebhookManager().logKick(target.getName(), target.getUniqueId(), BungeeCommandUtil.senderName(sender), BungeeCommandUtil.senderUuid(sender), reason);
        reply(sender, "§a§lKICK ➔ §a" + target.getName() + " foi expulso da rede.");
    }
}

