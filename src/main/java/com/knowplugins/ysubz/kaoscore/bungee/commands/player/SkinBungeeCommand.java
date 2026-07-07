package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import com.knowplugins.ysubz.kaoscore.bukkit.integration.proxy.ProxySyncChannel;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
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

        if (args.length == 2) {
            if ("reset".equalsIgnoreCase(args[0])) {
                if (!require(sender, "kaoscore.staff.skin", "§c§lSKIN ➔ §cVocê não possui permissão para executar este comando.")) {
                    return;
                }
                String targetName = args[1];
                ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);
                if (target == null) {
                    reply(player, "§c§lSKIN ➔ §cJogador não está online.");
                    return;
                }
                plugin.getSkins().delete(target.getUniqueId());
                if (!BungeeSyncMessenger.send(target, ProxySyncChannel.SKIN_RESET)) {
                    reply(target, "§a§lSKIN ➔ §aSua skin foi resetada para a original.");
                }
                reply(player, "§a§lSKIN ➔ §aA skin de " + target.getName() + " foi resetada.");
                return;
            } else {
                if (!require(sender, "kaoscore.staff.skin", "§c§lSKIN ➔ §cVocê não possui permissão para executar este comando.")) {
                    return;
                }
                String targetName = args[0];
                String targetSkin = Texts.safe(args[1]).trim();
                ProxiedPlayer target = ProxyServer.getInstance().getPlayer(targetName);
                if (target == null) {
                    reply(player, "§c§lSKIN ➔ §cJogador não está online.");
                    return;
                }
                if (targetSkin.isEmpty() || targetSkin.length() > 16) {
                    reply(player, "§c§lSKIN ➔ §cNick de skin inválido.");
                    return;
                }
                plugin.getSkins().savePending(target.getUniqueId(), targetSkin);
                if (!BungeeSyncMessenger.send(target, ProxySyncChannel.SKIN_SET, targetSkin)) {
                    reply(target, "§e§lSKIN ➔ §eSkin pendente salva. Ela será aplicada no próximo login.");
                }
                reply(player, "§a§lSKIN ➔ §aA skin de " + target.getName() + " foi alterada para: " + targetSkin);
                return;
            }
        }

        if (args.length != 1) {
            if (plugin.getPermissions().hasPermission(player, "kaoscore.staff.skin")) {
                reply(player, "§c§lSKIN ➔ §cUso correto: /skin <nick> | /skin reset [jogador] | /skin <jogador> <skin> | /skin gui");
            } else {
                reply(player, "§c§lSKIN ➔ §cUso correto: /skin <nick> | /skin reset | /skin gui");
            }
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
