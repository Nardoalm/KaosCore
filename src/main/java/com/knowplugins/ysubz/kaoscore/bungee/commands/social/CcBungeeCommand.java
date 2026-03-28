package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class CcBungeeCommand extends BaseBungeeCommand {

    CcBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "cc");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = playerOnly(sender);
        if (player == null) {
            return;
        }
        if (!require(sender, "kaoscore.member.clan", "§cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length == 0) {
            reply(sender, "§cUse: /cc <mensagem>");
            return;
        }

        Clan clan = plugin.getClans().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            reply(sender, "§cVocê não está em um clan.");
            return;
        }

        String message = Texts.join(args, 0);
        String speaker = plugin.getDisguises().getVisibleName(player.getUniqueId());
        String formatted = "§6[CLAN] " + speaker + " §8» §7" + message;
        for (UUID memberId : clan.getMembers().keySet()) {
            ProxiedPlayer member = ProxyServer.getInstance().getPlayer(memberId);
            if (member != null) {
                reply(member, formatted);
            }
        }
    }
}

