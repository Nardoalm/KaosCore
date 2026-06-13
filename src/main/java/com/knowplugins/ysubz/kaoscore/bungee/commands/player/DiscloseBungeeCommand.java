package com.knowplugins.ysubz.kaoscore.bungee;

import com.knowplugins.ysubz.kaoscore.bungee.bootstrap.BungeeKaosCore;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class DiscloseBungeeCommand extends BaseBungeeCommand {

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    DiscloseBungeeCommand(BungeeKaosCore plugin) {
        super(plugin, "disclose", "divulgar");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = playerOnly(sender);
        if (player == null) {
            return;
        }

        Configuration config = plugin.getMainConfig();
        if (!config.getBoolean("divulgar.enabled", true)) {
            reply(sender, "§c§lDIVULGAR ➔ §cEste comando está desativado.");
            return;
        }

        String permission = config.getString("divulgar.permission", "kaoscore.member.divulgar");
        String bypass = config.getString("divulgar.bypass-permission", "kaoscore.staff.divulgar");
        if (!require(sender, permission, "§c§lDIVULGAR ➔ §cVocê não tem permissão para usar esse comando.")) {
            return;
        }
        if (args.length < 1) {
            reply(sender, "§c§lDIVULGAR ➔ §cUso correto: /divulgar <mensagem>");
            return;
        }

        int cooldownSeconds = Math.max(0, config.getInt("divulgar.cooldown-seconds", 60));
        if (!plugin.getPermissions().hasPermission(sender, bypass) && cooldownSeconds > 0) {
            long now = System.currentTimeMillis();
            long remaining = cooldowns.getOrDefault(player.getUniqueId(), 0L) - now;
            if (remaining > 0L) {
                reply(sender, "§c§lDIVULGAR ➔ §cAguarde " + Math.max(1L, remaining / 1000L) + "s para divulgar novamente.");
                return;
            }
            cooldowns.put(player.getUniqueId(), now + (cooldownSeconds * 1000L));
        }

        String message = Texts.join(args, 0);
        List<String> lines = config.getStringList("divulgar.broadcast");
        if (lines == null || lines.isEmpty()) {
            lines = BungeeCommandUtil.getStringListOrFallback(config, "divulgar.broadcast", "&6&lDIVULGAR &8➔ &e{player}: &b{link}");
        }
        for (String line : lines) {
            Texts.broadcast(Texts.safe(line)
                    .replace("{player}", player.getName())
                    .replace("{link}", message));
        }
    }
}
