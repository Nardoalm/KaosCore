package com.knowplugins.ysubz.kaoscore.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BungeeCommandUtil {

    private BungeeCommandUtil() {
    }

    public static UUID senderUuid(CommandSender sender) {
        return sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId() : null;
    }

    public static String senderName(CommandSender sender) {
        return sender == null ? "CONSOLE" : sender.getName();
    }

    public static TargetResult resolveTarget(BungeeKaosCore plugin, String input) {
        if (plugin != null && plugin.getDisguises() != null) {
            ProxiedPlayer disguised = plugin.getDisguises().findOnlineByVisibleName(input);
            if (disguised != null) {
                return new TargetResult(disguised.getUniqueId(), disguised.getName(), disguised);
            }
        }

        TargetResult online = TargetResult.resolve(input);
        if (online.getUuid() != null || Texts.normalize(online.getName()).isEmpty()) {
            return online;
        }

        UUID knownUuid = plugin.getPlayerAddresses().findUuidByName(input);
        if (knownUuid != null) {
            return new TargetResult(knownUuid, plugin.getPlayerAddresses().findLatestName(knownUuid), ProxyServer.getInstance().getPlayer(knownUuid));
        }

        if (!plugin.getPlayerAddresses().hasSeenName(input)) {
            return new TargetResult(null, "", null);
        }
        return new TargetResult(Texts.offlineUuid(input), input.trim(), null);
    }

    public static String punishmentSummary(PunishmentEntry entry) {
        if (entry == null) {
            return "Registro invalido";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(entry.getType().name());
        builder.append(" §8| §7motivo: §f").append(Texts.safe(entry.getReason()));
        builder.append(" §8| §7staff: §f").append(Texts.safe(entry.getPunisherName()));
        builder.append(" §8| §7quando: §f").append(PunishmentTime.formatDate(entry.getCreatedAt()));
        if (entry.getType() != PunishmentType.WARN) {
            builder.append(" §8| §7duracao: §f").append(entry.isPermanent() ? "Permanente" : PunishmentTime.formatRemaining(entry.getExpiresAt()));
        }
        builder.append(" §8| §7status: ").append(entry.isActive() ? "§aATIVA" : "§cENCERRADA");
        return builder.toString();
    }

    public static List<String> getStringListOrFallback(Configuration config, String path, String fallback) {
        List<String> lines = config == null ? null : config.getStringList(path);
        if (lines == null || lines.isEmpty()) {
            lines = new ArrayList<>();
            lines.add(fallback);
        }
        return lines;
    }
}
