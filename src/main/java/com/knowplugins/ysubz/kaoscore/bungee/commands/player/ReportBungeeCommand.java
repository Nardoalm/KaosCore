package com.knowplugins.ysubz.kaoscore.bungee.commands.player;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import com.knowplugins.ysubz.kaoscore.bungee.BungeeKaosCore;
import com.knowplugins.ysubz.kaoscore.bungee.BaseBungeeCommand;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ReportBungeeCommand extends BaseBungeeCommand {

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    ReportBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "report", "denunciar");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer reporter = playerOnly(sender);
        if (reporter == null) {
            return;
        }
        if (!require(sender, "kaoscore.member.report", "§c§lREPORT ➔ §cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length < 2) {
            reply(sender, "§c§lREPORT ➔ §cUso correto: /report <jogador> <motivo>");
            return;
        }

        TargetResult target = BungeeCommandUtil.resolveTarget(plugin, args[0]);
        if (target.getPlayer() == null) {
            reply(sender, "§c§lREPORT ➔ §cJogador não está online.");
            return;
        }
        if (target.getUuid().equals(reporter.getUniqueId())) {
            reply(sender, "§c§lREPORT ➔ §cVocê não pode se reportar.");
            return;
        }

        int cooldownSeconds = Math.max(0, plugin.getMainConfig().getInt("report.cooldown-seconds", 30));
        if (cooldownSeconds > 0) {
            long now = System.currentTimeMillis();
            long remaining = cooldowns.getOrDefault(reporter.getUniqueId(), 0L) - now;
            if (remaining > 0L) {
                reply(sender, "§c§lREPORT ➔ §cAguarde " + Math.max(1L, remaining / 1000L) + "s para reportar novamente.");
                return;
            }
            cooldowns.put(reporter.getUniqueId(), now + (cooldownSeconds * 1000L));
        }

        String reason = Texts.join(args, 1);
        String staffPermission = plugin.getMainConfig().getString("report.staff-permission", "kaoscore.staff.report.receive");
        String format = plugin.getMainConfig().getString("report.format", "&c&lREPORT &8➔ &f{reporter} &7reportou &f{target} &7por: &f{reason}");
        String reporterName = plugin.getDisguises().getVisibleName(reporter.getUniqueId());
        String targetName = plugin.getDisguises().getVisibleName(target.getUuid());
        String message = Texts.safe(format)
                .replace("{reporter}", reporterName)
                .replace("{target}", targetName)
                .replace("{reason}", reason);

        boolean delivered = false;
        for (ProxiedPlayer online : ProxyServer.getInstance().getPlayers()) {
            if (plugin.getPermissions().hasPermission(online, staffPermission)) {
                Texts.send(online, message);
                delivered = true;
            }
        }

        reply(sender, delivered
                ? "§a§lREPORT ➔ §aSeu reporte foi enviado para a equipe."
                : "§e§lREPORT ➔ §eNenhum staff online com permissão para receber reportes.");
    }
}

