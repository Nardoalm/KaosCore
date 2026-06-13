package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import com.knowplugins.ysubz.kaoscore.integration.proxy.ProxySyncChannel;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

final class SkinBungeeCommand extends BaseBungeeCommand {

    SkinBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "skin", "setskin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = playerOnly(sender);
        if (player == null) {
            return;
        }

        if (!require(sender, "kaoscore.member.skin", "§c§lSKIN ➔ §cVocê não possui permissão para executar este comando.")) {
            return;
        }

        if (args.length == 1 && "gui".equalsIgnoreCase(args[0])) {
            if (!BungeeSyncMessenger.send(player, ProxySyncChannel.SKIN_GUI)) {
                reply(player, "§c§lSKIN ➔ §cGUI indisponível no servidor atual.");
            }
            return;
        }

        if (args.length != 1) {
            reply(player, "§c§lSKIN ➔ §cUso correto: /skin <nick> | /skin reset | /skin gui");
            return;
        }

        String targetSkin = Texts.safe(args[0]).trim();
        if ("reset".equalsIgnoreCase(targetSkin)) {
            plugin.getSkins().delete(player.getUniqueId());
            if (!BungeeSyncMessenger.send(player, ProxySyncChannel.SKIN_RESET)) {
                reply(player, "§a§lSKIN ➔ §aSua skin foi resetada para a original.");
            }
            return;
        }

        if (targetSkin.isEmpty() || targetSkin.length() > 16) {
            reply(player, "§c§lSKIN ➔ §cNick inválido.");
            return;
        }

        plugin.getSkins().savePending(player.getUniqueId(), targetSkin);
        if (!BungeeSyncMessenger.send(player, ProxySyncChannel.SKIN_SET, targetSkin)) {
            reply(player, "§e§lSKIN ➔ §eSkin pendente salva. Ela será aplicada no próximo login.");
        }
    }
}
